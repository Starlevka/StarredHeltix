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
    
    override fun getCustomName(): Component = Component.literal("§b${this.gameProfile.name}")
    
    override fun hasCustomName(): Boolean = true
    
    override fun isCustomNameVisible(): Boolean = true
    
    fun setSkinLayers(layers: Byte) {
        this.entityData.set(net.minecraft.world.entity.Avatar.DATA_PLAYER_MODE_CUSTOMISATION, layers)
    }

    override fun getSkin(): net.minecraft.world.entity.player.PlayerSkin {
        val skinLocation = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/penguin.png")
        val bodyTex = ClientAsset.ResourceTexture(skinLocation, skinLocation)
        return net.minecraft.world.entity.player.PlayerSkin.insecure(bodyTex, null, null, PlayerModelType.SLIM)
    }

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean = true
}

object GhostNPCHandler {
    private val mc = Minecraft.getInstance()
    private var NPC_POS = BlockPos(2, 72, -89)
    private val NPC_NAME = "Penguin"
    private val NPC_UUID = UUID.fromString("8667ba71-b85a-4004-af54-445a97e63e11")
    private val OVERWORLD_DIM_ID = "minecraft:overworld"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val DIALOGUE = listOf(
        "§7[§cPenguin§7]§f: Нашему Скайблоку исполнилось целых 100 лет! И только недавно мы оказались в 2026 году!",
        "§7[§cPenguin§7]§f: Спасибо Вам, что вы есть и с интересом проводите время на сервере! §4:D",
        "§7[§cPenguin§7]§f: Держи кусочек тортика 🍰!"
    )

