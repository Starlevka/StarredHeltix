package moe.nea.firmament.util.render

import moe.nea.firmament.annotations.Subscribe
import moe.nea.firmament.events.DebugInstantiateEvent

object FirmamentShaders {

	@Subscribe
	fun debugLoad(event: DebugInstantiateEvent) {
		// TODO: do i still need to work with shaders like this?
	}
}
