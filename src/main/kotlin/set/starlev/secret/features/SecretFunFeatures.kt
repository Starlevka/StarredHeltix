package set.starlev.secret.features

import net.minecraft.network.chat.Component
import set.starlev.secret.config.SecretConfig
import set.starlev.secret.config.SecretMenuManager

object SecretFunFeatures {
    private val isProcessing = ThreadLocal.withInitial { false }
    private val isForceEnabled = ThreadLocal.withInitial { false }

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

        // Специфические эффекты ников были удалены.
        // Здесь остаются только коды эффектов (&z, &s, &f), которые обрабатывает ChatFormatting.
        if (fullTextHasEffectCodes(component)) {
            return set.starlev.features.chat.ChatFormatting.processComponent(component)
        }

        return component
    }

    private fun fullTextHasEffectCodes(component: Component): Boolean {
        return try {
            val text = component.string
            text.contains("§z") || text.contains("&z") || text.contains("§s") || text.contains("&s") ||
                text.contains("§f") || text.contains("&f")
        } catch (e: Exception) {
            false
        }
    }

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

    fun isBigHeadsEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.bigHeads
    }

    fun getBigHeadScale(): Float {
        if (!SecretMenuManager.isConfigInitialized) return 1.0f
        return SecretMenuManager.secretConfig.funCategory.bigHeadScale
    }
}
