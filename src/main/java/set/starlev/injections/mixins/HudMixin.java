package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;
import net.minecraft.world.scores.Objective;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import set.starlev.render.RenderEngine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import set.starlev.features.skyblock.SlotOverlayUtil;

@Mixin(Hud.class)
public class HudMixin {


    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getHideStatusEffects()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDisplayScoreboardSidebar(GuiGraphicsExtractor guiGraphics, Objective objective, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getSkyblock().getScoreboard().getEnabled()) {
            ci.cancel();
        }
    }


    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRenderHud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() == null && !mc.gui.hud.isHidden()) {
            RenderEngine.renderHud(graphics, delta);
            // events.onOverlay for JS modules: fireRender was previously fired only
            // from AbstractContainerScreenMixin (i.e. ONLY inside containers), so
            // crosshair/vignette/toasts never worked in the world. Now overlay works in HUD too.
            set.starlev.events.GuiEvents.fireRender(graphics);
        }
    }



    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void onRenderSlot(GuiGraphicsExtractor context, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) return;

        SlotOverlayUtil.INSTANCE.drawOverlay(context, x, y, stack);
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetOverlayMessage(Component component, boolean animateColor, CallbackInfo ci) {
        if (component == null) return;
        try {
            String text = component.getString();
            if (text == null || text.isEmpty()) return;
            // Событийный путь для Skill XP
            set.starlev.utils.detectors.SkillXpDetector.INSTANCE.handleActionBar(text);
            // Проверка скрытия скилл XP
            boolean hideSkill = false;
            try {
                hideSkill = set.starlev.config.ConfigManager.INSTANCE.getFeaturesSafe().getSkyblock().getSkills().getHideActionBar();
            } catch (Exception e) {
                try { hideSkill = StarredHeltix.Companion.getFeature().getSkyblock().getSkills().getHideActionBar(); } catch (Exception ex2) { hideSkill = false; }
            }
            if (hideSkill && set.starlev.utils.detectors.SkillXpDetector.INSTANCE.isPureSkillXpText(text)) {
                ci.cancel();
            }
        } catch (Exception e) {
            // игнор
        }
    }

}