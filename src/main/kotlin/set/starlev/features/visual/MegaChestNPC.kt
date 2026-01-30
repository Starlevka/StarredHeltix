package set.starlev.features.visual

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.EntityType
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import set.starlev.StarredHeltix
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import set.starlev.registry.EntityRegistry

class GhostMegaChestMagmaCube(type: EntityType<out MagmaCube>, level: ClientLevel) : MagmaCube(type, level) {
    var isMegaChest: Boolean = true
    private var targetYaw: Float = 0f
    private var jumpDelay: Int = 0

    private var wasInAir: Boolean = false
    private var lastJumpSoundTime: Long = 0

    constructor(level: ClientLevel) : this(EntityType.MAGMA_CUBE, level)

    override fun shouldShowName(): Boolean = true
    override fun getCustomName(): net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.literal("§6§l[МЕГА-ЯЩИК]")
    override fun hasCustomName(): Boolean = true
    override fun isCustomNameVisible(): Boolean = true

    override fun isSensitiveToWater(): Boolean = false

    override fun isNoGravity(): Boolean = false
    override fun isEffectiveAi(): Boolean = true

    override fun tick() {
        if (jumpDelay > 0) jumpDelay--

        val player = Minecraft.getInstance().player
        
        yHeadRot = Mth.rotLerp(0.08f, yHeadRot, targetYaw)
        yBodyRot = Mth.rotLerp(0.05f, yBodyRot, targetYaw)
        
        if (Math.abs(yRot - targetYaw) > 1f) {
            yRot = Mth.rotLerp(0.1f, yRot, targetYaw)
        }

        if (player != null && distanceToSqr(player) < 100.0) {
            // Реже смотрим на игрока (шанс 1 из 60 каждый тик)
            if (random.nextInt(60) == 0) {
                val dX = player.x - x
                val dZ = player.z - z
                targetYaw = (Mth.atan2(dZ, dX) * (180.0 / Math.PI)).toFloat() - 90f
            }
        } else if (random.nextInt(100) == 0) {
            targetYaw += (random.nextFloat() * 180f - 90f)
        }

        if (onGround() && jumpDelay <= 0) {
            if (random.nextInt(30) == 0) {
                jumpFromGround()
                
                // Звук прыжка слизня
                val now = System.currentTimeMillis()
                if (now - lastJumpSoundTime > 500) {
                    Minecraft.getInstance().level?.playSound(Minecraft.getInstance().player, x, y, z, SoundEvents.SLIME_JUMP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f)
                    lastJumpSoundTime = now
                }

                // Увеличиваем высоту прыжка (стандарт 0.42 -> 0.55 для +0.5 блока высоты)
                deltaMovement = Vec3(deltaMovement.x, 0.55, deltaMovement.z)
                
                jumpDelay = 20 + random.nextInt(20)

                if (random.nextBoolean()) {
                    targetYaw += (random.nextFloat() * 90f - 45f)
                }

                // Уменьшаем дальность прыжка (0.5f -> 0.25f)
                val speed = 0.25f
                val rad = targetYaw * (Math.PI / 180.0).toFloat()
                val moveX = -Mth.sin(rad) * speed
                val moveZ = Mth.cos(rad) * speed
                
                deltaMovement = deltaMovement.add(moveX.toDouble(), 0.0, moveZ.toDouble())
                hasImpulse = true
            }
        }

        // Отталкивание от игрока
        if (player != null && distanceToSqr(player) < 1.0) {
            val dX = x - player.x
            val dZ = z - player.z
            val length = Math.sqrt(dX * dX + dZ * dZ)
            if (length > 0) {
                val pushX = (dX / length) * 0.2
                val pushZ = (dZ / length) * 0.2
                deltaMovement = deltaMovement.add(pushX, 0.0, pushZ)
                hasImpulse = true
            }
        }

        // Вызываем move для обработки гравитации и коллизий
        val lastDeltaY = deltaMovement.y
        move(MoverType.SELF, deltaMovement)
        
        // Ручное применение гравитации, если super.tick() её игнорирует
        if (!onGround()) {
            deltaMovement = deltaMovement.add(0.0, -0.08, 0.0)
            if (deltaMovement.y < -0.1) wasInAir = true
        } else {
            // Если приземлились, создаем частицы магмового куба
            if (wasInAir && lastDeltaY < -0.05) {
                for (i in 0 until 15) {
                    level().addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 1.5, y, z + (random.nextDouble() - 0.5) * 1.5, 0.0, 0.1, 0.0)
                    level().addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 1.5, y, z + (random.nextDouble() - 0.5) * 1.5, 0.0, 0.1, 0.0)
                }
                Minecraft.getInstance().level?.playSound(Minecraft.getInstance().player, x, y, z, SoundEvents.SLIME_SQUISH, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.5f)
                wasInAir = false
            }
            deltaMovement = deltaMovement.multiply(0.8, 1.0, 0.8) // Трение об землю
        }

        super.tick()
    }
}

