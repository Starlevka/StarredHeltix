package moe.nea.firmament.mixins.render.entitytints;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moe.nea.firmament.events.EntityRenderTintEvent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Patch to make {@link ItemStackRenderState} use a {@link RenderType} that allows uses Minecraft's overlay texture.
 *
 * @see UseOverlayableHeadFeatureRenderer
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public class UseOverlayableItemRenderer {
	@ModifyExpressionValue(method = "submit", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;renderType:Lnet/minecraft/client/renderer/RenderType;", opcode = Opcodes.GETFIELD))
	private RenderType replace(RenderType original) {
		if (EntityRenderTintEvent.overlayOverride != null && original instanceof RenderType.CompositeRenderType multiPhase && multiPhase.state.textureState instanceof RenderStateShard.TextureStateShard texture && texture.cutoutTexture().isPresent())
			return RenderType.entityTranslucent(texture.cutoutTexture().get());
		return original;
	}
}
