package set.starlev.scripting.api

import net.minecraft.network.chat.Component
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.UniqueTag
import set.starlev.features.skyblock.NpcDialogueOverlay
import set.starlev.render.ScriptNpcSkins
import set.starlev.render.ScriptNPCManager
import java.util.UUID

/**
 * JS API: Объект `npc` — визуальные клиентские NPC-игроки для модулей.
 *
 * Пример:
 * ```js
 * const bear = npc.create({
 *     name: "Мишка",
 *     x: 10, y: 70, z: -5,
 *     skin: "lic:Notch",          // скин с лицензии (или "steve", "assets:starredheltix:npc/медведь", "url:https://...", UUID)
 *     showName: true,
 *     onClick: function(npcName) { ... }
 * });
 * bear.say("Привет, игрок!");
 * bear.lookAtPlayer();
 * bear.remove();
 * ```
 */
class JSNpc(private val moduleName: String) {

    fun create(options: Scriptable?): JSNpcHandle? {
        if (options == null) return null
        try {
            val name = options.optString("name", "NPC")
            val x = options.optNumber("x", 0.0)
            val y = options.optNumber("y", 64.0)
            val z = options.optNumber("z", 0.0)
            val yaw = options.optNumber("yaw", 0.0).toFloat()
            val pitch = options.optNumber("pitch", 0.0).toFloat()
            val showName = options.optBoolean("showName", true)
            val skinSource = options.optString("skin", "steve")
            val onClick = options.optCallable("onClick")
            val morphType = options.optString("morph", "").takeIf { it.isNotBlank() }
            val lookAtPlayer = options.optBoolean("lookAtPlayer", false)

            val skinState = ScriptNpcSkins.resolve(skinSource, UUID.randomUUID())
            val entry = ScriptNPCManager.create(
                ownerModule = moduleName,
                name = name,
                x = x, y = y, z = z,
                yaw = yaw, pitch = pitch,
                showName = showName,
                skinState = skinState
            )
            entry.onClick = onClick
            entry.morphType = morphType
            entry.lookAtPlayer = lookAtPlayer
            return JSNpcHandle(moduleName, entry)
        } catch (e: Throwable) {
            set.starlev.scripting.ScriptConsole.error(moduleName, "Ошибка создания NPC: ${e.message}", e)
            return null
        }
    }

    /** Удалить все NPC, созданные этим модулем. */
    fun removeAll() {
        ScriptNPCManager.removeForModule(moduleName)
    }
}

/**
 * Хэндл конкретного NPC, возвращаемый `npc.create(...)`.
 */
class JSNpcHandle(private val moduleName: String, private val entry: ScriptNPCManager.NpcEntry) {

    fun getName(): String = entry.name

    fun setName(name: String) {
        entry.name = name
    }

    fun getPos(): Map<String, Double> =
        mapOf("x" to entry.x, "y" to entry.y, "z" to entry.z)

    fun setPos(x: Double, y: Double, z: Double) {
        entry.x = x
        entry.y = y
        entry.z = z
    }

    /** Сместить NPC на дельту (в блоках). */
    fun move(dx: Double, dy: Double, dz: Double) {
        entry.x += dx
        entry.y += dy
        entry.z += dz
    }

    fun getYaw(): Float = entry.yaw

    fun getPitch(): Float = entry.pitch

    fun setYaw(yaw: Float) {
        entry.yaw = yaw
    }

    fun setPitch(pitch: Float) {
        entry.pitch = pitch
    }

    /** Повернуть NPC лицом к игроку (разовый вызов). */
    fun lookAtPlayer() {
        set.starlev.scripting.SHScriptEngine.let {
            val mc = net.minecraft.client.Minecraft.getInstance()
            val player = mc.player ?: return
            val dx = player.x - entry.x
            val dy = (player.y + player.eyeHeight) - (entry.y + 1.62)
            val dz = player.z - entry.z
            val horizontal = Math.sqrt(dx * dx + dz * dz)
            entry.yaw = (Math.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
            entry.pitch = (-(Math.atan2(dy, horizontal) * 180.0 / Math.PI)).toFloat()
        }
    }

    /**
     * Морф: превратить NPC в любую сущность (не только игрока).
     * @param type id сущности: "minecraft:zombie", "minecraft:pig", "minecraft:villager"...
     *             Пустая строка/null — вернуть облик игрока (скин сохраняется).
     */
    fun morph(type: String?) {
        entry.morphEntity?.discard()
        entry.morphEntity = null
        entry.morphType = type?.takeIf { it.isNotBlank() }
    }

    /** Текущий тип морфа (или null — обычный игрок). */
    fun getMorph(): String? = entry.morphType

    /**
     * Автоматический взгляд NPC на игрока (каждый тик, плавный поворот).
     */
    fun setLookAtPlayer(value: Boolean) {
        entry.lookAtPlayer = value
    }

    fun isLookingAtPlayer(): Boolean = entry.lookAtPlayer

    /** Скорость плавного поворота при auto-взгляде (0.01..1.0). */
    fun setLookSpeed(speed: Float) {
        entry.lookSpeed = speed.coerceIn(0.01f, 1f)
    }

    fun showName(value: Boolean) {
        entry.showName = value
    }

    /**
     * Интеракция по чату: NPC «говорит» — сообщение идёт в оверлей NPC-диалогов
     * (если включён) или напрямую в чат игрока.
     */
    fun say(message: String) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val player = mc.player ?: return
        mc.execute {
            val full = Component.literal("§b${entry.displayName}§f: $message")
            if (!NpcDialogueOverlay.onChat(full)) {
                player.sendSystemMessage(Component.literal("§7[§bNPC§7]§f $full"))
            }
        }
    }

    /** Установить/заменить обработчик клика по NPC (ЛКМ). */
    fun onClick(callable: Callable?) {
        entry.onClick = callable
    }

    fun remove() {
        ScriptNPCManager.remove(entry.uuid)
    }
}

// ==== Вспомогательные расширения для чтения JS-объектов (Rhino NativeObject) ====

private val NOT_FOUND = org.mozilla.javascript.Scriptable.NOT_FOUND
private val undefinedValue: Any? by lazy { org.mozilla.javascript.Undefined.instance }

internal fun Scriptable.optString(key: String, default: String): String {
    val v = try { get(key, this) } catch (e: Exception) { null }
    return if (v == null || v == NOT_FOUND || v === undefinedValue) default else v.toString()
}

internal fun Scriptable.optNumber(key: String, default: Double): Double {
    val v = try { get(key, this) } catch (e: Exception) { null }
    return if (v == null || v == NOT_FOUND || v === undefinedValue) default
    else (v as? Number)?.toDouble() ?: default
}

internal fun Scriptable.optBoolean(key: String, default: Boolean): Boolean {
    val v = try { get(key, this) } catch (e: Exception) { null }
    return if (v == null || v == NOT_FOUND || v === undefinedValue) default
    else (v as? Boolean) ?: default
}

internal fun Scriptable.optCallable(key: String): Callable? {
    val v = try { get(key, this) } catch (e: Exception) { null }
    return if (v == null || v == NOT_FOUND || v === undefinedValue) null
    else v as? Callable
}