object MegaChestNPCHandler {
    private const val OVERWORLD_DIM_ID = "minecraft:overworld"
    private val mc = Minecraft.getInstance()
    private val megaChests = ConcurrentHashMap<UUID, Any>()
    private val random = Random(42) // Фиксированный сид для постоянства позиций
    
    private var lastInteractionTime = 0L
    private var hasSpawnedInitialChests = false

    private val DIALOGUES = listOf(
        "Копать-копать-копать!",
        "Привет, я тот самый Мега-ящик!",
        "Лёва-бабуин - это факт вселенной.",
        "StarredHeltix код для секретных настроек... А фиг вам! >:P",
        "Я какащке :3",
        "Обожаю Хелтикс! Особенно прыгать в Деревне!",
        "У меня есть меч Хайпа! А у тебя что?",
        "Если мама удалит мне Роблокс, то я стану фембоем. TwT",
        "СО2 не регает.",
        "Я лишь иллюзия, а ты просто Л.",
        "Когда я запустил StarredHeltix.exe, у меня запустилась ратка!",
        "Слушай, а не слушая, я просто блёбик.",
        "Звездофрукт, я, леденец... *рыгает*",
        "Когда Хелтикс на Хайтейле?",
        "Когда Хайтейл на Хелтикс?",
        "Хайтейл - моя новая суровая жизнь!",
        "Платили ли мне за рекламу? Думаю, да. Меня просто вшили в эту систему.",
        "ЛОООООР",
        "Вы любите ЛООР? Его можно было увидеть на ФорджКрафте! Только вот Лёва уже спился.",
        "Как играть на вашем Хелтиксе, когда слишком много нонов и банят Мега-ящиков...? ;(",
        "Мы есть МЕГА!",
        "I AM A MEGA!",
        "Сблевухня - одна из лучших Майнкрафт карт во вселенной, которая известна лишь Лёве.",
        "- ..- - / -... -.-- .-.. / ... - .- .-. .-.. . ...- -.-.--",
        "..-. -.- / -....- / ---. .. - -.-- -.-.--",
        "..-. -.- / ---. .. - -.-- / -....- / .-.. .. ---- -..- / -. .- / -.- -- ..--- --..-- / - .. .--. --- / -.- ... ..--- / .-- / -- .- .--- -. -.- .-. .- ..-. - .",
        "-. . --. .-. -.-- / .-- ... . / .--. .. -.. --- .-. .- ... -.--",
        "Кличка Лёвы - это ... - .- .-. --. . .---",
        "Смотрите у меня вышло новое видео! @megachromex",
        "Я MegaChromeX и я люблю пиво.",
        "Пей пиво на заре, пей пиво перед сном...",
        "Открой базу и верни мне бр бр патапима... :C",
        "Фортинайте ор пабадже? Хайтейл.",
        "Форточку отпройке!",
        "Я тебе ничего не скажу!",
        "Ты мне не нравишься, пока что)",
        "Эй, убери руки!",
        "Я устал с тобой болтать...",
        "Бара-бара-бара, бере-бере-бере",
        "Совет от Меги: станьте Мегой",
        "Мега-ящик из Бравл Старса я, а кто же ещё?",
        "Бабл квас надоел мне, что ж продам себя что-ли...",
        "Мега-рыцарь уже тут!",
        "Копать!!!",
        "*злится*",
        "*шакалит*",
        "*вспоминает молодость и плачет*",
        "*смотрит видео*",
        "*беседует с Лёвой по телефону*",
        "Когда обновление мода StarredHeltix?!",
        "Интересный факт: Вас сегодня забанят! C:<",
        "Интересный факт: Я забанился с Лёвой после 100-летия Хелтикс Скайблока! Только я тут крутой, а он тупой.",
        "Новости Меги: Я забанил Лёву, а он меня... *отключение*",
        "Я люблю копать! Но я состою из хлипкой слизи...",
        "Создал ли меня Лёва? Только Л знает.",
        "Факт дня: Я не гей! *запивает стаканом пивка*",
        "За рудой! Копать копать копать копать!",
        "В ТГК @starlevka и @megastales всегда потрясные новости!"
    )
    
