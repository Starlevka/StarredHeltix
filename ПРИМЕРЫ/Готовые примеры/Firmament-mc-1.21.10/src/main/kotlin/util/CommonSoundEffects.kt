

package moe.nea.firmament.util

import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.resources.ResourceLocation

// TODO: Replace these with custom sound events that just re use the vanilla ogg s
object CommonSoundEffects {
    fun playSound(identifier: ResourceLocation) {
        MC.soundManager.play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(identifier), 1F))
    }

    fun playFailure() {
        playSound(ResourceLocation.fromNamespaceAndPath("minecraft", "block.anvil.place"))
    }

    fun playSuccess() {
        playDing()
    }

    fun playDing() {
        playSound(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.arrow.hit_player"))
    }
}