    private var currentDialogueIndex = -1
    private var isDialogueActive = false
    private var lastClickTime = 0L
    private var lastHintTime = 0L
    private var interactionCount = 0
    private var lastDialogueStepTime = 0L

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
        ClientTickEvents.END_CLIENT_TICK.register {
            updateFakePlayer()
            updateDialogueAuto()
        }
    }

    private fun updateDialogueAuto() {
        if (!isDialogueActive) return
        
        val now = System.currentTimeMillis()
        if (now - lastDialogueStepTime > 1500L) { // Пауза 1.5 секунды между репликами
            lastDialogueStepTime = now
            playNextDialogueLine()
        }
    }

    private fun checkHint() {
        val player = mc.player ?: return
        if (currentDialogueIndex != -1) return
        
        val hasTalked = StarredHeltix.feature.visuals.newYear.hasTalkedToPenguin
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
                player.displayClientMessage(Component.literal("§b[§cPenguin§b]§f: Эй! Подойди и §bкликни по мне ЛКМ§f, чтобы поговорить!"), false)
            }
        }
    }

    fun handleAttack(): Boolean {
        val player = mc.player ?: return false
        val fake = fakePlayer ?: return false
        
        val target = mc.hitResult
        if (target != null && target.type == HitResult.Type.ENTITY) {
            val entityTarget = target as EntityHitResult
            if (entityTarget.entity == fake) {
                if (!isDialogueActive) {
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
    }

    private fun playNextDialogueLine() {
        mc.execute {
            if (currentDialogueIndex >= DIALOGUE.size) {
                finishDialogue()
                return@execute
            }

            val line = DIALOGUE[currentDialogueIndex]
            mc.player?.displayClientMessage(Component.literal(line), false)

            currentDialogueIndex++
            if (currentDialogueIndex >= DIALOGUE.size) {
                finishDialogue()
            }
        }
    }

    private fun finishDialogue() {
        isDialogueActive = false
        currentDialogueIndex = -1
        StarredHeltix.feature.visuals.newYear.hasTalkedToPenguin = true
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
        interactionCount++

        if (StarredHeltix.feature.visuals.newYear.hasTalkedToPenguin) {
            val achievements = mapOf(
                10 to "§6§l[Достижение] §eЛюбитель сладкого! §f(10 кусочков тортика)",
                20 to "§6§l[Достижение] §eЦенитель десертов! §f(§l20 кусочков тортика, ура!§f)",
                50 to "§6§l[Достижение] §eТортовый магнат! §f(§c50 кусочков тортика, ты не думаешь, что это уже много?§f)",
                100 to "§6§l[Достижение] §eСахарный король! §f(§c100 кусочков тортика, юху!!! ДИАБЕТ! §f)",
                500 to "§6§l[Достижение] §eКондитерский мастер! §f(§c500 кусочков тортика, куда тебе столько???§f)",
                1000 to "§6§l[Достижение] §eЛегендарный едок! §f(§c§l1000 кусочков тортика! ЗАЧЕМ ТЕБЕ СТОЛЬКО?§f)",
                5000 to "§6§l[Достижение] §dТортовая аномалия! §f(§c§l5000 кусочков тортика! Вы уже накормили ВЕСЬ СЕРВЕР!)",
                10000 to "§6§l[Достижение] §dСладкая бесконечность! §f(§4§l10000 кусочков тортика! Когда закончится запас тортиков?§f)",
                50000 to "§6§l[Достижение] §bПовелитель сахара! §f(§4§l50000 кусочков тортика!!! Я СЕЙЧАС ЧУВСТВУЮ СЕБЯ ТОРТИКОМ!!! §f)",
                100000 to "§6§l[Достижение] §5БОГ ТОРТИКОВ! §f(§4§l100000 кусочков тортика!!! 🍰 !!! ВЫ ОКОНЧАТЕЛЬНО СТАЛИ МАГНАТОМ КУСОЧКОВ ТОРТИКА !!!§f)"
            )

            mc.player?.displayClientMessage(Component.literal("§7[§cPenguin§7]§f: Мы ведь уже говорили! С юбилеем Скайблока! Вот тебе ещё кусочек тортика 🍰."), false)
            mc.player?.displayClientMessage(Component.literal("§8[§7Статистика§8] §fВы получили уже §b$interactionCount §fкусочков тортика!"), false)
            
            achievements[interactionCount]?.let { achievementMsg ->
                mc.player?.displayClientMessage(Component.literal(achievementMsg), false)
                mc.player?.playSound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
            }
            return
        }

        isDialogueActive = true
        currentDialogueIndex = 0
        lastDialogueStepTime = System.currentTimeMillis()
        playNextDialogueLine()
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

                // Плавная анимация полета с использованием System.currentTimeMillis()
                // Это исправляет "рванность" в сетевой игре, так как время не зависит от тиков сервера
                val floatRange = 0.4 // Амплитуда
                val floatSpeed = 0.002 // Скорость (для millis)
                
                val time = System.currentTimeMillis()
                val verticalOffset = 1.2 + Math.sin(time.toDouble() * floatSpeed) * floatRange
                
                // Обновляем старые координаты перед установкой новых для плавной интерполяции
                fake.xo = fake.x
                fake.yo = fake.y
                fake.zo = fake.z
                fake.xOld = fake.x
                fake.yOld = fake.y
                fake.zOld = fake.z
                
                val targetX = NPC_POS.x.toDouble() + 0.5
                val targetY = NPC_POS.y.toDouble() + verticalOffset
                val targetZ = NPC_POS.z.toDouble() + 0.5

                fake.setPos(targetX, targetY, targetZ)
                
                fake.yRotO = fake.yRot
                fake.xRotO = fake.xRot
                fake.yHeadRotO = fake.yHeadRot
                
                if (!level.entitiesForRendering().contains(fake)) {
                    StarredHeltix.LOGGER.warn("Ghost NPC was missing from rendering list, re-adding...")
                    level.addEntity(fake)
                }
                val dx = player.x - fake.x
                val dy = (player.y + player.eyeHeight) - (fake.eyeHeight + fake.y)
                val dz = player.z - fake.z
                val distanceToPlayer = Math.sqrt(dx * dx + dz * dz)
                val totalDistanceSq = player.position().distanceToSqr(fake.position())

                if (totalDistanceSq < 144.0) { // Within 12 blocks
                    targetYaw = (Math.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
                    targetPitch = (-(Math.atan2(dy, distanceToPlayer) * 180.0 / Math.PI)).toFloat()
                } else { // Further than 12 blocks - Idle look
                    if (now - lastIdleLookTime > 4000L) {
                        targetYaw = (random.nextFloat() * 360f) - 180f
                        targetPitch = (random.nextFloat() * 60f) - 30f // Look slightly up/down
                        lastIdleLookTime = now
                    }
                }
                
                currentYaw = rotLerp(currentYaw, targetYaw, 0.1f)
                currentPitch = rotLerp(currentPitch, targetPitch, 0.1f)
                
                fake.setYRot(currentYaw)
                fake.setXRot(currentPitch)
                fake.setYHeadRot(currentYaw)

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

