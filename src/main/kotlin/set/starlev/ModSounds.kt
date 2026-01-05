package set.starlev

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

object ModSounds {
    lateinit var VOTING_REMINDER: SoundEvent
    lateinit var UPDATE_AVAILABLE: SoundEvent

    fun register() {
        VOTING_REMINDER = registerSoundEvent("voting_reminder")
        UPDATE_AVAILABLE = registerSoundEvent("update_available")
    }

    private fun registerSoundEvent(name: String): SoundEvent {
        val id = ResourceLocation.fromNamespaceAndPath("starredheltix", name)
        val soundEvent = SoundEvent.createVariableRangeEvent(id)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, soundEvent)
        return soundEvent
    }
}