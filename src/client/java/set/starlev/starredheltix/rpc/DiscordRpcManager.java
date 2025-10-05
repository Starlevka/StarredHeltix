package set.starlev.starredheltix.rpc;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import set.starlev.starredheltix.config.StarredHeltixConfig;

public class DiscordRpcManager {
    private boolean initialized = false;
    private String lastDetails = "Гринжу...";
    private String lastLargeImageKey = "starredheltix";
    private String lastLargeImageText = "Играю на Heltix Skyblock";
    private String lastSmallImageKey = "starpyps";
    private String lastSmallImageText = "starlev.heltix.net";

    public void init(StarredHeltixConfig config) {
        if (!config.discordRpc.rpcEnabled || initialized) {
            System.out.println("Discord RPC not enabled or already initialized");
            return;
        }
        
        try {
            System.out.println("Initializing Discord RPC with App ID: " + config.discordRpc.rpcApplicationId);
            DiscordRPC.discordInitialize(config.discordRpc.rpcApplicationId, null, true);
            initialized = true;
            System.out.println("Discord RPC initialized successfully");
            update(config);
        } catch (Throwable t) {
            System.err.println("Discord RPC init failed: " + t.getMessage());
            t.printStackTrace();
            initialized = false;
            config.discordRpc.rpcEnabled = false;
        }
    }

    public void update(StarredHeltixConfig config) {
        if (!initialized) {
            System.out.println("Discord RPC not initialized, skipping update");
            return;
        }
        
        try {
            System.out.println("Updating Discord RPC with details: " + config.discordRpc.rpcDetails);
            // Check if anything has actually changed to avoid unnecessary updates
            if (hasConfigChanged(config)) {
                updateLastConfig(config);
                
                DiscordRichPresence.Builder b = new DiscordRichPresence.Builder(config.discordRpc.rpcDetails);
                b.setBigImage(config.discordRpc.rpcLargeImageKey, config.discordRpc.rpcLargeImageText);
                if (config.discordRpc.rpcSmallImageKey != null && !config.discordRpc.rpcSmallImageKey.isEmpty()) {
                    b.setSmallImage(config.discordRpc.rpcSmallImageKey, config.discordRpc.rpcSmallImageText);
                }
                
                DiscordRPC.discordUpdatePresence(b.build());
                System.out.println("Discord RPC updated successfully");
            } else {
                System.out.println("Discord RPC config unchanged, skipping update");
            }
        } catch (Throwable t) {
            System.err.println("Discord RPC update failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private boolean hasConfigChanged(StarredHeltixConfig config) {
        return !lastDetails.equals(config.discordRpc.rpcDetails) ||
               !lastLargeImageKey.equals(config.discordRpc.rpcLargeImageKey) ||
               !lastLargeImageText.equals(config.discordRpc.rpcLargeImageText) ||
               !lastSmallImageKey.equals(config.discordRpc.rpcSmallImageKey) ||
               !lastSmallImageText.equals(config.discordRpc.rpcSmallImageText);
    }

    private void updateLastConfig(StarredHeltixConfig config) {
        lastDetails = config.discordRpc.rpcDetails;
        lastLargeImageKey = config.discordRpc.rpcLargeImageKey;
        lastLargeImageText = config.discordRpc.rpcLargeImageText;
        lastSmallImageKey = config.discordRpc.rpcSmallImageKey;
        lastSmallImageText = config.discordRpc.rpcSmallImageText;
    }

    public void tick() {
        if (!initialized) {
            return;
        }
        try {
            DiscordRPC.discordRunCallbacks();
        } catch (Throwable t) {
            System.err.println("Discord RPC callback failed: " + t.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void shutdown() {
        if (!initialized) return;
        try {
            DiscordRPC.discordClearPresence();
            DiscordRPC.discordShutdown();
            System.out.println("Discord RPC shutdown successfully");
        } catch (Throwable ignored) {
        } finally {
            initialized = false;
        }
    }
}