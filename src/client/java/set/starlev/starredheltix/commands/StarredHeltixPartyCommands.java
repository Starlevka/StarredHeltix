package set.starlev.starredheltix.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class StarredHeltixPartyCommands {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("starredheltix");
            
            // Register party commands
            registerPartyCommands(builder);
            
            dispatcher.register(builder);
        });
    }
    
    private static void registerPartyCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // This method is now empty as we removed the /starredheltix яготовлёвал command
        // The standalone /яготовлёвал command is registered separately
    }
}