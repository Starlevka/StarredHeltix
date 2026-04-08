package set.starlev.secret.features

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import set.starlev.secret.config.SecretConfig
import set.starlev.secret.config.SecretMenuManager
import set.starlev.utils.CacheManager
import java.util.ArrayList
import java.util.UUID

import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.regex.Pattern

object SecretFunFeatures {
    private val starlevPattern = Pattern.compile("(?i)starlev")
    private val megaChromeXPattern = Pattern.compile("(?i)MegaChromeX")
    private val maksimwainPattern = Pattern.compile("(?i)maksimwain")
    private val ridarPattern = Pattern.compile("(?i)ridar")
    private val zinanel0Pattern = Pattern.compile("(?i)zinanel0")
    private val zurGamesPattern = Pattern.compile("(?i)ZurGames")
    private val niKoMaoPattern = Pattern.compile("(?i)NiKoMao")
    private val apostol312Pattern = Pattern.compile("(?i)apostol312")
    private val timyr12Pattern = Pattern.compile("(?i)timyr12")
    
    private var cachedCustomPattern: Pair<String, Pattern>? = null
    
    private val isProcessing = ThreadLocal.withInitial { false }
    private val isForceEnabled = ThreadLocal.withInitial { false }

    private val STARLEV_COLOR = 0xFFFFF5 // Trigger ID 10
    private val MEGACHROME_COLOR = 0xAA00F3 // Dark Red (&4) + Fade+Shake (ID 12)
    // maksimwain: более мягкий бирюзовый оттенок с волной (ID 2)
    private val MAKSIMWAIN_COLOR = 0x33E0FF // (51, 224, 255)
    // ridar: сине-фиолетовое переливание (ID 3)
    private val RIDAR_COLOR = 0x6432C8 // (100, 50, 200)
    // zinanel0: голубо-синее переливание (ID 3)
    private val ZINANEL0_COLOR = 0x3296C8 // (50, 150, 200)
    // ZurGames: голубо-фиолетовый, статичный, волна
    private val ZURGAMES_COLOR = 0x8060FF // (128, 96, 255)
    // NiKoMao: розовый, волна
    private val NIKOMAO_COLOR = 0xFF80C0 // (255, 128, 192)
    // apostol312: серый цвет (&7)
    private val APOSTOL312_COLOR = 0xAAAAAA // (170, 170, 170)
    // timyr12: синий оттенок с волной (ID 2)
    private val TIMYR12_COLOR = 0x1E90FF // (30, 144, 255)
    private val RAINBOW_TRIGGER_COLOR = 0xFFFFFC // ID 3
    private val SPIN_TRIGGER_COLOR = 0xFFFFF7 // ID 8
    
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
        // 3. ИЛИ если в тексте есть специальные триггер-коды (&z, &f и т.д.) - это обрабатывается в ChatFormatting
        
        // Но здесь мы обрабатываем специфические слова "Starlev", "MegaChromeX", "maksimwain", "ridar", "zinanel0" и "Apostol312"
         val isStarlevEnabled = isStarlevNameEffectEnabled() && (isForceEnabled.get() || force)
         val isMegaChromeEnabled = isMegaChromeXEffectEnabled() && (isForceEnabled.get() || force)
         val isMaksimwainEnabled = isMaksimwainEffectEnabled() && (isForceEnabled.get() || force)
         val isRidarEnabled = isRidarEffectEnabled() && (isForceEnabled.get() || force)
         val isZinanel0Enabled = isZinanel0EffectEnabled() && (isForceEnabled.get() || force)
         val isZurGamesEnabled = isZurGamesEffectEnabled() && (isForceEnabled.get() || force)
         val isNiKoMaoEnabled = isNiKoMaoEffectEnabled() && (isForceEnabled.get() || force)
         val isApostol312Enabled = isApostol312EffectEnabled() && (isForceEnabled.get() || force)
         val isTimyr12Enabled = isTimyr12EffectEnabled() && (isForceEnabled.get() || force)
         val isCustomEnabled = isCustomNameEffectEnabled() && (isForceEnabled.get() || force)
        
