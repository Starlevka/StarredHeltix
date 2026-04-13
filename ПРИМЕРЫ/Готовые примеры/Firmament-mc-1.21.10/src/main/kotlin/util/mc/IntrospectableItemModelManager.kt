package moe.nea.firmament.util.mc

import net.minecraft.resources.ResourceLocation

interface IntrospectableItemModelManager {
	fun hasModel_firmament(identifier: ResourceLocation): Boolean
}
