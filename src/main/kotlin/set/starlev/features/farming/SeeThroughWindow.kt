package set.starlev.features.farming

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.events.ModKeybinds

/**
 * See Through Farming — прозрачное окно.
 *
 * GLFW 3.4 умеет менять прозрачность окна (`glfwSetWindowOpacity`), поэтому сквозь игру
 * видно YouTube/гайд. Процент непрозрачности и клавиша — в конфиге, ключ синхронизируется
 * в `KeyMapping` мода, чтобы он менялся прямо в настройках.
 *
 * Ограничения:
 *  - в полноэкранном режиме не работает — GLFW игнорирует прозрачность;
 *  - не все оконные системы это умеют (Wayland/X11 — да, Windows — обычно нет).
 *    Если система отказала, сообщаем один раз и больше не пытаемся.
 */
object SeeThroughWindow {
    private val mc = Minecraft.getInstance()

    private val keyMapping: KeyMapping by lazy {
        KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.starredheltix.see_through_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                ModKeybinds.CATEGORY
            )
        )
    }

    @Volatile
    private var active = false

    @Volatile
    private var currentOpacity = 1f

    /** Окно однажды отказало — дальше не мучаем систему вызовами. */
    @Volatile
    private var unsupported = false

    private val config
        get() = StarredHeltix.feature.farming.seeThroughWindow

    /** Последний ключ, которым мы синхронизировали KeyMapping. */
    private var boundKey: Int = GLFW.GLFW_KEY_UNKNOWN

    fun init() {
        keyMapping
        syncKey()
    }

    /** Вызывается каждый тик из KeyBindHandler. */
    fun tick() {
        syncKey()
        if (boundKey == GLFW.GLFW_KEY_UNKNOWN) return
        while (keyMapping.consumeClick()) {
            // В GUI клавиша не переключает — иначе можно «включить» из меню и забыть.
            if (mc.gui.screen() != null) return
            active = !active
            apply()
        }
    }

    /** Сброс при смене мира/сервера — окно возвращается к непрозрачному. */
    fun onWorldChange() {
        if (!active) return
        active = false
        apply()
    }

    /** Конфиг изменили — применяем новое значение. */
    fun onConfigChanged() {
        syncKey()
        apply()
    }

    fun isActive(): Boolean = active

    /** Переносит ключ из конфига в KeyMapping (МулConfig пишет int напрямую). */
    private fun syncKey() {
        val wanted = try {
            config.keybind
        } catch (_: Throwable) {
            GLFW.GLFW_KEY_UNKNOWN
        }
        if (wanted == boundKey) return
        boundKey = wanted
        runCatching { keyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(wanted)) }
    }

    private fun apply() {
        if (unsupported) return
        val target = if (active) (config.opacity / 100f).coerceIn(0.05f, 1f) else 1f
        if (currentOpacity == target) return
        if (trySetOpacity(target)) {
            currentOpacity = target
        } else {
            unsupported = true
            mc.player?.sendSystemMessage(
                Component.literal("§cОконная система не поддерживает прозрачное окно")
            )
        }
    }

    /** Прозрачность окна через GLFW; true — система приняла значение. */
    private fun trySetOpacity(alpha: Float): Boolean {
        val handle = runCatching { mc.window.handle() }.getOrNull() ?: return false
        GLFW.glfwGetError(null) // гасим ошибку предыдущего вызова
        GLFW.glfwSetWindowOpacity(handle, alpha)
        return GLFW.glfwGetError(null) == GLFW.GLFW_NO_ERROR
    }
}