        val fullText = component.string
        val style = component.style
        val colorValue = style.color?.value
        
        // Если это уже цвет эффекта, не трогаем, но разрешаем ChatFormatting
        if (colorValue == STARLEV_COLOR || colorValue == MEGACHROME_COLOR || 
           colorValue == MAKSIMWAIN_COLOR || colorValue == RIDAR_COLOR || 
           colorValue == ZINANEL0_COLOR || colorValue == ZURGAMES_COLOR || colorValue == NIKOMAO_COLOR ||
           colorValue == APOSTOL312_COLOR || colorValue == TIMYR12_COLOR ||
           colorValue == RAINBOW_TRIGGER_COLOR || colorValue == SPIN_TRIGGER_COLOR) {
            return component
        }
        
        // Если форса нет, но в тексте есть коды эффектов, то ChatFormatting сам справится.
        if (!isStarlevEnabled && !isMegaChromeEnabled && !isMaksimwainEnabled && !isRidarEnabled && !isZinanel0Enabled && !isZurGamesEnabled && !isNiKoMaoEnabled && !isApostol312Enabled && !isTimyr12Enabled && !isCustomEnabled) {
            // Проверка на наличие кодов эффектов в тексте, даже если форса нет
            if (fullText.contains("§z") || fullText.contains("&z") || fullText.contains("§f") || fullText.contains("&f") || fullText.contains("§s") || fullText.contains("&s")) {
                return set.starlev.features.chat.ChatFormatting.processComponent(component)
            }
            return component
        }
        
        // Проверяем кэш (только если не форсируем)
        val styleHash = style.hashCode()
        
        if (!force) {
            val cached = CacheManager.getCachedTextEffect(fullText, styleHash)
            if (cached != null) return cached
        }
        
