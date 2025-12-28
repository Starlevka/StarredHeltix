package set.starlev.features.visual

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import set.starlev.StarredHeltix
import set.starlev.ModSounds
import set.starlev.render.RenderEvents
import set.starlev.render.RenderContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.core.ClientAsset
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GhostPlayer(level: ClientLevel, profile: GameProfile) : RemotePlayer(level, profile) {
    
    override fun shouldShowName(): Boolean = true
    
    override fun getCustomName(): Component = Component.literal("§eМега-Ящик")
    
    override fun hasCustomName(): Boolean = true
    
    override fun isCustomNameVisible(): Boolean = true

    fun setSkinLayers(layers: Byte) {
        this.entityData.set(net.minecraft.world.entity.Avatar.DATA_PLAYER_MODE_CUSTOMISATION, layers)
    }

    override fun getSkin(): net.minecraft.world.entity.player.PlayerSkin {
        val skinLocation = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/megachromex.png")
        val bodyTex = ClientAsset.ResourceTexture(skinLocation, skinLocation)
        return net.minecraft.world.entity.player.PlayerSkin.insecure(bodyTex, null, null, PlayerModelType.WIDE)
    }

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean = true
}

object GhostNPCHandler {
    private val mc = Minecraft.getInstance()
    private var NPC_POS = BlockPos(2, 70, -89)
    private val NPC_NAME = "Мега-Ящик"
    private val NPC_UUID = UUID.fromString("8667ba71-b85a-4004-af54-445a97e63e11")
    private val OVERWORLD_DIM_ID = "minecraft:overworld"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val DIALOGUE = listOf(
        "§e[Мега-Ящик]§f: Привет! Я — тот самый Мега-Ящик (§dMegaChromeX§f)!",
        "§e[Мега-Ящик]§f: Ты нашел меня... Но я лишь проекция мода §6StarredHeltix§f.",
        "§e[Мега-Ящик]§f: Я хотел бы передать тебе спасибо, что ты играешь, общаешься, хорошо проводишь время, преодолеваешь трудности и находишь новых друзей на §aHeltix Skyblock§f!",
        "§e[Мега-Ящик]§f: §cПоздравляю нас с новым 2026 годом!"
    )

    private val DIALOGUE_SOUNDS = listOf(
        ModSounds.NPC_1,
        ModSounds.NPC_2,
        ModSounds.NPC_3,
        ModSounds.NPC_4
    )

    private val DIALOGUE_DELAYS = listOf(3000L, 4000L, 9000L, 4000L)

    private var currentDialogueIndex = -1
    private var isDialogueActive = false
    private var isChoicePending = false
    private var isSoundMode = false
    private var lastClickTime = 0L
    private var lastHintTime = 0L

    private var fakePlayer: GhostPlayer? = null
    private var targetYaw = 0f
    private var currentYaw = 0f
    private var targetPitch = 0f
    private var currentPitch = 0f
    private var lastIdleLookTime = 0L
    private val random = Random()

    fun setPos(pos: BlockPos) {
        NPC_POS = pos
    }

    fun getPos(): BlockPos = NPC_POS

    fun init() {
        RenderEvents.register { context ->
            renderName(context)
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            updateFakePlayer()
        }
    }

    private fun checkHint() {
        val player = mc.player ?: return
        if (currentDialogueIndex != -1) return
        
        val hasTalked = StarredHeltix.feature.misc.newYear.hasTalkedToNPC
        if (hasTalked) return

        val npcVec = net.minecraft.world.phys.Vec3(
            NPC_POS.x.toDouble() + 0.5,
            NPC_POS.y.toDouble() + 1.0,
            NPC_POS.z.toDouble() + 0.5
        )

        // Радиус 12 блоков (12 * 12 = 144)
        if (player.position().distanceToSqr(npcVec) < 144.0) {
            val now = System.currentTimeMillis()
            if (now - lastHintTime > 30000L) {
                lastHintTime = now
                player.displayClientMessage(Component.literal("§e[Мега-Ящик]§f: Эй! Подойди и §bкликни по мне ЛКМ§f, чтобы поговорить!"), false)
            }
        }
    }

