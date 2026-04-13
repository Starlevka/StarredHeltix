package set.starlev.injections.mixin.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;

import net.minecraft.network.chat.Component;
import set.starlev.secret.features.SecretFunFeatures;
import set.starlev.secret.config.SecretMenuManager;

@Mixin(Gui.class)
public class GuiMixin {
    private boolean didPushScoreboardMatrix = false;

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getHideStatusEffects()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDisplayScoreboardSidebar(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getSkyblock().getScoreboard().getEnabled()) {
            ci.cancel();
        }
    }

    @ModifyArgs(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")
    )
    private void onDrawScoreboardBackground(Args args) {
        if (!StarredHeltix.Companion.getFeature().getSkyblock().getScoreboard().getEnabled()) {
            return;
        }
        // Всегда скрываем стандартную тень майнкрафта
        args.set(4, 0);
    }

    @ModifyArgs(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V")
    )
    private void onDrawScoreboardString(Args args, GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective) {
        // Разрешаем выполнение для ванильного скорборда, чтобы работали эффекты
        Component component = args.get(1);
        
        // Process text effects
        if (SecretMenuManager.INSTANCE.isConfigInitialized()) {
            boolean starlevEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect();
            boolean megaChromeEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getMegaChromeXEffect();
            
            if (component != null && (starlevEnabled || megaChromeEnabled)) {
                // Применяем эффекты принудительно для scoreboard (чтобы работало всегда, как просил пользователь)
                component = SecretFunFeatures.processComponent(component, true);
                args.set(1, component);
            }
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onDisplayScoreboardSidebarHead(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        didPushScoreboardMatrix = false;
        if (StarredHeltix.Companion.getFeature().getSkyblock().getScoreboard().getEnabled()) {
            ci.cancel();
            return;
        }

        // Кастомный скорборд выключен: используем ванильный скорборд без дополнительных инъекций
        return;
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void onDisplayScoreboardSidebarReturn(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        if (didPushScoreboardMatrix) {
            guiGraphics.pose().popMatrix();
            didPushScoreboardMatrix = false;
        }
    }
}
