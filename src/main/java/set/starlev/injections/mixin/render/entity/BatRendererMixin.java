package set.starlev.injections.mixin.render.entity;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.DungeonDetector;

@Mixin(BatRenderer.class)
public class BatRendererMixin {
    private static final ResourceLocation GREEN_BAT_LOCATION = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/entity/green_bat.png");

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(BatRenderState state, CallbackInfoReturnable<ResourceLocation> cir) {
        if (StarredHeltix.Companion.getFeature().getDungeons().getVisuals().getGreenBats() && DungeonDetector.INSTANCE.isInDungeon()) {
            cir.setReturnValue(GREEN_BAT_LOCATION);
        }
    }
}