    private fun renderName(context: RenderContext) {
        val fake = fakePlayer ?: return
        val player = mc.player ?: return
        
        if (fake.isRemoved) return
        
        val pos = fake.position()
        val x = pos.x
        val y = pos.y + fake.boundingBox.maxY - fake.y + 0.5
        val z = pos.z
        
        val distSq = player.position().distanceToSqr(pos)
        if (distSq > 400.0) return

        val matrices = context.matrices
        matrices.pushPose()
        matrices.translate(x - context.camera.position.x, y - context.camera.position.y, z - context.camera.position.z)
        
        matrices.mulPose(context.camera.rotation())
        matrices.scale(-0.025f, -0.025f, 0.025f)
        
        val name = Component.literal(NPC_NAME)
        val width = mc.font.width(name)
        
        mc.font.drawInBatch(
            name,
            -width.toFloat() / 2f,
            0f,
            0xFFFFFF,
            true,
            matrices.last()!!.pose(),
            context.vertexConsumers,
            net.minecraft.client.gui.Font.DisplayMode.NORMAL,
            0,
            15728880
        )
        
        matrices.popPose()
    }

    fun handleAttack(): Boolean {
        val player = mc.player ?: return false
        val fake = fakePlayer ?: return false
        
        val target = mc.hitResult
        if (target != null && target.type == HitResult.Type.ENTITY) {
            val entityTarget = target as EntityHitResult
            if (entityTarget.entity == fake) {
                if (isDialogueActive) {
                    if (!isChoicePending && !isSoundMode) {
                        playNextDialogueLine(withSound = false)
                    }
                } else {
                    startDialogue()
                }
                return true
            }
        }
        return false
    }

    fun resetDialogue() {
        isDialogueActive = false
        currentDialogueIndex = -1
        isChoicePending = false
        isSoundMode = false
    }

    fun handleDialogueChoice(type: String) {
        if (!isChoicePending) return
        isChoicePending = false
        
        if (type == "sound") {
            isSoundMode = true
            startSoundDialogue()
        } else {
            isSoundMode = false
            startTextDialogue()
        }
    }

    private fun startTextDialogue() {
        playNextDialogueLine(withSound = false)
    }

    private fun startSoundDialogue() {
        playNextDialogueLine(withSound = true)
    }

    private fun playNextDialogueLine(withSound: Boolean = false) {
        mc.execute {
            if (currentDialogueIndex >= DIALOGUE.size) {
                finishDialogue()
                return@execute
            }

            val line = DIALOGUE[currentDialogueIndex]
            mc.player?.displayClientMessage(Component.literal(line), false)

            if (withSound) {
                val sound = DIALOGUE_SOUNDS[currentDialogueIndex]
                mc.player?.playSound(sound, 1.0f, 1.0f)
                
                val delay = DIALOGUE_DELAYS[currentDialogueIndex]
                currentDialogueIndex++
                
                scheduler.schedule({
                    playNextDialogueLine(withSound)
                }, delay, TimeUnit.MILLISECONDS)
            } else {
                currentDialogueIndex++
                if (currentDialogueIndex >= DIALOGUE.size) {
                    finishDialogue()
                }
            }
        }
    }

    private fun finishDialogue() {
        isDialogueActive = false
        currentDialogueIndex = -1
        isSoundMode = false
        StarredHeltix.feature.misc.newYear.hasTalkedToNPC = true
    }

    private fun startDialogue() {
        if (isDialogueActive) return
        
        val player = mc.player ?: return
        val npcVec = net.minecraft.world.phys.Vec3(
            NPC_POS.x.toDouble() + 0.5,
            NPC_POS.y.toDouble() + 1.0,
            NPC_POS.z.toDouble() + 0.5
        )
        
        // Проверка дистанции перед началом диалога (12 блоков)
        if (player.position().distanceToSqr(npcVec) > 144.0) {
            return
        }
        
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 1000) return
        lastClickTime = now

        if (StarredHeltix.feature.misc.newYear.hasTalkedToNPC) {
            mc.player?.displayClientMessage(Component.literal("§e[Мега-Ящик]§f: Мы ведь уже говорили! С Новым Годом тебя еще раз! §c❤"), false)
            return
        }

