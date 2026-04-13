
package moe.nea.firmament.gui.entity

import com.google.gson.JsonObject
import net.minecraft.world.entity.LivingEntity

fun interface EntityModifier {
    fun apply(entity: LivingEntity, info: JsonObject): LivingEntity
}
