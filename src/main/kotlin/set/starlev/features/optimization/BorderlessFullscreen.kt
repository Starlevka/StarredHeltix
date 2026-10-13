package set.starlev.features.optimization

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix

/**
 * Бордерлесс — полноэкранный режим окна без рамки.
 *
 * Идея из `scripts/SOURCES/Borderless-Fullscreen-1.21.1-main`: окно остаётся ОКОННЫМ
 * (`glfwGetWindowMonitor == 0`), но у него снимаются декорации и оно растягивается на
 * весь монитор. В отличие от ванильного фулскрина это не exclusive fullscreen: alt+tab
 * не выкидывает игру, окно можно перетащить на другой монитор, смена видеорежима делается
 * на лету.
 *
 * Переключение мгновенное (перезапуск не нужен): раз в тик сравниваем настройку с тем,
 * что реально применено, и «лечим» состояние, если его сбил ванильный полный экран (F11
 * возвращает окно в оконный режим с рамкой). Настоящий фулскрин не трогаем — если
 * `glfwGetWindowMonitor != 0`, выходим и ждём, пока игрок сам вернётся в оконный режим.
 *
 * Про hide/show окна (как в оригинальном моде) здесь намеренно отказались: на Windows
 * `ShowWindow(SW_HIDE)` деактивирует окно, а `SW_SHOWNA` не активирует его обратно —
 * клиент остался бы «не в фокусе», и Minecraft ставил бы игру на паузу. Вместо этого меняем
 * атрибут и геометрию подряд, за один кадр.
 */
object BorderlessFullscreen {
    private val LOGGER = StarredHeltix.LOGGER

    /** Реально применён ли бордерлесс (состояние окна, а не значение настройки). */
    private var applied = false

    /** Оконная геометрия до включения — восстанавливается при выключении. */
    private var saved: SavedState? = null

    private class SavedState(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val maximized: Boolean,
        val autoIconify: Boolean,
    )

    private val config
        get() = StarredHeltix.feature.optimization.window

    /** На macOS GLFW_AUTO_ICONIFY меняется только при создании окна — не трогаем. */
    private val macOs: Boolean = try {
        Util.getPlatform() == Util.OS.OSX
    } catch (_: Throwable) {
        false
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { tick() }
    }

    // ─────────────────────────── тик ───────────────────────────

    private fun tick() {
        val want = try {
            config.borderless
        } catch (_: Throwable) {
            return
        }
        val handle = try {
            Minecraft.getInstance().window.handle()
        } catch (_: Throwable) {
            return
        }
        if (handle == 0L) return

        // Ванильный (exclusive) фулскрин — не лезем в геометрию окна.
        if (GLFW.glfwGetWindowMonitor(handle) != 0L) {
            applied = false
            saved = null
            return
        }

        // Саморемонт: окно снова с рамкой (ванильный F11, смена разрешения и т.п.).
        val decorated = try {
            GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE
        } catch (_: Throwable) {
            false
        }

        try {
            when {
                want && !applied -> enable(handle)
                want && applied && decorated -> enable(handle)
                !want && applied -> disable(handle)
            }
        } catch (t: Throwable) {
            // Клиент не должен падать из-за настройки окна.
            applied = false
            LOGGER.warn("[Borderless] Не удалось применить режим: ${t.message}")
        }
    }

    // ─────────────────────────── включение / выключение ───────────────────────────

    private fun enable(handle: Long) {
        // Свёрнутое окно не трогаем — вернёмся к нему следующим тиком.
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) != 0) return

        val alreadyApplied = applied
        val maximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE
        if (saved == null) saved = readState(handle, maximized)

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE)
        if (!macOs) GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE)

        if (!maximized) {
            val monitor = monitorAt(handle)
            if (monitor != null) {
                GLFW.glfwSetWindowPos(handle, monitor.x, monitor.y)
                GLFW.glfwSetWindowSize(handle, monitor.width, monitor.height)
            }
        }
        applied = true
    }

    private fun disable(handle: Long) {
        val state = saved
        saved = null
        applied = false

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE)
        if (!macOs) {
            GLFW.glfwSetWindowAttrib(
                handle,
                GLFW.GLFW_AUTO_ICONIFY,
                if (state?.autoIconify == false) GLFW.GLFW_FALSE else GLFW.GLFW_TRUE,
            )
        }
        if (state != null && !state.maximized) {
            GLFW.glfwSetWindowPos(handle, state.x, state.y)
            GLFW.glfwSetWindowSize(handle, state.width, state.height)
        }
    }

    // ─────────────────────────── GLFW-хелперы ───────────────────────────

    private fun readState(handle: Long, maximized: Boolean): SavedState {
        val x = IntArray(1)
        val y = IntArray(1)
        GLFW.glfwGetWindowPos(handle, x, y)
        val w = IntArray(1)
        val h = IntArray(1)
        GLFW.glfwGetWindowSize(handle, w, h)
        return SavedState(
            x = x[0],
            y = y[0],
            width = w[0],
            height = h[0],
            maximized = maximized,
            autoIconify = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY) == GLFW.GLFW_TRUE,
        )
    }

    private class MonitorInfo(val x: Int, val y: Int, val width: Int, val height: Int)

    /** Монитор, на котором сейчас лежит окно (по его центру), иначе основной. */
    private fun monitorAt(handle: Long): MonitorInfo? {
        val x = IntArray(1)
        val y = IntArray(1)
        GLFW.glfwGetWindowPos(handle, x, y)
        val w = IntArray(1)
        val h = IntArray(1)
        GLFW.glfwGetWindowSize(handle, w, h)
        val cx = x[0] + w[0] / 2
        val cy = y[0] + h[0] / 2

        val monitors = GLFW.glfwGetMonitors() ?: return readMonitor(GLFW.glfwGetPrimaryMonitor())
        for (i in monitors.position() until monitors.limit()) {
            val info = readMonitor(monitors.get(i)) ?: continue
            if (cx >= info.x && cy >= info.y && cx < info.x + info.width && cy < info.y + info.height) {
                return info
            }
        }
        return readMonitor(GLFW.glfwGetPrimaryMonitor())
    }

    private fun readMonitor(monitor: Long): MonitorInfo? {
        if (monitor == 0L) return null
        val mode = GLFW.glfwGetVideoMode(monitor) ?: return null
        val x = IntArray(1)
        val y = IntArray(1)
        GLFW.glfwGetMonitorPos(monitor, x, y)
        return MonitorInfo(x[0], y[0], mode.width(), mode.height())
    }
}