        isDialogueActive = true
        isChoicePending = true
        currentDialogueIndex = 0
        
        val message = Component.literal("§e[Мега-Ящик]§f: Выбери режим диалога: ")
            .append(Component.literal("§b[ЗВУК]")
                .withStyle { s -> s.withClickEvent(ClickEvent.RunCommand("/sh_dialogue sound"))
                                   .withHoverEvent(HoverEvent.ShowText(Component.literal("Включить озвучку"))) })
            .append(Component.literal(" §7| "))
            .append(Component.literal("§a[ТЕКСТ]")
                .withStyle { s -> s.withClickEvent(ClickEvent.RunCommand("/sh_dialogue text"))
                                   .withHoverEvent(HoverEvent.ShowText(Component.literal("Только текстовый режим"))) })
        
        mc.player?.displayClientMessage(message, false)
    }

    fun updateFakePlayer() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        val now = System.currentTimeMillis()
        
        // Check dimension
        val currentDim = level.dimension().location().toString()
        if (currentDim != OVERWORLD_DIM_ID) {
            removeFakePlayer()
            return
        }
        
        val distSq = player.blockPosition().distSqr(NPC_POS)
        
        if (distSq < 2500) { // 50 blocks
            if (fakePlayer == null) {
                StarredHeltix.LOGGER.info("Spawning Ghost NPC at $NPC_POS")
                val profile = GameProfile(NPC_UUID, NPC_NAME)
                val newPlayer = GhostPlayer(level, profile)
                newPlayer.setPos(NPC_POS.x.toDouble() + 0.5, NPC_POS.y.toDouble(), NPC_POS.z.toDouble() + 0.5)
                newPlayer.setYRot(0f)
                newPlayer.setXRot(0f)
                newPlayer.setSkinLayers(127.toByte())
                
                // В 1.21.10 для отображения RemotePlayer на клиенте без сервера
                // может потребоваться явно добавить его в список сущностей уровня
                level.addEntity(newPlayer)
                fakePlayer = newPlayer
                StarredHeltix.LOGGER.info("Ghost NPC spawned successfully with ID: ${newPlayer.id}")
            }
            
            fakePlayer?.let { fake ->
                if (fake.isRemoved) {
                    StarredHeltix.LOGGER.warn("Ghost NPC was removed from level, recreating...")
                    fakePlayer = null
                    return@let
                }

                if (!level.entitiesForRendering().contains(fake)) {
                    StarredHeltix.LOGGER.warn("Ghost NPC was missing from rendering list, re-adding...")
                    level.addEntity(fake)
                }
                val dx = player.x - fake.x
                val dy = (player.y + player.eyeHeight) - (fake.y + fake.eyeHeight)
                val dz = player.z - fake.z
                val distance = Math.sqrt(dx * dx + dz * dz)
                
                targetYaw = (Math.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
                targetPitch = (-(Math.atan2(dy, distance) * 180.0 / Math.PI)).toFloat()
                
                currentYaw = rotLerp(currentYaw, targetYaw, 0.15f)
                currentPitch = rotLerp(currentPitch, targetPitch, 0.15f)
                
                fake.setYRot(currentYaw)
                fake.setXRot(currentPitch)
                fake.setYHeadRot(currentYaw)
                
                // Idle look logic
                if (now - lastIdleLookTime > 5000L) {
                    if (random.nextFloat() < 0.2f) {
                        targetYaw += (random.nextFloat() - 0.5f) * 60f
                        targetPitch += (random.nextFloat() - 0.5f) * 30f
                        lastIdleLookTime = now
                    }
                }

                checkHint()
            }
        } else {
            removeFakePlayer()
        }
    }

    private fun removeFakePlayer() {
        fakePlayer?.let {
            it.discard()
            fakePlayer = null
        }
    }

    private fun rotLerp(start: Float, end: Float, pct: Float): Float {
        var diff = end - start
        while (diff < -180f) diff += 360f
        while (diff >= 180f) diff -= 360f
        return start + diff * pct
    }
}

