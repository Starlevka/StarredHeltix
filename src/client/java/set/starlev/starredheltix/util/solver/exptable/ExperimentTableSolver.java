package set.starlev.starredheltix.util.solver.exptable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

import java.util.*;

public class ExperimentTableSolver {
    // Predefined patterns for experiment table solving
    // Each pattern maps slot positions to item types
    private static final List<Map<Integer, Item>> PATTERNS = new ArrayList<>();
    
    // Initialize with empty patterns - remove the predefined patterns with diamonds, coal, etc.
    static {
        // Add empty patterns that will be filled dynamically
        for (int i = 0; i < 5; i++) {
            PATTERNS.add(new HashMap<>());
        }
    }
    
    // Current pattern index
    private static int currentPatternIndex = -1;
    
    // Random generator for pattern selection
    private static final Random random = Random.create();
    
    /**
     * Get the current pattern for the experiment table
     * @return The current pattern mapping slot positions to items
     */
    
    /**
     * Select a random pattern for the experiment table
     */
    public static void selectRandomPattern() {
        currentPatternIndex = random.nextInt(PATTERNS.size());
    }
    
    /**
     * Reset the pattern selection (e.g., when starting a new experiment table)
     */
    public static void resetPattern() {
        currentPatternIndex = -1;
    }
    
    /**
     * Check if the current pattern is solved
     * @param revealedItems Map of revealed items from the experiment table
     * @return True if all items in the pattern match the revealed items
     */
    public static boolean isPatternSolved(Map<Integer, ItemStack> revealedItems) {
        if (currentPatternIndex == -1) {
            return false;
        }
        
        Map<Integer, Item> pattern = PATTERNS.get(currentPatternIndex);
        
        // Check if all pattern items are revealed and match
        for (Map.Entry<Integer, Item> entry : pattern.entrySet()) {
            int slot = entry.getKey();
            Item expectedItem = entry.getValue();
            
            // Check if this slot has been revealed
            if (!revealedItems.containsKey(slot)) {
                return false;
            }
            
            // Check if the revealed item matches the expected item
            ItemStack revealedItem = revealedItems.get(slot);
            if (!revealedItem.isOf(expectedItem)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get the solution items for the current pattern
     * @return Map of slot positions to solution items
     */
    public static Map<Integer, ItemStack> getSolution() {
        if (currentPatternIndex == -1) {
            selectRandomPattern();
        }
        
        Map<Integer, Item> pattern = PATTERNS.get(currentPatternIndex);
        Map<Integer, ItemStack> solution = new HashMap<>();
        
        for (Map.Entry<Integer, Item> entry : pattern.entrySet()) {
            solution.put(entry.getKey(), new ItemStack(entry.getValue()));
        }
        
        return solution;
    }
    
    /**
     * Get the number of patterns available
     * @return Number of available patterns
     */
    public static int getPatternCount() {
        return PATTERNS.size();
    }
    
    /**
     * Get the current pattern index
     * @return Current pattern index, or -1 if no pattern is selected
     */
    public static int getCurrentPatternIndex() {
        return currentPatternIndex;
    }
}