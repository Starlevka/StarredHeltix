package set.starlev.render

import com.mojang.authlib.GameProfile
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.mozilla.javascript.Callable
import set.starlev.StarredHeltix
import set.starlev.scripting.ScriptConsole
import set.starlev.scripting.SHScriptEngine
import java.util.Base64
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ÐšÐ»Ð¸ÐµÐ½Ñ‚ÑÐºÐ¸Ð¹ NPC-Ð¸Ð³Ñ€Ð¾Ðº Ð´Ð»Ñ JS-Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹: Ð²Ð¸Ð·ÑƒÐ°Ð»ÑŒÐ½Ð°Ñ ÑÑƒÑ‰Ð½Ð¾ÑÑ‚ÑŒ Ð±ÐµÐ· ÑƒÑ‡Ð°ÑÑ‚Ð¸Ñ ÑÐµÑ€Ð²ÐµÑ€Ð°.
 * Ð¡ÐºÐ¸Ð½ â€” Ð´Ð¸Ð½Ð°Ð¼Ð¸Ñ‡ÐµÑÐºÐ¸Ð¹ (Ð¸Ð· Ñ€ÐµÑÑƒÑ€ÑÐ¾Ð², Ð¿Ð¾ Ð½Ð¸ÐºÑƒ/UUID Ñ Ð»Ð¸Ñ†ÐµÐ½Ð·Ð¸Ð¸, Ð¿Ð¾ URL Ð¸Ð»Ð¸ Ð´ÐµÑ„Ð¾Ð»Ñ‚Ð½Ñ‹Ð¹).
 */
class ScriptFakePlayer(
    level: ClientLevel,
    profile: GameProfile,
    val entry: ScriptNPCManager.NpcEntry
) : RemotePlayer(level, profile) {

    override fun shouldShowName(): Boolean = entry.showName

    override fun getCustomName(): Component = Component.literal(entry.displayName)

    override fun hasCustomName(): Boolean = true

    override fun isCustomNameVisible(): Boolean = entry.showName

    override fun getSkin(): PlayerSkin = try {
        entry.skinSupplier()
    } catch (e: Exception) {
        DefaultPlayerSkin.getDefaultSkin()
    }

    fun setSkinLayers(layers: Byte) {
        this.entityData.set(net.minecraft.world.entity.Avatar.DATA_PLAYER_MODE_CUSTOMISATION, layers)
    }
}

object ScriptNPCManager {
    private val mc: Minecraft get() = Minecraft.getInstance()

    // 26.2: ÐºÐ»Ð¸ÐµÐ½Ñ‚ÑÐºÐ¸Ðµ ÑÑƒÑ‰Ð½Ð¾ÑÑ‚Ð¸ Ñ‚Ñ€ÐµÐ±ÑƒÑŽÑ‚ id != 0 Ð´Ð¾ addEntity (ÑÐ¼. RULES.md).
    // Ð‘Ð¾Ð»ÑŒÑˆÐ¾Ð¹ Ð¾Ñ„Ñ„ÑÐµÑ‚, Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ð¿ÐµÑ€ÐµÑÐµÑ‡ÑŒÑÑ Ñ ÑÐµÑ€Ð²ÐµÑ€Ð½Ñ‹Ð¼Ð¸ id.
    private val NEXT_CLIENT_ENTITY_ID = AtomicInteger(2_000_000_000)

    private val npcs = ConcurrentHashMap<UUID, NpcEntry>()

    class NpcEntry(
        val uuid: UUID,
        val ownerModule: String,
        @Volatile var name: String,
        @Volatile var x: Double,
        @Volatile var y: Double,
        @Volatile var z: Double,
        @Volatile var yaw: Float,
        @Volatile var pitch: Float,
        @Volatile var showName: Boolean,
        val skinState: SkinState
    ) {
        @Volatile
        var onClick: Callable? = null

        /** Автоматически поворачивать голову к игроку. */
        @Volatile
        var lookAtPlayer: Boolean = false

        /** Скорость плавного поворота (0..1 за тик). */
        @Volatile
        var lookSpeed: Float = 0.15f

        /** Тип морфа: null/"" — обычный игрок, иначе id сущности ("minecraft:zombie"). */
        @Volatile
        var morphType: String? = null

        internal var morphEntity: net.minecraft.world.entity.Entity? = null

        val displayName: String get() = name
        val skinSupplier: () -> PlayerSkin get() = { skinState.skin }
        @Volatile
        var entity: ScriptFakePlayer? = null
    }

