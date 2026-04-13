package moe.nea.firmament.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformer
import moe.nea.firmament.init.MixinPlugin

class MixinTest {
	@Test
	fun hasInstalledMixinTransformer() {
		Assertions.assertInstanceOf(
			IMixinTransformer::class.java,
			MixinEnvironment.getCurrentEnvironment().activeTransformer
		)
	}
}

