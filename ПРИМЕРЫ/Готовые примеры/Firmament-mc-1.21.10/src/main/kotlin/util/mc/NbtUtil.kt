package moe.nea.firmament.util.mc

import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.Tag
import net.minecraft.nbt.ListTag
import moe.nea.firmament.mixins.accessor.AccessorNbtComponent

fun Iterable<Tag>.toNbtList() = ListTag().also {
	for (element in this) {
		it.add(element)
	}
}

@Suppress("CAST_NEVER_SUCCEEDS")
val CustomData.unsafeNbt get() = (this as AccessorNbtComponent).unsafeNbt_firmament