    /**
     * Ð˜Ð·Ð¼ÐµÐ½ÑÐµÐ¼Ð¾Ðµ ÑÐ¾ÑÑ‚Ð¾ÑÐ½Ð¸Ðµ ÑÐºÐ¸Ð½Ð°: Ð¿Ð¾ÐºÐ° ÑÐºÐ¸Ð½ Ð³Ñ€ÑƒÐ·Ð¸Ñ‚ÑÑ â€” Ð´ÐµÑ„Ð¾Ð»Ñ‚, Ð¿Ð¾ÑÐ»Ðµ â€” Ñ€ÐµÐ°Ð»ÑŒÐ½Ñ‹Ð¹.
     */
    class SkinState(@Volatile var skin: PlayerSkin = DefaultPlayerSkin.getDefaultSkin())

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { tick() }
    }

    fun create(
        ownerModule: String,
        name: String,
        x: Double, y: Double, z: Double,
        yaw: Float, pitch: Float,
        showName: Boolean,
        skinState: SkinState
    ): NpcEntry {
        val uuid = UUID.randomUUID()
        val entry = NpcEntry(uuid, ownerModule, name, x, y, z, yaw, pitch, showName, skinState)
        npcs[uuid] = entry
        ScriptConsole.info(ownerModule, "Ð¡Ð¾Ð·Ð´Ð°Ð½ NPC \"$name\" ($x, $y, $z)")
        return entry
    }

    fun remove(uuid: UUID) {
        val entry = npcs.remove(uuid) ?: return
        entry.entity?.discard()
        entry.entity = null
        entry.morphEntity?.discard()
        entry.morphEntity = null
    }

    fun removeForModule(moduleName: String) {
        npcs.values.filter { it.ownerModule.equals(moduleName, ignoreCase = true) }.map { it.uuid }.toList()
            .forEach { remove(it) }
    }

    fun clearAll() {
        npcs.keys.toList().forEach { remove(it) }
    }

    fun getEntry(uuid: UUID): NpcEntry? = npcs[uuid]

    private fun tick() {
        if (npcs.isEmpty()) return
        val level = mc.level
        if (level == null) {
            npcs.values.forEach { it.entity = null }
            return
        }

        npcs.values.forEach { entry ->
            var entity = entry.entity
            // ÐŸÐµÑ€ÐµÑÐ¾Ð·Ð´Ð°Ð½Ð¸Ðµ Ð¿Ð¾ÑÐ»Ðµ ÑÐ¼ÐµÐ½Ñ‹ Ð¼Ð¸Ñ€Ð°/Ð²Ñ‹Ð³Ñ€ÑƒÐ·ÐºÐ¸
            if (entity != null && (entity.isRemoved || entity.level() !== level)) {
                entity = null
                entry.entity = null
            }

            if (entity == null) {
                entity = spawn(level, entry)
            }

            entity?.let { fake ->
                fake.xo = fake.x
                fake.yo = fake.y
                fake.zo = fake.z
                fake.xOld = fake.x
                fake.yOld = fake.y
                fake.zOld = fake.z
                fake.setPos(entry.x, entry.y, entry.z)
                fake.yRotO = fake.yRot
                fake.xRotO = fake.xRot
                fake.yHeadRotO = fake.yHeadRot
                fake.setYRot(entry.yaw)
                fake.setXRot(entry.pitch)
                fake.setYHeadRot(entry.yaw)
                fake.yBodyRot = entry.yaw
            }

            updateMorph(level, entry)
            updateLookAtPlayer(entry)
        }
    }

    /** Морф: спрятать игрока, показать моба-двойника на его месте. */
    private fun updateMorph(level: ClientLevel, entry: NpcEntry) {
        val morphId = entry.morphType
        val current = entry.morphEntity

        // Мир умер — морф больше не валиден
        if (current != null && (current.isRemoved || current.level() !== level)) {
            current.discard()
            entry.morphEntity = null
        } else if (current != null && (morphId == null || morphId.isBlank())) {
            current.discard()
            entry.morphEntity = null
        }

        if (morphId != null && morphId.isNotBlank()) {
            var morph = entry.morphEntity
            val wantType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(morphId)).orElse(null)

            // Смена типа морфа — пересоздаём
            if (morph != null && wantType != null && morph.type !== wantType) {
                morph.discard()
                morph = null
                entry.morphEntity = null
            }

            if (wantType != null && morph == null) {
                morph = try {
                    val newEntity = wantType.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD)
                    if (newEntity != null) {
                        newEntity.setPos(entry.x, entry.y, entry.z)
                        newEntity.setYRot(entry.yaw)
                        newEntity.setYHeadRot(entry.yaw)
                        newEntity.setId(NEXT_CLIENT_ENTITY_ID.getAndIncrement())
                        newEntity.setCustomName(
                            net.minecraft.network.chat.Component.literal(entry.displayName)
                                .withStyle(net.minecraft.ChatFormatting.BOLD)
                        )
                        newEntity.setCustomNameVisible(entry.showName)
                        level.addEntity(newEntity)
                    }
                    newEntity
                } catch (e: Exception) {
                    StarredHeltix.LOGGER.warn("[StarredHeltix] Морф \"$morphId\" не создан: ${e.message}")
                    null
                }
                entry.morphEntity = morph
            }

            // Игрок-двойник скрыт, пока есть морф
            entry.entity?.isInvisible = morph != null

            // Морф двигается вместе с NPC
            morph?.let { m ->
                m.xo = m.x
                m.yo = m.y
                m.zo = m.z
                m.setPos(entry.x, entry.y, entry.z)
                m.yRotO = m.yRot
                m.setYRot(entry.yaw)
                (m as? net.minecraft.world.entity.LivingEntity)?.let { living ->
                    living.yHeadRotO = living.yHeadRot
                    living.setYHeadRot(entry.yaw)
                    living.yBodyRot = entry.yaw
                }
            }
        } else {
            // Без морфа игрок снова видим
            entry.entity?.isInvisible = false
        }
    }

    /** Авто-взгляд головы NPC на игрока (плавный). */
    private fun updateLookAtPlayer(entry: NpcEntry) {
        if (!entry.lookAtPlayer) return
        val player = mc.player ?: return

        val targetYaw = (Math.atan2(player.z - entry.z, player.x - entry.x) * 180.0 / Math.PI).toFloat() - 90f
        val eyeDist = (player.y + player.eyeHeight) - (entry.y + 1.62)
        val horizontal = kotlin.math.sqrt(
            (player.x - entry.x) * (player.x - entry.x) + (player.z - entry.z) * (player.z - entry.z)
        )
        val targetPitch = (-(Math.atan2(eyeDist, horizontal) * 180.0 / Math.PI)).toFloat()

        entry.yaw = rotLerp(entry.yaw, targetYaw, entry.lookSpeed)
        entry.pitch = entry.pitch + (targetPitch - entry.pitch) * entry.lookSpeed
    }

    private fun rotLerp(start: Float, end: Float, pct: Float): Float {
        var diff = end - start
        while (diff < -180f) diff += 360f
        while (diff >= 180f) diff -= 360f
        return start + diff * pct
    }

    private fun spawn(level: ClientLevel, entry: NpcEntry): ScriptFakePlayer? {
        return try {
            val profile = GameProfile(entry.uuid, entry.name)
            val player = ScriptFakePlayer(level, profile, entry)
            player.setPos(entry.x, entry.y, entry.z)
            player.setYRot(entry.yaw)
            player.setXRot(entry.pitch)
            player.setSkinLayers(127.toByte())
            player.setId(NEXT_CLIENT_ENTITY_ID.getAndIncrement())
            level.addEntity(player)
            entry.entity = player
            player
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("[StarredHeltix] ÐÐµ ÑƒÐ´Ð°Ð»Ð¾ÑÑŒ Ð·Ð°ÑÐ¿Ð°Ð²Ð½Ð¸Ñ‚ÑŒ NPC \"${entry.name}\": ${e.message}")
            null
        }
    }

    /**
     * ÐžÐ±Ñ€Ð°Ð±Ð¾Ñ‚ÐºÐ° Ð›ÐšÐœ: Ð²Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ð°ÐµÑ‚ true, ÐµÑÐ»Ð¸ ÐºÐ»Ð¸Ðº Ð¿Ñ€Ð¸ÑˆÑ‘Ð»ÑÑ Ð¿Ð¾ ÑÐºÑ€Ð¸Ð¿Ñ‚Ð¾Ð²Ð¾Ð¼Ñƒ NPC
     * (Ð¸ Ð²Ñ‹Ð·Ð²Ð°Ð» ÐµÐ³Ð¾ ÐºÐ¾Ð»Ð±ÑÐº onClick).
     */
    fun handleAttack(): Boolean {
        if (npcs.isEmpty()) return false
        val hit = mc.hitResult ?: return false
        if (hit.type != HitResult.Type.ENTITY) return false
        val entity = (hit as EntityHitResult).entity
        val entry = npcs.values.firstOrNull { it.entity === entity } ?: return false
        val callback = entry.onClick ?: return false

        CompletableFuture.runAsync {
            SHScriptEngine.withContext { cx ->
                try {
                    callback.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(entry.name))
                } catch (e: Throwable) {
                    ScriptConsole.error(entry.ownerModule, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð² onClick NPC \"${entry.name}\": ${e.message}", e)
                }
            }
        }
        return true
    }

    /** ÐŸÐ»Ð°Ð²Ð½Ñ‹Ð¹ Ñ€Ð°Ð·Ð²Ð¾Ñ€Ð¾Ñ‚ NPC Ðº Ð¸Ð³Ñ€Ð¾ÐºÑƒ (Ð²Ñ‹Ð·Ñ‹Ð²Ð°ÐµÑ‚ÑÑ Ð¸Ð· JS-Ñ…ÑÐ½Ð´Ð»Ð°). */
    fun lookAtPlayer(entry: NpcEntry) {
        val player = mc.player ?: return
        val dx = player.x - entry.x
        val dy = (player.y + player.eyeHeight) - (entry.y + 1.62)
        val dz = player.z - entry.z
        val horizontal = Math.sqrt(dx * dx + dz * dz)
        entry.yaw = (Math.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
        entry.pitch = (-(Math.atan2(dy, horizontal) * 180.0 / Math.PI)).toFloat()
    }

    /** ÐžÐ³Ñ€Ð°Ð½Ð¸Ñ‡ÐµÐ½Ð¸Ðµ ÑƒÐ³Ð»Ð¾Ð² (helper Ð´Ð»Ñ JS). */
    fun wrapDegrees(deg: Float): Float = Mth.wrapDegrees(deg)

    fun cameraDistanceSq(entry: NpcEntry): Double {
        val player = mc.player ?: return Double.MAX_VALUE
        val dx = player.x - entry.x
        val dy = player.y - entry.y
        val dz = player.z - entry.z
        return dx * dx + dy * dy + dz * dz
    }
}

