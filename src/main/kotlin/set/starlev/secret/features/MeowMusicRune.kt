package set.starlev.secret.features

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Cat
import set.starlev.secret.config.SecretMenuManager
import set.starlev.utils.detectors.EntityDeathDetector
import kotlin.random.Random

object MeowMusicRune {
    private val mc = Minecraft.getInstance()
    private const val MAX_RADIUS = 16.0

    private val meowSounds = listOf(
        SoundEvents.CAT_AMBIENT,
        SoundEvents.CAT_PURREOW,
        SoundEvents.CAT_BEG_FOR_FOOD
    )

    private var lastDeathTime = 0L
    private const val DEATH_COOLDOWN = 50L

    fun init() {
        EntityDeathDetector.registerListener { entity ->
            if (!isEnabled()) return@registerListener
            onEntityDeath(entity)
        }
    }

    private fun isEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.meowMusicRune
    }

    @JvmStatic
    fun isMeowRuneEnabled(): Boolean = isEnabled()

    private fun onEntityDeath(entity: Entity) {
        val player = mc.player ?: return
        val world = mc.level ?: return

        if (entity is Cat) return
        if (entity == player) return

        val distance = player.distanceToSqr(entity)
        if (distance > MAX_RADIUS * MAX_RADIUS) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDeathTime < DEATH_COOLDOWN) return
        lastDeathTime = currentTime

        if (entity is LivingEntity) {
            playMeowSound(entity.x, entity.y, entity.z)
        }
    }

    private fun playMeowSound(x: Double, y: Double, z: Double) {
        val sound = getRandomMeowSound()
        val pitch = 0.8f + (Random.nextFloat() * 0.4f)
        val configVolume = if (SecretMenuManager.isConfigInitialized) {
            SecretMenuManager.secretConfig.funCategory.meowVolume.value
        } else {
            1.0f
        }
        val volume = (configVolume * 4.0f).coerceAtMost(1.0f)

        mc.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, volume))
    }

    fun getRandomMeowSound(): SoundEvent {
        return meowSounds[Random.nextInt(meowSounds.size)]
    }
}