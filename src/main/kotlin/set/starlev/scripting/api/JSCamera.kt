package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import set.starlev.scripting.CameraController

/**
 * JS API: Объект `camera` — кастомизация камеры.
 *
 * Камера — глобальный ресурс (одна на клиент), поэтому объект один,
 * а не на модуль. Пока камера отцеплена (detach), её позиция и/или поворот
 * подменяются на уровне Camera.renderState; поворот мышью продолжает
 * работать, пока не задан свой через setRot. Движение игрока (WASD/Space/Shift)
 * подавляется, пока камера отцеплена — опрос клавиш для управления: keybind.isDown.
 *
 * ```js
 * camera.enable();               // отцепить камеру с текущей позиции
 * camera.setPos(x, y, z);        // переместить камеру
 * var p = camera.getPos();       // {x, y, z}
 * camera.setRot(yaw, pitch);     // задать свой поворот (градусы)
 * camera.clearRot();             // вернуть поворот от игрока (обзор мышью)
 * var r = camera.getRot();       // {yaw, pitch} — текущий эффективный поворот
 * var r = camera.getPlayerRot(); // {yaw, pitch} — поворот игрока
 * var p = camera.getPlayerPos(); // {x, y, z} — позиция глаз игрока
 * camera.disable();              // вернуть камеру к игроку
 * camera.toggle(); camera.isEnabled();
 * ```
 */
class JSCamera {

    fun isEnabled(): Boolean = CameraController.detached

    /** Отцепить камеру от игрока (с текущей позиции камеры). false — нет мира/игрока. */
    fun enable(): Boolean = CameraController.detach()

    /** Вернуть камеру к игроку (позиция и поворот). */
    fun disable() = CameraController.attach()

    /** Переключить detach. Возвращает новое состояние. */
    fun toggle(): Boolean =
        if (CameraController.detached) {
            CameraController.attach(); false
        } else {
            CameraController.detach()
        }

    /** Текущая позиция отцепленной камеры: {x, y, z}. */
    fun getPos(): Map<String, Double> = mapOf(
        "x" to CameraController.x,
        "y" to CameraController.y,
        "z" to CameraController.z
    )

    /** Переместить отцепленную камеру. */
    fun setPos(x: Double, y: Double, z: Double) {
        CameraController.x = x
        CameraController.y = y
        CameraController.z = z
    }

    /** Задать свой поворот камеры (градусы). Отключает наследование от игрока. */
    fun setRot(yaw: Double, pitch: Double) {
        CameraController.yaw = yaw.toFloat()
        CameraController.pitch = pitch.toFloat()
        CameraController.rotOverride = true
    }

    /** Вернуть поворот от игрока (обзор мышью). */
    fun clearRot() {
        CameraController.rotOverride = false
    }

    /** Переопределён ли поворот через setRot. */
    fun isRotOverridden(): Boolean = CameraController.rotOverride

    /** Эффективный поворот камеры: {yaw, pitch} в градусах (свой или от игрока). */
    fun getRot(): Map<String, Double>? {
        if (CameraController.rotOverride) {
            return mapOf(
                "yaw" to CameraController.yaw.toDouble(),
                "pitch" to CameraController.pitch.toDouble()
            )
        }
        return getPlayerRot()
    }

    /** Поворот игрока: {yaw, pitch} в градусах. */
    fun getPlayerRot(): Map<String, Double>? {
        val player = Minecraft.getInstance().player ?: return null
        return mapOf(
            "yaw" to player.yRot.toDouble(),
            "pitch" to player.xRot.toDouble()
        )
    }

    /** Позиция глаз игрока: {x, y, z} или null. */
    fun getPlayerPos(): Map<String, Double>? {
        val player = Minecraft.getInstance().player ?: return null
        return mapOf(
            "x" to player.x,
            "y" to player.eyeY,
            "z" to player.z
        )
    }
}