/**
 * Ð—Ð°Ð³Ñ€ÑƒÐ·ÐºÐ° ÑÐºÐ¸Ð½Ð¾Ð² Ð´Ð»Ñ ÑÐºÑ€Ð¸Ð¿Ñ‚Ð¾Ð²Ñ‹Ñ… NPC:
 * - "steve"/"alex"/"default" â€” ÑÑ‚Ð°Ð½Ð´Ð°Ñ€Ñ‚Ð½Ñ‹Ðµ ÑÐºÐ¸Ð½Ñ‹;
 * - "starredheltix:npc/Ð¸Ð¼Ñ.png" Ð¸Ð»Ð¸ "assets:<namespace>:<path>" â€” Ñ‚ÐµÐºÑÑ‚ÑƒÑ€Ð° Ð¸Ð· Ñ€ÐµÑÑƒÑ€ÑÐ¾Ð² Ð¼Ð¾Ð´Ð°;
 * - Ð½Ð¸Ðº Ð¸Ð³Ñ€Ð¾ÐºÐ° (Ð¸Ð»Ð¸ "lic:<Ð½Ð¸Ðº>") â€” UUID Ð¸ ÑÐºÐ¸Ð½ Ñ ÑÐµÑ€Ð²Ð¸ÑÐ¾Ð² Mojang;
 * - UUID-ÑÑ‚Ñ€Ð¾ÐºÐ° â€” ÑÐºÐ¸Ð½ Ð¿Ð¾ Ð¿Ñ€Ð¾Ñ„Ð¸Ð»ÑŽ;
 * - "url:https://..." â€” Ð¿Ñ€ÑÐ¼Ð°Ñ ÑÑÑ‹Ð»ÐºÐ° Ð½Ð° PNG.
 * Ð—Ð°Ð³Ñ€ÑƒÐ·ÐºÐ° Ð°ÑÐ¸Ð½Ñ…Ñ€Ð¾Ð½Ð½Ð°Ñ: Ð´Ð¾ Ð³Ð¾Ñ‚Ð¾Ð²Ð½Ð¾ÑÑ‚Ð¸ Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·ÑƒÐµÑ‚ÑÑ Ð´ÐµÑ„Ð¾Ð»Ñ‚Ð½Ñ‹Ð¹ ÑÐºÐ¸Ð½.
 */
