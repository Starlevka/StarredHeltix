package moe.nea.firmament.util.mc

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

fun FabricClientCommandSource.asFakeServer(): CommandSourceStack {
	val source = this
	return CommandSourceStack(
		object : CommandSource {
			override fun sendSystemMessage(message: Component?) {
				source.player.displayClientMessage(message, false)
			}

			override fun acceptsSuccess(): Boolean {
				return true
			}

			override fun acceptsFailure(): Boolean {
				return true
			}

			override fun shouldInformAdmins(): Boolean {
				return true
			}
		},
		source.position,
		source.rotation,
		null,
		0,
		"FakeServerCommandSource",
		Component.literal("FakeServerCommandSource"),
		null,
		source.player
	)
}
