package set.starlev.mixin;

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

import set.starlev.hud.HudScoreboard;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import set.starlev.features.combat.slayer.SlayerScoreboard;
import set.starlev.secret.features.SecretFunFeatures;
import set.starlev.secret.config.SecretMenuManager;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.List;

@Mixin(Gui.class)
public class GuiMixin {
    private int slayerYOffset = 0;
    private List<Component> slayerExtraLines;
    private float lastScoreboardX = 0;
    private float lastScoreboardY = 0;
    private float lastScoreboardScale = 1.0f;
    private int currentScoreboardWidth = 0;
    private int totalExtraHeight = 0;

    private boolean didPushScoreboardMatrix = false;

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getHideStatusEffects()) {
            ci.cancel();
        }
    }

    @ModifyArgs(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")
    )
    private void onDrawScoreboardBackground(Args args) {
        // Всегда скрываем стандартную тень майнкрафта
        args.set(4, 0);
    }

    @ModifyArgs(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V")
    )
    private void onDrawScoreboardString(Args args, GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective) {
        Font font = args.get(0);
        Component component = args.get(1);
        int x = args.get(2);
        int y = args.get(3);
        
        // 1. Process text effects
        if (SecretMenuManager.INSTANCE.isConfigInitialized()) {
            boolean starlevEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect();
            boolean megaChromeEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getMegaChromeXEffect();
            
            if (component != null && (starlevEnabled || megaChromeEnabled)) {
                // Применяем эффекты только если это форсировано (для скорборда обычно НЕ форсируем глобально)
                component = SecretFunFeatures.processComponent(component, false);
                args.set(1, component);
            }
        }

        // 2. Handle Slayer HUD injection
        int currentY = y + slayerYOffset;
        args.set(3, currentY);

        if (StarredHeltix.Companion.getFeature().getSlayer().getSlayerHud().getSlayerScoreboardHud() &&
            component != null) {
            
            String text = component.getString();
            // Поддержка русского и английского
            if (text.contains("/") && (text.contains("опыта") || text.contains("XP") || text.contains("опыта Боя"))) {
                if (slayerExtraLines != null && !slayerExtraLines.isEmpty()) {
                    int nextY = currentY + 9;
                    
                    for (Component extraLine : slayerExtraLines) {
                        // Используем 0xFFFFFFFF (полностью непрозрачный белый) и тень текста всегда ВКЛЮЧЕНА
                        guiGraphics.drawString(font, extraLine, x, nextY, 0xFFFFFFFF, true);
                        nextY += 9;
                    }
                    slayerYOffset += slayerExtraLines.size() * 9;
                }
            }
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onDisplayScoreboardSidebarHead(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        didPushScoreboardMatrix = false;
        if (set.starlev.StarredHeltix.Companion.getFeature().getVisuals().getScoreboard().getEnabled()) {
            ci.cancel();
            return;
        }
        
        // Если кастомный скорборд выключен, сбрасываем смещения для стандартного
        slayerYOffset = 0;
        slayerExtraLines = null;
        currentScoreboardWidth = 0;
        totalExtraHeight = 0;

        // Вычисляем текущую ширину скорборда
        Font font = Minecraft.getInstance().font;
        currentScoreboardWidth = font.width(objective.getDisplayName());
        net.minecraft.world.scores.Scoreboard scoreboard = objective.getScoreboard();
        
        // В 1.21.10 используем listPlayerScores или аналогичный метод
        // Для простоты и надежности возьмем логику из ScoreboardDetector
        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry score : scores) {
            String owner = score.owner();
            net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayersTeam(owner);
            Component lineComponent = score.display();
            if (lineComponent == null) {
                if (team != null) {
                    lineComponent = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(team, Component.literal(owner));
                } else {
                    lineComponent = Component.literal(owner);
                }
            }
            currentScoreboardWidth = Math.max(currentScoreboardWidth, font.width(lineComponent));
        }

        // Учитываем ширину наших доп. строк сразу при расчете ширины скорборда
        if (StarredHeltix.Companion.getFeature().getSlayer().getSlayerHud().getSlayerScoreboardHud()) {
            slayerExtraLines = SlayerScoreboard.INSTANCE.getExtraLines();
            if (slayerExtraLines != null && !slayerExtraLines.isEmpty()) {
                totalExtraHeight = slayerExtraLines.size() * 9;
                for (Component extraLine : slayerExtraLines) {
                    currentScoreboardWidth = Math.max(currentScoreboardWidth, font.width(extraLine));
                }
            }
        }
        
        if (SecretMenuManager.INSTANCE.isConfigInitialized()) {
            if (StarredHeltix.Companion.getFeature().getSlayer().getSlayerHud().getSlayerScoreboardHud()) {
                slayerExtraLines = SlayerScoreboard.INSTANCE.getExtraLines();
            }
            
            float scale = HudScoreboard.INSTANCE.getScale();
            // Вычисляем смещение относительно дефолтной позиции
            float offsetX = HudScoreboard.INSTANCE.getAdjustedX() - HudScoreboard.INSTANCE.getDefaultX();
            float offsetY = HudScoreboard.INSTANCE.getAdjustedY() - HudScoreboard.INSTANCE.getDefaultY();
            
            lastScoreboardX = offsetX;
            lastScoreboardY = offsetY;
            lastScoreboardScale = scale;
            
            if (offsetX != 0 || offsetY != 0 || scale != 1.0f) {
                guiGraphics.pose().pushMatrix();
                didPushScoreboardMatrix = true;
                
                if (scale != 1.0f) {
                    // Масштабируем относительно верхнего левого угла элемента
                    float pivotX = HudScoreboard.INSTANCE.getAdjustedX();
                    float pivotY = HudScoreboard.INSTANCE.getAdjustedY();
                    
                    guiGraphics.pose().translate(pivotX, pivotY);
                    guiGraphics.pose().scale(scale, scale);
                    guiGraphics.pose().translate(-pivotX, -pivotY);
                }
                
                if (offsetX != 0 || offsetY != 0) {
                    guiGraphics.pose().translate(offsetX, offsetY);
                }

                // Отрисовка единой кастомной тени скорборда
                if (true) { // Кастомная тень фона скорборда (можно добавить отдельную настройку если нужно)
                    int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    
                    int linesCount = scores.size();
                    int totalHeight = (linesCount + 1) * 9; // +1 для заголовка
                    int startY = guiHeight / 2 + totalHeight / 3;
                    
                    // Согласуем с расчетами в HudScoreboard.calculateSize
                    int padding = 4;
                    
                    int x1 = guiWidth - 3 - currentScoreboardWidth - padding;
                    int y1 = startY - totalHeight - padding;
                    int x2 = guiWidth - 3 + padding;
                    int y2 = startY + totalExtraHeight + padding;
                    
                    guiGraphics.fill(x1, y1, x2, y2, 0x70000000); // Используем 0x70000000 для консистентности
                }
            }
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void onDisplayScoreboardSidebarReturn(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        if (didPushScoreboardMatrix) {
            guiGraphics.pose().popMatrix();
            didPushScoreboardMatrix = false;
        }
    }
}