object ScriptNpcSkins {
    private const val MOJANG_PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/"
    private const val MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/"
    private const val SKIN_USER_AGENT = "StarredHeltix-Mod"

    private val mc: Minecraft get() = Minecraft.getInstance()

    fun resolve(source: String?, fallbackUuid: UUID): ScriptNPCManager.SkinState {
        val state = ScriptNPCManager.SkinState()
        if (source.isNullOrBlank() || source.equals("default", true) ||
            source.equals("steve", true) || source.equals("alex", true)
        ) {
            state.skin = defaultFor(fallbackUuid)
            return state
        }

        val trimmed = source.trim()
        when {
            trimmed.startsWith("url:", true) -> loadFromUrl(trimmed.substring(4).trim(), state)
            trimmed.startsWith("assets:", true) -> loadFromAssets(trimmed.substring(7).trim(), state)
            trimmed.startsWith("lic:", true) -> loadFromProfile(trimmed.substring(4).trim(), state, fetchUuid = true)
            isUuid(trimmed) -> loadFromProfile(trimmed, state, fetchUuid = false)
            else -> loadFromProfile(trimmed, state, fetchUuid = true)
        }
        return state
    }

    private fun defaultFor(uuid: UUID): PlayerSkin = DefaultPlayerSkin.get(uuid)

