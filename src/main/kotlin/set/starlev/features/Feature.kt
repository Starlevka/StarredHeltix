package set.starlev.features

import net.minecraft.client.Minecraft

/**
 * Базовый класс для всех фич мода.
 *
 * Паттерн вдохновлён Odin Module:
 * — Каждая фича имеет [name], [category], [description].
 * — Состояние включения/выключения управляется через [enabled] + [toggle].
 * — Жизненный цикл: [init] (один раз при старте), [onEnable] / [onDisable] (при переключении).
 */
abstract class Feature(
    val name: String,
    val category: Category,
    val description: String = "",
    initialEnabled: Boolean = false
) {
    protected val mc: Minecraft get() = Minecraft.getInstance()

    /**
     * Текущее состояние фичи (включена/выключена).
     * При изменении вызывает [onEnable] / [onDisable].
     */
    var enabled: Boolean = initialEnabled

    /**
     * Уникальный идентификатор (snake_case, используется в конфиге и GUI).
     */
    open val id: String = name.lowercase().replace(" ", "_")

    /**
     * Вызывается один раз при старте мода.
     */
    open fun init() {}

    /**
     * Вызывается при включении фичи.
     */
    open fun onEnable() {}

    /**
     * Вызывается при выключении фичи.
     */
    open fun onDisable() {}

    /**
     * Переключить состояние фичи.
     */
    fun toggle() {
        enabled = !enabled
        if (enabled) onEnable() else onDisable()
    }

    /**
     * Установить состояние фичи программно.
     */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (enabled) onEnable() else onDisable()
    }

    fun isEnabled(): Boolean = enabled

    override fun toString(): String = "Feature($name, enabled=$enabled)"
}