        try {
            isProcessing.set(true)
            
            val hasStarlev = isStarlevEnabled && fullText.contains("Starlev", ignoreCase = true)
             val hasMegaChrome = isMegaChromeEnabled && fullText.contains("MegaChromeX", ignoreCase = true)
             val hasMaksimwain = isMaksimwainEnabled && fullText.contains("maksimwain", ignoreCase = true)
             val hasRidar = isRidarEnabled && fullText.contains("ridar", ignoreCase = true)
             val hasZinanel0 = isZinanel0Enabled && fullText.contains("zinanel0", ignoreCase = true)
             val hasZurGames = isZurGamesEnabled && fullText.contains("ZurGames", ignoreCase = true)
             val hasNiKoMao = isNiKoMaoEnabled && fullText.contains("NiKoMao", ignoreCase = true)
             val hasApostol312 = isApostol312Enabled && fullText.contains("Apostol312", ignoreCase = true)
             val hasTimyr12 = isTimyr12Enabled && fullText.contains("Timyr12", ignoreCase = true)
             
             // Check for custom effect target presence
             var hasCustom = false
             if (isCustomEnabled) {
                 val target = getCustomEffectTarget()
                 if (target.isNotEmpty() && fullText.contains(target, ignoreCase = true)) {
                     hasCustom = true
                 }
             }
             
             // Если нет специфических слов, но есть коды эффектов - обрабатываем их
             if (!hasStarlev && !hasMegaChrome && !hasMaksimwain && !hasRidar && !hasZinanel0 && !hasZurGames && !hasNiKoMao && !hasApostol312 && !hasTimyr12 && !hasCustom) {
                val formatted = set.starlev.features.chat.ChatFormatting.processComponent(component)
                if (!force && formatted !== component) CacheManager.cacheTextEffect(fullText, styleHash, formatted)
                return formatted
            }
            
            val modified = modifyComponent(component)
            // После обработки специфических слов, прогоняем через ChatFormatting для поддержки кодов в этом же компоненте
            val finalModified = set.starlev.features.chat.ChatFormatting.processComponent(modified)
            
            if (!force) CacheManager.cacheTextEffect(fullText, styleHash, finalModified)
            return finalModified
        } catch (e: Exception) {
            return component
        } finally {
            isProcessing.set(false)
        }
    }

    private fun modifyComponent(component: Component): Component {
        val color = component.style.color?.value
        if (color == STARLEV_COLOR || color == MEGACHROME_COLOR || 
           color == RAINBOW_TRIGGER_COLOR || color == SPIN_TRIGGER_COLOR || color == ZINANEL0_COLOR || color == ZURGAMES_COLOR || color == NIKOMAO_COLOR || color == APOSTOL312_COLOR || color == TIMYR12_COLOR) return component

        val contents = component.contents
        var result: MutableComponent? = null
        
        if (contents is PlainTextContents) {
            val text = contents.text()
            if (text.contains("Starlev", ignoreCase = true) || text.contains("MegaChromeX", ignoreCase = true) || 
               text.contains("maksimwain", ignoreCase = true) || text.contains("ridar", ignoreCase = true) || 
               text.contains("zinanel0", ignoreCase = true) || text.contains("ZurGames", ignoreCase = true) ||
               text.contains("NiKoMao", ignoreCase = true) || text.contains("Apostol312", ignoreCase = true) ||
               text.contains("Timyr12", ignoreCase = true)) {
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
        val isMaksimwainEnabled = isMaksimwainEffectEnabled()
        val isRidarEnabled = isRidarEffectEnabled()
        val isZinanel0Enabled = isZinanel0EffectEnabled()
        val isZurGamesEnabled = isZurGamesEffectEnabled()
        val isNiKoMaoEnabled = isNiKoMaoEffectEnabled()
        val isApostol312Enabled = isApostol312EffectEnabled()

        // Custom Effect Logic - удалено
        val isCustomEnabled = false

        // Создаем список всех вхождений
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
        
        if (isMaksimwainEnabled) {
            val matcher = maksimwainPattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), MAKSIMWAIN_COLOR))
            }
        }
        
        if (isRidarEnabled) {
            val matcher = ridarPattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), RIDAR_COLOR))
            }
        }
        
        if (isZinanel0Enabled) {
           val matcher = zinanel0Pattern.matcher(input)
           while (matcher.find()) {
                 matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), ZINANEL0_COLOR))
           }
        }

        if (isZurGamesEnabled) {
           val matcher = zurGamesPattern.matcher(input)
           while (matcher.find()) {
                 matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), ZURGAMES_COLOR))
           }
        }

        if (isNiKoMaoEnabled) {
           val matcher = niKoMaoPattern.matcher(input)
           while (matcher.find()) {
                 matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), NIKOMAO_COLOR))
           }
        }

        if (isApostol312Enabled) {
            val matcher = apostol312Pattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), APOSTOL312_COLOR))
            }
        }
        
        val isTimyr12Enabled = isTimyr12EffectEnabled()
        if (isTimyr12Enabled) {
            val matcher = timyr12Pattern.matcher(input)
            while (matcher.find()) {
                matches.add(MatchResult(matcher.start(), matcher.end(), matcher.group(), TIMYR12_COLOR))
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

    private fun isMaksimwainEffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.maksimwainEffect
    }

    private fun isRidarEffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.ridarEffect
    }

    private fun isZinanel0EffectEnabled(): Boolean {
       if (!SecretMenuManager.isConfigInitialized) return false
       return SecretMenuManager.secretConfig.funCategory.zinanel0Effect
    }

    private fun isZurGamesEffectEnabled(): Boolean {
       if (!SecretMenuManager.isConfigInitialized) return false
       return SecretMenuManager.secretConfig.funCategory.zurGamesEffect
    }

    private fun isNiKoMaoEffectEnabled(): Boolean {
       if (!SecretMenuManager.isConfigInitialized) return false
       return SecretMenuManager.secretConfig.funCategory.niKoMaoEffect
    }

    private fun isApostol312EffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.apostol312Effect
    }

    private fun isTimyr12EffectEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.timyr12Effect
    }
    
    private fun isCustomNameEffectEnabled(): Boolean = false
    private fun getCustomNameEffect(): SecretConfig.NameEffectType = SecretConfig.NameEffectType.NONE
    private fun getCustomEffectTarget(): String = ""
    private fun getCustomPattern(target: String): Pattern = Pattern.compile("")
}
