package set.starlev

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

object ModSounds {
    lateinit var VOTING_REMINDER: SoundEvent
    lateinit var UPDATE_AVAILABLE: SoundEvent
    lateinit var NPC_1: SoundEvent
    lateinit var NPC_2: SoundEvent
    lateinit var NPC_3: SoundEvent
    lateinit var NPC_4: SoundEvent

    fun register() {
        VOTING_REMINDER = registerSoundEvent("voting_reminder")
        UPDATE_AVAILABLE = registerSoundEvent("update_available")
        NPC_1 = registerSoundEvent("dialogue.1")
        NPC_2 = registerSoundEvent("dialogue.2")
        NPC_3 = registerSoundEvent("dialogue.3")
        NPC_4 = registerSoundEvent("dialogue.4")
    }

    private fun registerSoundEvent(name: String): SoundEvent {
        val id = ResourceLocation.fromNamespaceAndPath("starredheltix", name)
        val soundEvent = SoundEvent.createVariableRangeEvent(id)
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, soundEvent)
        return soundEvent
    }
}