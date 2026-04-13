

package moe.nea.firmament.events

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.ResourceLocation
import moe.nea.firmament.util.render.drawGuiTexture

interface SlotRenderEvents {
    val context: GuiGraphics
    val slot: Slot

	fun highlight(sprite: ResourceLocation) {
		context.drawGuiTexture(
			slot.x, slot.y, 0, 16, 16,
			sprite
		)
	}

    data class Before(
        override val context: GuiGraphics, override val slot: Slot,
    ) : FirmamentEvent(),
        SlotRenderEvents {
        companion object : FirmamentEventBus<Before>()
    }

    data class After(
        override val context: GuiGraphics, override val slot: Slot,
    ) : FirmamentEvent(),
        SlotRenderEvents {
        companion object : FirmamentEventBus<After>()
    }
}
