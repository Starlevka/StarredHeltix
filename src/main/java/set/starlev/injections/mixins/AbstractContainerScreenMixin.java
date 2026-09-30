package set.starlev.injections.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.enchanting.ExperimentSolverManager;
import set.starlev.features.skyblock.SlotOverlayUtil;
import set.starlev.events.GuiEvents;
import set.starlev.render.RenderEngine;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    @Nullable
    private Slot hoveredSlot;

    private boolean starredheltix$isStarlevScreen() {
        return ((Object) this).getClass().getName().startsWith("set.starlev.");
    }

    /**
     * Ванильная белая рамка наведения перекрывает подсветку редкости слота
     * (SlotOverlayUtil) — на таких слотах её не рисуем (порт идеи heltix-mod,
     * где rarity-фон рисовался вместо стандартного хайлайта).
     */
    @Unique
    private boolean starredheltix$skipHoverHighlight() {
        try {
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig cfg =
                set.starlev.StarredHeltix.Companion.getFeature().getSkyblock().getSlotOverlay();
            if (!cfg.getHideHoverHighlight()) return false;
            Slot slot = this.hoveredSlot;
            if (slot == null || !slot.hasItem()) return false;
            return SlotOverlayUtil.INSTANCE.getRgbColor(slot.getItem()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private boolean starredheltix$hideOffhandHighlight() {
        try {
            if (!set.starlev.features.visual.OffhandSlotHighlight.INSTANCE.isHidden()) return false;
            Slot slot = this.hoveredSlot;
            if (slot == null) return false;
            // Offhand slot is Inventory.SLOT_OFFHAND (40) inside player Inventory container
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                if (slot.getContainerSlot() == net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private boolean starredheltix$shouldSkipHighlight() {
        return starredheltix$skipHoverHighlight() || starredheltix$hideOffhandHighlight();
    }

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void starredheltix$skipHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (starredheltix$shouldSkipHighlight()) ci.cancel();
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void starredheltix$skipHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (starredheltix$shouldSkipHighlight()) ci.cancel();
    }

    // ——— Experimentation solvers: замена рендера и тултипа ———
    @Unique
    private Slot starredheltix$currentSlot;

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void starredheltix$setCurrentSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        this.starredheltix$currentSlot = slot;
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void starredheltix$clearCurrentSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        this.starredheltix$currentSlot = null;
    }

    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void starredheltix$wrapItem(GuiGraphicsExtractor instance, ItemStack stack, int x, int y, int seed, Operation<Void> original) {
        try {
            Slot slot = this.starredheltix$currentSlot;
            if (slot != null) {
                ItemStack render = ExperimentSolverManager.INSTANCE.getRenderStack(slot, stack);
                original.call(instance, render, x, y, seed);
                return;
            }
        } catch (Exception e) {}
        original.call(instance, stack, x, y, seed);
    }

    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void starredheltix$wrapFakeItem(GuiGraphicsExtractor instance, ItemStack stack, int x, int y, int seed, Operation<Void> original) {
        try {
            Slot slot = this.starredheltix$currentSlot;
            if (slot != null) {
                ItemStack render = ExperimentSolverManager.INSTANCE.getRenderStack(slot, stack);
                original.call(instance, render, x, y, seed);
                return;
            }
        } catch (Exception e) {}
        original.call(instance, stack, x, y, seed);
    }

    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void starredheltix$wrapDecorations(GuiGraphicsExtractor instance, Font font, ItemStack stack, int x, int y, String text, Operation<Void> original) {
        try {
            Slot slot = this.starredheltix$currentSlot;
            if (slot != null) {
                ItemStack render = ExperimentSolverManager.INSTANCE.getRenderStack(slot, stack);
                original.call(instance, font, render, x, y, text);
                return;
            }
        } catch (Exception e) {}
        original.call(instance, font, stack, x, y, text);
    }

    @WrapOperation(method = "extractTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack starredheltix$wrapTooltipStack(Slot instance, Operation<ItemStack> original) {
        ItemStack stack = original.call(instance);
        try {
            return ExperimentSolverManager.INSTANCE.getTooltipStack(instance, stack);
        } catch (Exception e) {
            return stack;
        }
    }


    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        GuiEvents.fireOpen((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRenderHead(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!starredheltix$isStarlevScreen()) {
            GuiEvents.fireRender(graphics);
            RenderEngine.renderHud(graphics, net.minecraft.client.Minecraft.getInstance().getDeltaTracker());
        }
        if (starredheltix$isStarlevScreen()) {
            RenderEngine.renderHud(graphics, net.minecraft.client.Minecraft.getInstance().getDeltaTracker());
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRenderTail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!starredheltix$isStarlevScreen()) {
            GuiEvents.fireForeground(graphics, mouseX, mouseY, (AbstractContainerScreen<?>) (Object) this);
        }
        if (starredheltix$isStarlevScreen()) {
            GuiEvents.fireRender(graphics);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!starredheltix$isStarlevScreen()) {
            if (GuiEvents.fireClick(event.x(), event.y(), event.button(), (AbstractContainerScreen<?>) (Object) this)) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Хоткеи внутри контейнерных экранов (Leap-оверлей и подобные).
     * Обработанные GuiEvents.fireKey клавиши не доходят до ванильной логики
     * (например, цифры 1-4 иначе свапают слоты с хотбаром).
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void starredheltix$onKeyPressed(net.minecraft.client.input.KeyEvent event, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!starredheltix$isStarlevScreen()) {
            try {
                if (GuiEvents.fireKey(event, (AbstractContainerScreen<?>) (Object) this)) {
                    cir.setReturnValue(true);
                }
            } catch (Exception e) {}
        }
    }


    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        GuiEvents.fireClose((AbstractContainerScreen<?>) (Object) this);
    }



    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void onRenderSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        // Скрываем оригинальные слоты липа когда оверлей активен — показываем только крупные кнопки
        try {
            if (set.starlev.features.combat.dungeons.LeapOverlay.INSTANCE.isLeapScreen((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this)) {
                ci.cancel();
                return;
            }
        } catch (Exception e) {}
        ItemStack stack = slot.getItem();
        if (stack == null || stack.isEmpty()) return;

        SlotOverlayUtil.INSTANCE.drawOverlay(context, slot.x, slot.y, stack);
    }

    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void hideLeapMenuBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (set.starlev.features.combat.dungeons.LeapOverlay.INSTANCE.isLeapScreen((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this)) {
                ci.cancel();
            }
        } catch (Exception e) {}
    }

}