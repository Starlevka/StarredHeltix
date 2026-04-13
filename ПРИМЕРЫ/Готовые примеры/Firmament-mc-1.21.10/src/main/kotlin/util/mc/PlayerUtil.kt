package moe.nea.firmament.util.mc

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player


val Player.mainHandStack get() = this.getItemBySlot(EquipmentSlot.MAINHAND)
