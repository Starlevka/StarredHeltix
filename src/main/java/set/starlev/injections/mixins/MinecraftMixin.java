package set.starlev.injections.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        net.minecraft.client.gui.screens.Screen toOpen = StarredHeltix.Companion.getScreenToOpen();
        if (toOpen != null) {
            StarredHeltix.Companion.setScreenToOpen(null);
            ((Minecraft)(Object)this).setScreenAndShow(toOpen);
        }
        set.starlev.features.combat.dungeons.AutoReadyNotifier.INSTANCE.tick();
        set.starlev.features.skyblock.AutoSprint.INSTANCE.tick();
        set.starlev.features.chat.CustomBindManager.INSTANCE.tick();
        set.starlev.utils.detectors.StatsDetectors.INSTANCE.tick();
        set.starlev.features.skyblock.InventoryHistoryLog.INSTANCE.tick();
        set.starlev.features.combat.dungeons.solvers.ThreeWeirdos.INSTANCE.tick();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onStartUseItem(CallbackInfo ci) {
        set.starlev.features.skyblock.SmoothAote.INSTANCE.onItemUse();
    }
}
