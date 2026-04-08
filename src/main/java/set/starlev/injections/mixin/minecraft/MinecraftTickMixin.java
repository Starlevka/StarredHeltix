package set.starlev.injections.mixin.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;

@Mixin(Minecraft.class)
public class MinecraftTickMixin {
    @Shadow
    public Screen screen;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {   
        Screen toOpen = StarredHeltix.Companion.getScreenToOpen();
        if (toOpen != null) {
            StarredHeltix.Companion.setScreenToOpen(null);
            ((Minecraft)(Object)this).setScreen(toOpen);
        }
        set.starlev.features.combat.dungeons.AutoReadyNotifier.INSTANCE.tick();
        set.starlev.features.misc.AutoSprint.INSTANCE.tick();
        set.starlev.features.chat.CustomBindManager.INSTANCE.tick();
        set.starlev.features.misc.info.StatsTracker.INSTANCE.tick();
        set.starlev.features.misc.InventoryHistoryLog.INSTANCE.tick();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onStartUseItem(CallbackInfo ci) {
        set.starlev.features.skyblock.SmoothAote.INSTANCE.onItemUse();
    }
}
