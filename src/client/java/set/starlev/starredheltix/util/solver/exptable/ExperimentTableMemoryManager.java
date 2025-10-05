package set.starlev.starredheltix.util.solver.exptable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashMap;
import java.util.Map;

public class ExperimentTableMemoryManager {
    // 存储记忆的物品，键是位置（0-53，标准箱子界面大小），值是物品
    private static final Map<Integer, ItemStack> rememberedItems = new HashMap<>();
    
    // 存储已揭示的物品位置
    private static final Map<Integer, Boolean> revealedPositions = new HashMap<>();
    
    /**
     * 记忆在实验桌界面中点击的物品
     * @param slot 点击的槽位 (0-53)
     * @param itemStack 物品堆栈
     */
    public static void rememberItem(int slot, ItemStack itemStack) {
        if (slot >= 0 && slot < 54) { // 标准箱子界面有54个槽位
            // Don't remember items with the name "Нажмите!"
            if (itemStack.getName().getString().equals("Нажмите!")) {
                return;
            }
            
            // Check if the item is glass - if it is, we don't want to overwrite a previously remembered item
            if (isGlass(itemStack)) {
                // If it's glass and we already have a remembered item, keep the remembered item
                // Otherwise, remember the glass (this shouldn't happen in normal gameplay)
                if (!rememberedItems.containsKey(slot)) {
                    rememberedItems.put(slot, itemStack.copy());
                    revealedPositions.put(slot, false);
                }
            } else {
                // It's not glass, so remember this item
                rememberedItems.put(slot, itemStack.copy());
                revealedPositions.put(slot, true);
            }
        }
    }
    
    /**
     * 获取记忆的物品
     * @param slot 槽位
     * @return 记忆的物品，如果没有则返回null
     */
    public static ItemStack getRememberedItem(int slot) {
        return rememberedItems.get(slot);
    }
    
    /**
     * 检查槽位是否已被揭示
     * @param slot 槽位
     * @return 如果已揭示返回true，否则返回false
     */
    public static boolean isRevealed(int slot) {
        return revealedPositions.getOrDefault(slot, false);
    }
    
    /**
     * 清除所有记忆
     */
    public static void clearMemory() {
        rememberedItems.clear();
        revealedPositions.clear();
        // Also reset the experiment table pattern when clearing memory
        ExperimentTableSolver.resetPattern();
    }
    
    /**
     * Save memory to config
     */
    public static void saveMemory() {
        // This is a placeholder method to fix compilation error
        // In a real implementation, this would save the memory to config
    }
    
    /**
     * 获取所有记忆的物品
     * @return 记忆物品的映射
     */
    public static Map<Integer, ItemStack> getAllRememberedItems() {
        return new HashMap<>(rememberedItems);
    }
    
    /**
     * 获取记忆物品的数量
     * @return 记忆物品的数量
     */
    public static int getRememberedItemCount() {
        return rememberedItems.size();
    }
    
    /**
     * 检查槽位是否已被揭示
     * @param slotIndex 槽位索引
     * @return 如果已揭示返回true，否则返回false
     */
    public static boolean wasSlotRevealed(int slotIndex) {
        return revealedPositions.getOrDefault(slotIndex, false);
    }
    
    /**
     * 获取实验桌解题方案
     * @return 解题方案，包含槽位到物品的映射
     */
    public static Map<Integer, ItemStack> getSolution() {
        return ExperimentTableSolver.getSolution();
    }
    
    /**
     * 检查当前模式是否已解决
     * @return 如果解决返回true，否则返回false
     */
    public static boolean isSolved() {
        return ExperimentTableSolver.isPatternSolved(rememberedItems);
    }
    
    /**
     * Check if an item is a glass variant
     * @param stack The item stack to check
     * @return true if it's a glass item, false otherwise
     */
    private static boolean isGlass(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check for various glass types that might be used in experiment tables
        return stack.isOf(Items.GLASS_PANE) ||
               stack.isOf(Items.GRAY_STAINED_GLASS_PANE) ||
               stack.isOf(Items.WHITE_STAINED_GLASS_PANE) ||
               stack.isOf(Items.BLACK_STAINED_GLASS_PANE) ||
               stack.isOf(Items.BROWN_STAINED_GLASS_PANE) ||
               stack.isOf(Items.RED_STAINED_GLASS_PANE) ||
               stack.isOf(Items.ORANGE_STAINED_GLASS_PANE) ||
               stack.isOf(Items.YELLOW_STAINED_GLASS_PANE) ||
               stack.isOf(Items.LIME_STAINED_GLASS_PANE) ||
               stack.isOf(Items.GREEN_STAINED_GLASS_PANE) ||
               stack.isOf(Items.CYAN_STAINED_GLASS_PANE) ||
               stack.isOf(Items.LIGHT_BLUE_STAINED_GLASS_PANE) ||
               stack.isOf(Items.BLUE_STAINED_GLASS_PANE) ||
               stack.isOf(Items.PURPLE_STAINED_GLASS_PANE) ||
               stack.isOf(Items.MAGENTA_STAINED_GLASS_PANE) ||
               stack.isOf(Items.PINK_STAINED_GLASS_PANE) ||
               stack.isOf(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
    }
}