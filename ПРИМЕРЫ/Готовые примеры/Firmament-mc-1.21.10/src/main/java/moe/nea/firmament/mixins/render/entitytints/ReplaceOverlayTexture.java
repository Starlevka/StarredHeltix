package moe.nea.firmament.mixins.render.entitytints;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moe.nea.firmament.events.EntityRenderTintEvent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the overlay texture used by rendering with the override specified in {@link EntityRenderTintEvent#overlayOverride}
 */
@Mixin(RenderType.OverlayStateShard.class)
public class ReplaceOverlayTexture {
	@ModifyExpressionValue(
		method = {"method_23555", "method_23556"},
		expect = 2,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;overlayTexture()Lnet/minecraft/client/renderer/texture/OverlayTexture;"))
	private static OverlayTexture replaceOverlayTexture(OverlayTexture original) {
		if (EntityRenderTintEvent.overlayOverride != null)
			return EntityRenderTintEvent.overlayOverride;
		return original;
	}
}