    // Список фиксированных или сгенерированных позиций
    private val positions = mutableListOf<BlockPos>()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!StarredHeltix.feature.visuals.megaChests.enabled) {
                clearAll()
                hasSpawnedInitialChests = false
                return@register
            }

            updateMegaChests()
        }
    }

    private fun updateMegaChests() {
        val level = mc.level ?: run {
            hasSpawnedInitialChests = false
            return
        }

        val config = StarredHeltix.feature.visuals.megaChests

        // Spawn initial chests if enabled and not already done
        if (config.spawnAtCoords) {
            if (!hasSpawnedInitialChests) {
                hasSpawnedInitialChests = true
                val spawnPos = BlockPos(-2, 200, -42)

                for (i in 0 until 8) {
                    val offsetPos = spawnPos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1)
                    spawnChest(level, offsetPos, 3, 5000 + i)
                }
            }
        } else if (hasSpawnedInitialChests) {
            // If disabled but was spawned, clear them
            for (i in 0 until 8) {
                removeChestAtIndex(5000 + i)
            }
            hasSpawnedInitialChests = false
        }

        updateChests(level)
    }

    fun spawnDebugChest() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        val pos = player.blockPosition()
        
        // Генерируем временный UUID для дебаг-честа
        val debugId = (megaChests.size + 1000)
        spawnChest(level, pos, 3, debugId)
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a[StarredHeltix] Мега-ящик заспавнен в вашей позиции!"), false)
    }

    fun handleAttack(): Boolean {
        val player = mc.player ?: return false

        val target = mc.hitResult
        if (target != null && target.type == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            val entityTarget = target as net.minecraft.world.phys.EntityHitResult
            val entity = entityTarget.entity

            if (entity is GhostMegaChestMagmaCube) {
                val now = System.currentTimeMillis()
                if (now - lastInteractionTime < 1000L) return true
                lastInteractionTime = now

                val message = DIALOGUES[random.nextInt(DIALOGUES.size)]
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§6§l[МЕГА-ЯЩИК]§f: $message"), false)
                return true
            }
        }
        return false
    }

    private fun generateRandomPositions() {
        // Генерируем несколько позиций на основе сида и широкого диапазона
        val seed = 12345L
        val random = Random(seed)
        
        for (i in 0 until 10) {
            val x = random.nextInt(2000) - 1000
            val z = random.nextInt(2000) - 1000
            // Ищем подходящую высоту Y (на поверхности)
            // Так как мы на клиенте, мы можем получить высоту только если чанк загружен
            // Поэтому для "предварительных" позиций ставим среднюю высоту
            positions.add(BlockPos(x, 70, z))
        }
    }

    private fun updateChests(level: ClientLevel) {
        val player = mc.player ?: return
        
        if (positions.isEmpty()) {
            generateRandomPositions()
        }

        val config = StarredHeltix.feature.visuals.megaChests
        
        positions.forEachIndexed { index, pos ->
            val distSq = player.blockPosition().distSqr(pos)
            
            if (distSq < 100 * 100) {
                // Если мы близко, пытаемся уточнить высоту Y по блокам
                var targetPos = pos
                if (level.isLoaded(pos)) {
                    val topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.x, pos.z)
                    if (topY > 0) {
                        targetPos = BlockPos(pos.x, topY, pos.z)
                    }
                }

                if (!megaChests.containsKey(UUID.nameUUIDFromBytes("megachest-$index".toByteArray()))) {
                    spawnChest(level, targetPos, 3, index)
                }
            } else {
                removeChestAtIndex(index)
            }
        }
    }

    private fun spawnChest(level: ClientLevel, pos: BlockPos, size: Int, index: Int) {
        val uuid = UUID.nameUUIDFromBytes("megachest-$index".toByteArray())
        
        val entity = GhostMegaChestMagmaCube(EntityRegistry.MEGA_CHEST_MAGMA, level).apply {
            setPos(pos.x.toDouble() + 0.5, pos.y.toDouble(), pos.z.toDouble() + 0.5)
            setSize(size, true)
            setUUID(uuid)
        }
        
        megaChests[uuid] = entity
        level.addEntity(entity)
    }

    private fun removeChestAtIndex(index: Int) {
        val uuid = UUID.nameUUIDFromBytes("megachest-$index".toByteArray())
        val entity = megaChests[uuid] as? net.minecraft.world.entity.Entity
        entity?.discard()
        megaChests.remove(uuid)
    }

    private fun clearAll() {
        megaChests.values.forEach { 
            (it as net.minecraft.world.entity.Entity).discard()
        }
        megaChests.clear()
    }
}
