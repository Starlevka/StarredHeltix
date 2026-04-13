package moe.nea.firmament.events

import org.lwjgl.glfw.GLFW
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import moe.nea.firmament.keybindings.GenericInputAction
import moe.nea.firmament.keybindings.InputModifiers
import moe.nea.firmament.keybindings.SavedKeyBinding

sealed interface HandledScreenInputEvent {
	val screen: Screen
	val input: GenericInputAction
	val modifiers: InputModifiers
}

data class HandledScreenKeyPressedEvent(
    override val screen: Screen,
    override val input: GenericInputAction,
    override val modifiers: InputModifiers,
	// TODO: val isRepeat: Boolean,
) : FirmamentEvent.Cancellable(), HandledScreenInputEvent {
	fun matches(keyBinding: SavedKeyBinding, atLeast: Boolean = false): Boolean {
		return keyBinding.matches(input, modifiers, atLeast)
	}

	fun isLeftClick() = input == GenericInputAction.mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT)
	companion object : FirmamentEventBus<HandledScreenKeyPressedEvent>()
}

data class HandledScreenKeyReleasedEvent(
    override val screen: AbstractContainerScreen<*>,
    override val input: GenericInputAction,
    override val modifiers: InputModifiers,
) : FirmamentEvent.Cancellable(), HandledScreenInputEvent {
	fun matches(keyBinding: SavedKeyBinding, atLeast: Boolean = false): Boolean {
		return keyBinding.matches(input, modifiers, atLeast)
	}

	companion object : FirmamentEventBus<HandledScreenKeyReleasedEvent>()
}
