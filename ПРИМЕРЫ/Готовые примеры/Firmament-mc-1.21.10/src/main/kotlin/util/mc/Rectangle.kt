package moe.nea.firmament.util.mc

import me.shedaniel.math.Rectangle
import net.minecraft.client.gui.navigation.ScreenAxis
import net.minecraft.client.gui.navigation.ScreenRectangle

fun Rectangle.asScreenRectangle() =
	ScreenRectangle.of(
		ScreenAxis.HORIZONTAL,
		x, y, width, height
	)
