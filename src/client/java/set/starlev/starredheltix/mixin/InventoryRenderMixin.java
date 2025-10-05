package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.slotlocking.SlotLockManager;

@Mixin(HandledScreen.class)
public class InventoryRenderMixin {
    
    private static final Identifier LOCK_ICON = Identifier.of("starredheltix", "textures/gui/lock_icon.png");
    
    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V", shift = At.Shift.AFTER))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Check if slot locking is enabled
        if (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && 
            SlotLockManager.isSlotLocked(slot.id) && 
            client.player != null) {
            
            // Get the slot position
            int x = slot.x;
            int y = slot.y;
            
            // Draw a dark overlay with transparency on top of the item
            context.fill(x, y, x + 16, y + 16, 0x80000000); // 50% transparent black
            
            // Draw the lock icon texture on top of the item and overlay
            context.drawTexture(RenderLayer::getGuiTextured, LOCK_ICON, x, y, 0, 0, 16, 16, 16, 16);
        }
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderComplete(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        HandledScreen<?> handledScreen = (HandledScreen<?>) (Object) this;
        
        // Check if slot locking is enabled
        if (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && 
            client.player != null) {
            
            // Render lock icons on top of everything, including tooltips and other UI elements
            try {
                // Access slots field using reflection
                java.lang.reflect.Field slotsField = HandledScreen.class.getDeclaredField("slots");
                slotsField.setAccessible(true);
                Iterable<Slot> slots = (Iterable<Slot>) slotsField.get(handledScreen);
                
                // Draw lock icons for all locked slots on top of everything
                for (Slot slot : slots) {
                    if (SlotLockManager.isSlotLocked(slot.id)) {
                        // Get the slot position
                        int x = slot.x;
                        int y = slot.y;
                        
                        // Draw a dark overlay with transparency on top of everything
                        context.fill(x, y, x + 16, y + 16, 0x80000000); // 50% transparent black
                        
                        // Draw the lock icon texture on top of all other elements
                        context.drawTexture(RenderLayer::getGuiTextured, LOCK_ICON, x, y, 0, 0, 16, 16, 16, 16);
                    }
                }
            } catch (Exception e) {
                // Silently fail if reflection doesn't work
            }
        }
    }
}