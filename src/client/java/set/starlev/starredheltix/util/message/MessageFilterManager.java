package set.starlev.starredheltix.util.message;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.Map;

public class MessageFilterManager {
    
    public static void register() {
        // Register the message filter
        ClientReceiveMessageEvents.ALLOW_GAME.register(MessageFilterManager::shouldAllowMessage);
    }
    
    /**
     * Checks if a message should be allowed to display based on the configured filters
     * @param message The message to check
     * @param overlay Whether the message is an overlay message
     * @return true if the message should be displayed, false if it should be hidden
     */
    public static boolean shouldAllowMessage(Text message, boolean overlay) {
        // If no filters are configured, allow all messages
        if (StarredHeltixClient.CONFIG.messageFilters.filters.isEmpty()) {
            return true;
        }
        
        String messageText = message.getString();
        
        // Check each filter
        for (String filterText : StarredHeltixClient.CONFIG.messageFilters.filters.values()) {
            // If the message starts with the filter text, hide it
            if (messageText.startsWith(filterText)) {
                return false;
            }
        }
        
        // If no filters matched, allow the message
        return true;
    }
    
    /**
     * Adds a new message filter
     * @param id The ID of the filter
     * @param prefix The prefix that messages must start with to be filtered
     */
    public static void addFilter(int id, String prefix) {
        StarredHeltixClient.CONFIG.messageFilters.filters.put(id, prefix);
        StarredHeltixClient.CONFIG.save();
    }
    
    /**
     * Removes a message filter
     * @param id The ID of the filter to remove
     */
    public static void removeFilter(int id) {
        StarredHeltixClient.CONFIG.messageFilters.filters.remove(id);
        StarredHeltixClient.CONFIG.save();
    }
    
    /**
     * Gets all currently configured filters
     * @return A map of filter IDs to filter prefixes
     */
    public static Map<Integer, String> getFilters() {
        return StarredHeltixClient.CONFIG.messageFilters.filters;
    }
}