    private fun isUuid(value: String): Boolean = try {
        UUID.fromString(value.trim())
        true
    } catch (e: IllegalArgumentException) {
        false
    }

    /** Ð¡ÐºÐ¸Ð½ Ð¸Ð· Ñ€ÐµÑÑƒÑ€ÑÐ¾Ð² Ð¼Ð¾Ð´Ð°: "starredheltix:npc/mishka" (Ð¸Ð»Ð¸ .png). */
    private fun loadFromAssets(spec: String, state: ScriptNPCManager.SkinState) {
        val withoutExt = if (spec.endsWith(".png")) spec.removeSuffix(".png") else spec
        val id = Identifier.parse(withoutExt)
        val body = ClientAsset.ResourceTexture(id, id)
        state.skin = PlayerSkin.insecure(body, null, null, PlayerModelType.WIDE)
    }

    /** ÐÐ¸Ðº Ð¸Ð»Ð¸ UUID: Ð¿Ð¾Ð´Ñ‚ÑÐ³Ð¸Ð²Ð°ÐµÐ¼ Ð¿Ñ€Ð¾Ñ„Ð¸Ð»ÑŒ Mojang Ð¸ Ñ‚ÐµÐºÑÑ‚ÑƒÑ€Ñƒ. */
    private fun loadFromProfile(source: String, state: ScriptNPCManager.SkinState, fetchUuid: Boolean) {
        CompletableFuture.runAsync {
            try {
                val uuid = if (fetchUuid) fetchUuidByName(source) else UUID.fromString(source)
                if (uuid == null) {
                    ScriptConsole.warn("Skins", "ÐŸÑ€Ð¾Ñ„Ð¸Ð»ÑŒ \"$source\" Ð½Ðµ Ð½Ð°Ð¹Ð´ÐµÐ½, Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·ÑƒÐµÑ‚ÑÑ Ð´ÐµÑ„Ð¾Ð»Ñ‚Ð½Ñ‹Ð¹ ÑÐºÐ¸Ð½")
                    return@runAsync
                }
                val skinInfo = fetchSkinInfo(uuid)
                if (skinInfo == null) {
                    mc.execute { state.skin = defaultFor(uuid) }
                    return@runAsync
                }
                val (url, model) = skinInfo
                registerUrlSkin(url, model) { loaded -> state.skin = loaded }
            } catch (e: Exception) {
                ScriptConsole.warn("Skins", "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐºÐ¸Ð½Ð° Ð´Ð»Ñ \"$source\": ${e.message}")
                mc.execute { state.skin = defaultFor(fallbackUuidValue) }
            }
        }
    }

