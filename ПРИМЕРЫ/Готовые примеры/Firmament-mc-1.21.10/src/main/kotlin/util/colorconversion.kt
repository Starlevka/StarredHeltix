

package moe.nea.firmament.util

import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.DyeColor

fun DyeColor.toShedaniel(): me.shedaniel.math.Color =
    me.shedaniel.math.Color.ofOpaque(this.textColor)

fun DyeColor.toTextColor(): TextColor =
    TextColor.fromRgb(this.textColor)

