package set.starlev.secret.features

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import set.starlev.secret.config.SecretConfig
import set.starlev.secret.config.SecretMenuManager
import java.util.ArrayList
import java.util.UUID

import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.regex.Pattern

import java.util.WeakHashMap
import java.util.Collections

object SecretFunFeatures {
    private val starlevPattern = Pattern.compile("(?i)starlev")
    private val megaChromeXPattern = Pattern.compile("(?i)MegaChromeX")
    
    private val isProcessing = ThreadLocal.withInitial { false }
    private val isForceEnabled = ThreadLocal.withInitial { false }
    private val componentCache = Collections.synchronizedMap(WeakHashMap<Component, Component>())

    private val STARLEV_COLOR = 0xFFFFF5 // Trigger ID 10
    private val MEGACHROME_COLOR = 0xAA00F3 // Dark Red (&4) + Fade+Shake (ID 12)
    
    fun init() {
        // ...
    }

    /**
     * Выполнить блок кода с принудительно включенными эффектами текста.
     * Используется для точечного включения эффектов в конкретных функциях мода.
     */
    fun <T> withForceEffects(block: () -> T): T {
        val previous = isForceEnabled.get()
        isForceEnabled.set(true)
        try {
            return block()
        } finally {
            isForceEnabled.set(previous)
        }
    }

    @JvmStatic
    fun processComponent(component: Component): Component {
        return processComponent(component, false)
    }

    @JvmStatic
    fun processComponent(component: Component, force: Boolean): Component {
        if (isProcessing.get()) return component
        
        // Эффекты работают только если:
        // 1. Они включены в конфиге И включен глобальный форс (isForceEnabled)
        // 2. ИЛИ если передан параметр force=true (для таба/тайтлов, если нужно)
        val isStarlevEnabled = isStarlevNameEffectEnabled() && (isForceEnabled.get() || force)
        val isMegaChromeEnabled = isMegaChromeXEffectEnabled() && (isForceEnabled.get() || force)
        
        if (!isStarlevEnabled && !isMegaChromeEnabled) return component
        
        // Проверяем кэш (только если не форсируем)
        if (!force) {
            val cached = componentCache[component]
            if (cached != null) return cached
        }
        
        val style = component.style
        val colorValue = style.color?.value
        
        // Если это уже цвет эффекта, не трогаем
        if (colorValue == STARLEV_COLOR || colorValue == MEGACHROME_COLOR) return component
        
        try {
            isProcessing.set(true)
            
            val fullText = component.getString()
            val hasStarlev = isStarlevEnabled && fullText.contains("Starlev", ignoreCase = true)
            val hasMegaChrome = isMegaChromeEnabled && fullText.contains("MegaChromeX", ignoreCase = true)
            
            if (!hasStarlev && !hasMegaChrome) {
                if (!force) componentCache[component] = component
                return component
            }
            
            val modified = modifyComponent(component)
            if (!force) componentCache[component] = modified
            return modified
        } catch (e: Exception) {
            return component
        } finally {
            isProcessing.set(false)
        }
    }

    private fun modifyComponent(component: Component): Component {
        val color = component.style.color?.value
        if (color == STARLEV_COLOR || color == MEGACHROME_COLOR) return component

        val contents = component.contents
        var result: MutableComponent? = null
        
        if (contents is PlainTextContents) {
            val text = contents.text()
            if (text.contains("Starlev", ignoreCase = true) || text.contains("MegaChromeX", ignoreCase = true)) {
                result = replaceInString(text, component.style)
            }
        } else if (contents is TranslatableContents) {
            val args = contents.args
            var argsChanged = false
            val newArgs = arrayOfNulls<Any>(args.size)
            
            for (i in args.indices) {
                val arg = args[i]
                if (arg is Component) {
                    val modifiedArg = modifyComponent(arg)
                    if (modifiedArg !== arg) {
                        argsChanged = true
                        newArgs[i] = modifiedArg
                    } else {
                        newArgs[i] = arg
                    }
                } else {
                    newArgs[i] = arg
                }
            }
            
            if (argsChanged) {
                result = Component.translatable(contents.key, *newArgs).withStyle(component.style)
            }
        }
        
        // Рекурсивно обрабатываем сиблингов
        val siblings = component.siblings
        var siblingsChanged = false
        val newSiblings = ArrayList<Component>(siblings.size)
        
        for (sibling in siblings) {
            val modifiedSibling = modifyComponent(sibling)
            if (modifiedSibling !== sibling) {
                siblingsChanged = true
            }
            newSiblings.add(modifiedSibling)
        }

        // Если ничего не изменилось ни в контенте, ни в сиблингах - возвращаем оригинал
        if (result == null && !siblingsChanged) {
            return component
        }

        // Собираем новый компонент
        val base = result ?: MutableComponent.create(contents).withStyle(component.style)
        
        // Очищаем сиблингов у новой базы (если они там были) и добавляем наших (возможно измененных)
        for (s in newSiblings) {
            base.append(s)
        }

        return base
    }

    private fun replaceInString(input: String, style: Style): MutableComponent {
        val root = Component.empty()
        
        val isStarlevEnabled = isStarlevNameEffectEnabled()
        val isMegaChromeEnabled = isMegaChromeXEffectEnabled()

        // Создаем список всех вхождений обоих паттернов
        val matches = mutableListOf<MatchResult>()
        
        if (isStarlevEnabled) {
            val matcher = starlevPattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), STARLEV_COLOR))
            }
        }
        
        if (isMegaChromeEnabled) {
            val matcher = megaChromeXPattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), MEGACHROME_COLOR))
            }
        }
        
        // Сортируем вхождения по позиции
        matches.sortBy { it.start }
        
        var lastEnd = 0
        for (match in matches) {
            if (match.start < lastEnd) continue // Пропускаем перекрывающиеся (маловероятно здесь)
            
            // Текст до совпадения
            if (match.start > lastEnd) {
                root.append(Component.literal(input.substring(lastEnd, match.start)).withStyle(style))
            }
            
            // Совпадение с эффектом
            root.append(Component.literal(match.text)
                .withStyle(style.withColor(TextColor.fromRgb(match.color))))
            
            lastEnd = match.end
        }
        
        // Оставшийся текст
        if (lastEnd < input.length) {
            root.append(Component.literal(input.substring(lastEnd)).withStyle(style))
        }
        
        return if (root.siblings.isEmpty()) Component.literal(input).withStyle(style) else root
    }

    private data class MatchResult(val start: Int, val end: Int, val text: String, val color: Int)
    
    fun isFlipEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.flipPlayer
    }

    fun isCustomWeatherEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.customWeather
    }

    fun getWeatherType(): SecretConfig.WeatherMode {
        if (!SecretMenuManager.isConfigInitialized) return SecretConfig.WeatherMode.CLEAR
        return SecretMenuManager.secretConfig.funCategory.weatherType
    }

    fun isSnowEverywhereEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.snowEverywhere
    }

    private fun isStarlevNameEffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.starlevNameEffect
    }

    private fun isMegaChromeXEffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.megaChromeXEffect
    }
}