    private var fallbackUuidValue: UUID = UUID.fromString("8667ba71-b85a-4004-af54-445a97e63e11")

    private fun loadFromUrl(url: String, state: ScriptNPCManager.SkinState) {
        CompletableFuture.runAsync {
            try {
                registerUrlSkin(url, PlayerModelType.WIDE) { loaded -> state.skin = loaded }
            } catch (e: Exception) {
                ScriptConsole.warn("Skins", "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐºÐ¸Ð½Ð° Ð¿Ð¾ URL: ${e.message}")
            }
        }
    }

    private fun registerUrlSkin(url: String, model: PlayerModelType, onLoaded: (PlayerSkin) -> Unit) {
        val textureId = Identifier.fromNamespaceAndPath("starredheltix", "dynamic/npc_skins/" + UUID.randomUUID().toString().replace("-", ""))
        val bytes = httpGetBytes(url) ?: return
        val image = com.mojang.blaze3d.platform.NativeImage.read(bytes)
        mc.execute {
            try {
                val dynamicTexture = net.minecraft.client.renderer.texture.DynamicTexture({ textureId.toString() }, image)
                mc.textureManager.register(textureId, dynamicTexture)
                val body = ClientAsset.ResourceTexture(textureId, textureId)
                onLoaded(PlayerSkin.insecure(body, null, null, model))
            } catch (e: Exception) {
                ScriptConsole.warn("Skins", "ÐÐµ ÑƒÐ´Ð°Ð»Ð¾ÑÑŒ Ð·Ð°Ñ€ÐµÐ³Ð¸ÑÑ‚Ñ€Ð¸Ñ€Ð¾Ð²Ð°Ñ‚ÑŒ ÑÐºÐ¸Ð½: ${e.message}")
            }
        }
    }

    private fun fetchUuidByName(nickname: String): UUID? {
        val json = httpGetString(MOJANG_PROFILE_API + nickname) ?: return null
        val obj = JsonParser.parseString(json).asJsonObject
        if (!obj.has("id")) return null
        val undashed = obj.get("id").asString
        return UUID.fromString(
            undashed.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        )
    }

    private fun fetchSkinInfo(uuid: UUID): Pair<String, PlayerModelType>? {
        val clean = uuid.toString().replace("-", "")
        val json = httpGetString(MOJANG_SESSION_API + clean) ?: return null
        val root = JsonParser.parseString(json).asJsonObject
        val properties = root.getAsJsonArray("properties") ?: return null
        if (properties.size() == 0) return null
        val value = properties[0].asJsonObject.get("value").asString
        val decoded = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        val textures = JsonParser.parseString(decoded).asJsonObject.getAsJsonObject("textures") ?: return null
        if (!textures.has("SKIN")) return null
        val skin = textures.getAsJsonObject("SKIN")
        val url = skin.get("url").asString
        val model = if (skin.has("metadata") &&
            skin.getAsJsonObject("metadata").has("model") &&
            skin.getAsJsonObject("metadata").get("model").asString.equals("slim", true)
        ) PlayerModelType.SLIM else PlayerModelType.WIDE
        return url to model
    }

    private fun httpGetString(url: String): String? {
        return try {
            val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", SKIN_USER_AGENT)
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val result = if (conn.responseCode / 100 == 2) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
            conn.disconnect()
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGetBytes(url: String): ByteArray? {
        return try {
            val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", SKIN_USER_AGENT)
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val result = if (conn.responseCode / 100 == 2) {
                conn.inputStream.use { it.readBytes() }
            } else null
            conn.disconnect()
            result
        } catch (e: Exception) {
            null
        }
    }
}
