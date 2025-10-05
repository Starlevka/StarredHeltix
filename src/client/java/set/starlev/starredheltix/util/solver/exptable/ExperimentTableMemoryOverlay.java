package set.starlev.starredheltix.util.solver.exptable;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.Map;

public class ExperimentTableMemoryOverlay {
    private static final int SLOT_SIZE = 18;
    private static final int OVERLAY_X = 10;
    private static final int OVERLAY_Y = 10;
    
    public static void register() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            renderOverlay(context);
        });
    }
    
    private static void renderOverlay(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // 只在实验桌界面显示
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) {
            return;
        }
        
        String title = screen.getTitle().getString();
        
        // Check if this is explicitly an experiment table or if the feature is enabled for all containers
        boolean isExperimentTable = title.equals("Стол экспериментов");
        boolean isExperimentFeatureEnabled = StarredHeltixClient.CONFIG.general.enabled;
        
        // Only proceed if it's an experiment table or if the feature is enabled for all containers
        if (!isExperimentTable && !isExperimentFeatureEnabled) {
            return;
        }
        
        // 检查是否启用了实验桌记忆功能
        if (!StarredHeltixClient.CONFIG.general.enabled) {
            return;
        }
        
        // Enable depth testing to render on top of everything
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500); // Move to front layer
        
        // 显示是否已解决
        int yPos = OVERLAY_Y;
        if (ExperimentTableMemoryManager.isSolved()) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("§aSOLVED!"), OVERLAY_X, yPos, 0xFFFFFF);
            yPos += 12;
        }
        
        // 获取解题方案
        Map<Integer, ItemStack> solution = ExperimentTableMemoryManager.getSolution();
        if (solution.isEmpty()) {
            context.getMatrices().pop();
            return;
        }
        
        // 在屏幕左上角显示解题方案
        context.drawTextWithShadow(client.textRenderer, Text.literal("Experiment Table Solution:"), OVERLAY_X, yPos, 0xFFFFFF);
        yPos += 12;
        
        for (Map.Entry<Integer, ItemStack> entry : solution.entrySet()) {
            ItemStack itemStack = entry.getValue();
            String itemName = itemStack.getName().getString();
            
            // 显示物品名称和槽位索引
            context.drawTextWithShadow(client.textRenderer, 
                Text.literal("Slot " + entry.getKey() + ": " + itemName), 
                OVERLAY_X, yPos, 0xFFFFFF);
            yPos += 10;
        }
        
        // 显示记忆的物品
        Map<Integer, ItemStack> rememberedItems = ExperimentTableMemoryManager.getAllRememberedItems();
        if (!rememberedItems.isEmpty()) {
            yPos += 5; // Add some spacing
            context.drawTextWithShadow(client.textRenderer, Text.literal("Remembered Items:"), OVERLAY_X, yPos, 0xFFFFFF);
            yPos += 12;
            
            for (Map.Entry<Integer, ItemStack> entry : rememberedItems.entrySet()) {
                ItemStack itemStack = entry.getValue();
                String itemName = itemStack.getName().getString();
                
                // 显示物品名称和槽位索引
                context.drawTextWithShadow(client.textRenderer, 
                    Text.literal("Slot " + entry.getKey() + ": " + itemName), 
                    OVERLAY_X, yPos, 0xFFFFFF);
                yPos += 10;
            }
        }
        
        context.getMatrices().pop();
    }
}