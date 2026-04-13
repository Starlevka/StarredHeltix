package moe.nea.firmament.compat.sodium

import moe.nea.firmament.util.compatloader.CompatMeta
import moe.nea.firmament.util.compatloader.ICompatMeta
import net.fabricmc.loader.api.FabricLoader

@CompatMeta
object Compat : ICompatMeta {
	override fun shouldLoad(): Boolean {
		return FabricLoader.getInstance().isModLoaded("sodium")
	}
}
