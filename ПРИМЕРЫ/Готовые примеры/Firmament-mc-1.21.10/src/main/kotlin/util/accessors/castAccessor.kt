@file:OptIn(ExperimentalContracts::class)

package moe.nea.firmament.util.accessors

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import moe.nea.firmament.mixins.accessor.AccessorHandledScreen


inline fun AbstractContainerScreen<*>.castAccessor(): AccessorHandledScreen {
	contract {
		returns() implies (this@castAccessor is AccessorHandledScreen)
	}
	return this as AccessorHandledScreen
}
