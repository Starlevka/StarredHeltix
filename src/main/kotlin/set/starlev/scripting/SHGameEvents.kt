package set.starlev.scripting

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket

/**
 * Редкие игровые события для JS-модулей: pickup, damage, connect/disconnect.
 *
 * Триггеры:
 *  - "pickup"      — игрок поднял предмет: (itemId, amount)
 *  - "damage"      — любой сущности нанесён урон (сервер прислал ClientboundDamageEventPacket):
 *                    (entityName, damageTypeId, isSelf)
 *  - "connect"     — вход на сервер
 *  - "disconnect"  — выход с сервера
 *
 * Подписка стандартная: events.onPickup(fn) и т.д. (см. JSEvents).
 */
object SHGameEvents {

    private val client: Minecraft get() = Minecraft.getInstance()

    private var installed = false

    fun init() {
        if (installed) return
        installed = true
        // ===== connect / disconnect =====
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            SHScriptEngine.fireTrigger("connect")
            SHScriptEngine.fireTrigger("serverConnect")
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            // Кастомная камера: сбрасываем, чтобы не осталась отцепленной в новом мире
            CameraController.attach()
            SHScriptEngine.fireTrigger("disconnect")
            SHScriptEngine.fireTrigger("serverDisconnect")
        }

        // ===== entityLoad / entityUnload (Fabric ClientEntityEvents) =====
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (SHScriptEngine.isLoaded) {
                SHScriptEngine.fireTrigger(
                    "entityLoad",
                    entity.name.string,
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString(),
                    entity.uuid.toString()
                )
            }
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            if (SHScriptEngine.isLoaded) {
                SHScriptEngine.fireTrigger(
                    "entityUnload",
                    entity.name.string,
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString(),
                    entity.uuid.toString()
                )
            }
        }

        // ===== day/night (смена дня/ночи, проверка раз в секунду) =====
        // 26.2: isDarkOutside() = skyDarken >= 4 (см. Level.isBrightOutside)
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            val now = System.currentTimeMillis()
            if (now - lastDayCheck > 1000) {
                lastDayCheck = now
                val level = client.level ?: return@register
                val isNight = level.isDarkOutside
                if (isNight != lastWasNight) {
                    lastWasNight = isNight
                    SHScriptEngine.fireTrigger(if (isNight) "nightStart" else "dayStart")
                }
            }
        }
    }

    private var lastDayCheck = 0L
    private var lastWasNight = false

    /**
     * Вызывается из ConnectionMixin после общего packetReceive:
     * семантический разбор игровых пакетов (pickup/damage).
     */
    fun handleGamePacket(packet: Any) {
        when (packet) {
            is ClientboundTakeItemEntityPacket -> {
                val level = client.level ?: return
                val playerId = packet.playerId
                val collected = level.getEntity(playerId) ?: return
                val localPlayer = client.player ?: return
                if (collected === localPlayer) {
                    // itemId — сущность предмета, amount — количество
                    val itemEntity = level.getEntity(packet.itemId)
                    val itemId = itemEntity?.let {
                        if (it is net.minecraft.world.entity.item.ItemEntity) {
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.item.item).toString()
                        } else null
                    } ?: ""
                    SHScriptEngine.fireTrigger("pickup", itemId, packet.amount)
                }
            }
            is ClientboundDamageEventPacket -> {
                val level = client.level ?: return
                val entity = level.getEntity(packet.entityId) ?: return
                val damageTypeId = packet.sourceType.unwrapKey()
                    .map { it.identifier().toString() }
                    .orElse("")
                val isSelf = entity === client.player
                val entityName = entity.name.string
                SHScriptEngine.fireTrigger("damage", entityName, damageTypeId, isSelf)
            }
        }
    }
}
