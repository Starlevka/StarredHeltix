package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import set.starlev.injections.accessors.BossHealthOverlayAccessor
import java.util.UUID

/**
 * JS API: Кастомный боссбар.
 */
class JSBossBar(val mcValue: LerpingBossEvent) {
    fun getUUID(): String = mcValue.id.toString()

    fun getName(): String = mcValue.name.string

    fun setName(name: String) {
        mcValue.name = Component.literal(name.replace('&', '§'))
    }

    /** Заполненность 0..1 */
    fun getPercent(): Float = mcValue.progress

    fun setPercent(percent: Float) {
        mcValue.setProgress(percent.coerceIn(0f, 1f))
    }

    override fun toString(): String = "BossBar{name=${getName()}, percent=${getPercent()}}"
}

/**
 * JS API: Объект `bossbars` — чтение серверных боссбаров, создание и удаление
 * клиентских.
 *
 * Клиентские бары, созданные модулем, отслеживаются по UUID ([ownedBars]) — чтобы
 * при выгрузке модуля удалить ТОЛЬКО свои бары и не трогать серверные.
 */
class JSBossBars(private val moduleName: String) {
    private val overlay get() = Minecraft.getInstance().gui.hud.bossOverlay

    /** Все текущие боссбары (включая кастомные, созданные через add). */
    fun getAll(): List<JSBossBar> =
        (overlay as BossHealthOverlayAccessor).shGetBossEvents().values.map { JSBossBar(it) }

    fun getByName(name: String): List<JSBossBar> =
        getAll().filter { it.getName().contains(name, ignoreCase = true) }

    /**
     * Создаёт клиентский боссбар.
     * @param name имя (поддерживает &-цвета)
     * @param percent заполненность 0..1
     */
    fun add(name: String, percent: Float = 1f): JSBossBar {
        val uuid = UUID.randomUUID()
        val event = LerpingBossEvent(
            uuid,
            Component.literal(name.replace('&', '§')),
            percent.coerceIn(0f, 1f),
            net.minecraft.world.BossEvent.BossBarColor.WHITE,
            net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS,
            false, false, false
        )
        (overlay as BossHealthOverlayAccessor).shGetBossEvents()[uuid] = event
        ownedBars.computeIfAbsent(moduleName.lowercase()) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(uuid)
        return JSBossBar(event)
    }

    fun remove(bar: JSBossBar) {
        (overlay as BossHealthOverlayAccessor).shGetBossEvents().remove(bar.mcValue.id)
        ownedBars[moduleName.lowercase()]?.remove(bar.mcValue.id)
    }

    fun removeByName(name: String) {
        val owned = ownedBars[moduleName.lowercase()]
        (overlay as BossHealthOverlayAccessor).shGetBossEvents()
            .values.removeIf { bar ->
                if (bar.name.string.contains(name, ignoreCase = true)) {
                    owned?.remove(bar.id); true
                } else false
            }
    }

    /** Удаляет только клиентские бары ЭТОГО модуля (серверные не трогаем). */
    fun clearAll() = clearOwnedFor(moduleName)

    companion object {
        private val ownedBars = java.util.concurrent.ConcurrentHashMap<String, MutableSet<UUID>>()

        /** Удалить все созданные модулем бары (используется и при выгрузке модуля). */
        fun clearOwnedFor(moduleName: String) {
            val ids = ownedBars.remove(moduleName.lowercase()) ?: return
            val events = (Minecraft.getInstance().gui.hud.bossOverlay as BossHealthOverlayAccessor).shGetBossEvents()
            ids.forEach { events.remove(it) }
        }
    }
}
