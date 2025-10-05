package set.starlev.starredheltix.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.equipment.EquipmentManager;
import set.starlev.starredheltix.util.message.MessageFilterManager;
import set.starlev.starredheltix.util.solver.exptable.ExperimentTableMemoryManager;
import set.starlev.starredheltix.util.slotlocking.SlotLockManager;
import set.starlev.starredheltix.util.updater.ModUpdater;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StarredHeltixCommands {

    public static void registerCommands() {
        // Register the main command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("starredheltix");
            
            // Help command
            builder.then(ClientCommandManager.literal("help")
            .executes(context -> {
                    sendHelpMessage(context.getSource());
                    return 1;
                }));
            
            // Add update command
            builder.then(ClientCommandManager.literal("update")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("§eChecking for updates..."));
                    ModUpdater.checkForUpdates();
                    return 1;
                })
                .then(ClientCommandManager.literal("open")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§eOpening releases page..."));
                        ModUpdater.openReleasesPage();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("install")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§eDownloading and installing update..."));
                        ModUpdater.downloadUpdate();
                        return 1;
                    })));
            
            // QOL commands
            registerQolCommands(builder);
            
            // Utility commands
            registerUtilityCommands(builder);
            
            // Debug commands
            registerDebugCommands(builder);
            
            // Party commands
            registerPartyCommands(builder);
            
            // Configuration commands
            registerConfigCommands(builder);
            
            // Experiment table memory commands
            registerExperimentTableCommands(builder);
            
            // Blood room commands
            registerBloodRoomCommands(builder);

            // Feature-specific commands with help and toggle functionality
            registerFeatureCommands(builder);

            dispatcher.register(builder);
        });
        
        // Register standalone commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Login command - /вход
            LiteralArgumentBuilder<FabricClientCommandSource> loginBuilder = ClientCommandManager.literal("вход");
            loginBuilder.executes(context -> {
                if (!StarredHeltixClient.CONFIG.loginPassword.isEmpty()) {
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("login " + StarredHeltixClient.CONFIG.loginPassword);
                    context.getSource().sendFeedback(Text.literal("§aLogged in successfully"));
                } else {
                    context.getSource().sendError(Text.literal("§cLogin password not set. Use /starredheltix config password <password> to set it."));
                }
                return 1;
            });
            dispatcher.register(loginBuilder);
        });
        
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Ready phrase command - /яготовлёвал
            LiteralArgumentBuilder<FabricClientCommandSource> readyBuilder = ClientCommandManager.literal("яготовлёвал");
            readyBuilder.executes(context -> {
                if (StarredHeltixClient.CONFIG.general.chattingEnabled) {
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("pc " + StarredHeltixClient.CONFIG.partyCommands.customReadyPhrase);
                    context.getSource().sendFeedback(Text.literal("§aSent ready message"));
                } else {
                    context.getSource().sendError(Text.literal("§cChatting is disabled in the config"));
                }
                return 1;
            });
            dispatcher.register(readyBuilder);
        });
        
        System.out.println("StarredHeltix: Registered commands");
    }
    
    private static void registerQolCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder){
// Toggle command - enables/disables the mod
        builder.then(ClientCommandManager.literal("toggle")
            .executes(context -> {
                StarredHeltixClient.CONFIG.general.enabled = !StarredHeltixClient.CONFIG.general.enabled;
                StarredHeltixClient.CONFIG.save();
                if (StarredHeltixClient.CONFIG.general.enabled) {
                    context.getSource().sendFeedback(Text.literal("§aStarredHeltix enabled"));
                } else {
                    context.getSource().sendFeedback(Text.literal("§cStarredHeltix disabled"));
                }
                return 1;
            }));

        // Ready command - sendsareadymessage to chat
        builder.then(ClientCommandManager.literal("ready")
            .executes(context -> {
                if (StarredHeltixClient.CONFIG.general.chattingEnabled) {
                    MinecraftClient.getInstance().player.networkHandler.sendChatMessage(StarredHeltixClient.CONFIG.partyCommands.customReadyPhrase);
                   context.getSource().sendFeedback(Text.literal("§aSent ready message"));
                } else {
                    context.getSource().sendError(Text.literal("§cChatting is disabled in the config"));
                }
                return 1;
            }));

        // Filter commands - manage message filters
        LiteralArgumentBuilder<FabricClientCommandSource>filterBuilder= ClientCommandManager.literal("filter");
        filterBuilder.then(ClientCommandManager.literal("add")
            .then(ClientCommandManager.argument("prefix", StringArgumentType.greedyString())
                .executes(context -> {
                    String prefix = StringArgumentType.getString(context, "prefix");
                    // Generate a new IDforthefilter
                    AtomicInteger newId = new AtomicInteger(0);
                    MessageFilterManager.getFilters().keySet().forEach(id -> {
                        if (id >= newId.get()) {
                            newId.set(id + 1);
                        }
                    });
                    int id = newId.get();
                    MessageFilterManager.addFilter(id,prefix);
                    context.getSource().sendFeedback(Text.literal("§aAdded filter with ID " + id + " for prefix: " + prefix));
                    return 1;
                }))
            .executes(context -> {
                context.getSource().sendError(Text.literal("§cUsage: /starredheltix filteradd<prefix>"));
                return 1;
            }));
        filterBuilder.then(ClientCommandManager.literal("remove")
            .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(0))
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    if (MessageFilterManager.getFilters().containsKey(id)) {
                        MessageFilterManager.removeFilter(id);
                        context.getSource().sendFeedback(Text.literal("§aRemoved filter with ID " + id));
                    } else {
                        context.getSource().sendError(Text.literal("§cNo filter found with ID " + id));
                    }
                    return 1;
})))
            .executes(context -> {
                context.getSource().sendError(Text.literal("§cUsage: /starredheltix filter remove <id>"));
                return 1;
            });
        filterBuilder.then(ClientCommandManager.literal("list")
            .executes(context -> {
                Map<Integer,String>filters = MessageFilterManager.getFilters();
                if (filters.isEmpty()) {
                    context.getSource().sendFeedback(Text.literal("§eNo filters configured"));
                } else {
                    context.getSource().sendFeedback(Text.literal("§a=== Message Filters ==="));
                    filters.forEach((id, prefix) -> context.getSource().sendFeedback(Text.literal("§e" + id + ": §f" + prefix)));
                }
                return 1;
            }));
        filterBuilder.then(ClientCommandManager.literal("clear")
            .executes(context -> {
                int count = MessageFilterManager.getFilters().size();
                MessageFilterManager.getFilters().clear();
StarredHeltixClient.CONFIG.save();
                context.getSource().sendFeedback(Text.literal("§aCleared " + count + " filters"));
                return 1;
            }));
        builder.then(ClientCommandManager.literal("filter").then(filterBuilder));
    }

    private static void registerUtilityCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Reload config command
        builder.then(ClientCommandManager.literal("reloadconfig")
            .executes(context -> {
                StarredHeltixClient.reloadConfig();
                context.getSource().sendFeedback(Text.literal("§aConfiguration reloaded"));
                return 1;
            }));
    }

    private static void registerDebugCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Debug command
        builder.then(ClientCommandManager.literal("debug")
            .executes(context -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    // Get nearby entities
                    int entityCount = 0;
                    int endermanCount = 0;
                    int wolfCount = 0;
                    if (client.world != null) {
                        for (Entity entity : client.world.getEntities()) {
                            entityCount++;
                            if (entity instanceof EndermanEntity) {
                                endermanCount++;
                            } else if (entity instanceof WolfEntity) {
                                wolfCount++;
                            }
                        }
                    }
                    
                    context.getSource().sendFeedback(Text.literal("§a=== StarredHeltix Debug Info ==="));
                    context.getSource().sendFeedback(Text.literal("§ePlayer: §f" + client.player.getName().getString()));
                    context.getSource().sendFeedback(Text.literal("§eWorld: §f" + (client.world != null ? client.world.getRegistryKey().getValue() : "null")));
                    context.getSource().sendFeedback(Text.literal("§eEntities: §f" + entityCount + " (Endermen: " + endermanCount + ", Wolves: " + wolfCount + ")"));
                    context.getSource().sendFeedback(Text.literal("§eFPS: §f" + client.getCurrentFps()));
                    context.getSource().sendFeedback(Text.literal("§eMod users: §f" + StarredHeltixClient.getModUserCount()));
                }
                return 1;
            })
            .then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.general.debugMode = !StarredHeltixClient.CONFIG.general.debugMode;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aDebug mode " + (StarredHeltixClient.CONFIG.general.debugMode ? "enabled" : "disabled")));
                    return 1;
                })));
    }

    private static void registerPartyCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Party commands toggle
        builder.then(ClientCommandManager.literal("partycommands")
            .executes(context -> {
                StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled;
                StarredHeltixClient.CONFIG.save();
                context.getSource().sendFeedback(Text.literal("§aParty commands " + (StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ? "enabled" : "disabled")));
                return 1;
            })
            .then(ClientCommandManager.literal("privatemessage")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.partyCommands.partyPrivateMessageCommandsEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyPrivateMessageCommandsEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aPrivate message commands " + (StarredHeltixClient.CONFIG.partyCommands.partyPrivateMessageCommandsEnabled ? "enabled" : "disabled")));
                    return 1;
                })));
    }

    private static void registerConfigCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Configuration commands
        LiteralArgumentBuilder<FabricClientCommandSource> configBuilder = ClientCommandManager.literal("config");
        
        configBuilder.then(ClientCommandManager.literal("password")
            .then(ClientCommandManager.argument("password", StringArgumentType.word())
                .executes(context -> {
                    String password = StringArgumentType.getString(context, "password");
                    StarredHeltixClient.CONFIG.loginPassword = password;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aLogin password set"));
                    return 1;
                })));
        
        configBuilder.then(ClientCommandManager.literal("readyphrase")
            .then(ClientCommandManager.argument("phrase", StringArgumentType.greedyString())
                .executes(context -> {
                    String phrase = StringArgumentType.getString(context, "phrase");
                    StarredHeltixClient.CONFIG.partyCommands.customReadyPhrase = phrase;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aReady phrase set"));
                    return 1;
                })));
        
        builder.then(ClientCommandManager.literal("config").then(configBuilder));
    }

    private static void registerExperimentTableCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Experiment table memory commands
        LiteralArgumentBuilder<FabricClientCommandSource> experimentBuilder = ClientCommandManager.literal("experiment");
        
        experimentBuilder.then(ClientCommandManager.literal("clear")
            .executes(context -> {
                ExperimentTableMemoryManager.clearMemory();
                context.getSource().sendFeedback(Text.literal("§aExperiment table memory cleared"));
                return 1;
            }));
        
        experimentBuilder.then(ClientCommandManager.literal("save")
            .executes(context -> {
                ExperimentTableMemoryManager.saveMemory();
                context.getSource().sendFeedback(Text.literal("§aExperiment table memory saved"));
                return 1;
            }));
        
        builder.then(ClientCommandManager.literal("experiment").then(experimentBuilder));
    }

    private static void registerBloodRoomCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Blood room timer command
        builder.then(ClientCommandManager.literal("bloodroom")
            .executes(context -> {
                StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled = !StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled;
                StarredHeltixClient.CONFIG.save();
                context.getSource().sendFeedback(Text.literal("§aBlood room timer " + (StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ? "enabled" : "disabled")));
                return 1;
            }));
    }

    private static void registerFeatureCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        registerFeatureCommand(builder, "inventorywarning", "Toggles inventory full warning notifications", "starredheltix.command.help.inventorywarning");
        registerFeatureCommand(builder, "slotlocking", "Toggles slot locking functionality", "starredheltix.command.help.slotlocking.toggle");
        registerFeatureCommand(builder, "bloodroom", "Toggles blood room timer", "starredheltix.command.help.bloodroom.toggle");
        registerFeatureCommand(builder, "endermenhighlight", "Toggles endermen entity highlighting", "starredheltix.command.help.endermenhighlight");
        registerFeatureCommand(builder, "wolfhighlight", "Toggles wolf entity highlighting", "starredheltix.command.help.wolfhighlight");
        registerFeatureCommand(builder, "fairysouls", "Toggles fairy souls waypoints", "starredheltix.command.help.fairysouls");
        registerFeatureCommand(builder, "discordrpc", "Toggles Discord Rich Presence", "starredheltix.command.help.discordrpc");
        registerFeatureCommand(builder, "woodwormcooldown", "Toggles Woodworm axe cooldown visualization", "starredheltix.command.help.woodwormcooldown");
        registerFeatureCommand(builder, "hidearmor", "Toggles armor hiding", "starredheltix.command.help.hidearmor");
        registerFeatureCommand(builder, "experiment", "Toggles experiment table solver", "starredheltix.command.help.experiment.toggle");
        registerFeatureCommand(builder, "equipment", "Toggles equipment display", "starredheltix.command.help.equipment.toggle");
        registerFeatureCommand(builder, "partycommands", "Configures party commands", "starredheltix.command.help.partycommands");
        registerFeatureCommand(builder, "characterhighlight", "Toggles character highlighter", "starredheltix.command.help.characterhighlight");
        registerFeatureCommand(builder, "autosprint", "Toggles auto-sprint feature", "starredheltix.command.help.autosprint");
    }

    private static void registerFeatureCommand(LiteralArgumentBuilder<FabricClientCommandSource> builder, String featureName, String description, String toggleCommandKey) {
        LiteralArgumentBuilder<FabricClientCommandSource> featureBuilder = ClientCommandManager.literal(featureName);
        
        // Add subcommands for woodwormcooldown
        if ("woodwormcooldown".equals(featureName)) {
            // Toggle subcommand
            featureBuilder.then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.woodwormCooldown.enabled = !StarredHeltixClient.CONFIG.woodwormCooldown.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aWoodworm axe cooldown visualization " + 
                        (StarredHeltixClient.CONFIG.woodwormCooldown.enabled ? "enabled" : "disabled")));
                    return 1;
                }));
                
            // Percentage subcommand
            featureBuilder.then(ClientCommandManager.literal("percentage")
                .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                    .executes(context -> {
                        int percentage = IntegerArgumentType.getInteger(context, "value");
                        StarredHeltixClient.CONFIG.woodwormCooldown.cooldownPercentage = percentage;
                        StarredHeltixClient.CONFIG.save();
                        context.getSource().sendFeedback(Text.literal("§aWoodworm axe cooldown percentage set to " + percentage + "%"));
                        return 1;
                    })));
        }
        
        // Toggle functionality
        featureBuilder.executes(context -> {
            switch (featureName) {
                case "inventorywarning":
                    StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled = !StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aInventory warning " + 
                        (StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "slotlocking":
                    StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled = !StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aSlot locking " + 
                        (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "bloodroom":
                    StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled = !StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aBlood room timer " + 
                        (StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "endermenhighlight":
                    StarredHeltixClient.CONFIG.endermenHighlight.endermenHighlightEnabled = !StarredHeltixClient.CONFIG.endermenHighlight.endermenHighlightEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aEndermen highlight " + 
                        (StarredHeltixClient.CONFIG.endermenHighlight.endermenHighlightEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "wolfhighlight":
                    StarredHeltixClient.CONFIG.wolfHighlight.wolfHighlightEnabled = !StarredHeltixClient.CONFIG.wolfHighlight.wolfHighlightEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aWolf highlight " + 
                        (StarredHeltixClient.CONFIG.wolfHighlight.wolfHighlightEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "fairysouls":
                    StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled = !StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aFairy souls " + 
                        (StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "discordrpc":
                    StarredHeltixClient.CONFIG.discordRpc.rpcEnabled = !StarredHeltixClient.CONFIG.discordRpc.rpcEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aDiscord RPC " + 
                        (StarredHeltixClient.CONFIG.discordRpc.rpcEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "woodwormcooldown":
                    StarredHeltixClient.CONFIG.woodwormCooldown.enabled = !StarredHeltixClient.CONFIG.woodwormCooldown.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aWoodworm axe cooldown visualization " + 
                        (StarredHeltixClient.CONFIG.woodwormCooldown.enabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "hidearmor":
                    StarredHeltixClient.CONFIG.armorHiding.enabled = !StarredHeltixClient.CONFIG.armorHiding.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aArmor hiding " + 
                        (StarredHeltixClient.CONFIG.armorHiding.enabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "experiment":
                    StarredHeltixClient.CONFIG.general.enabled = !StarredHeltixClient.CONFIG.general.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aExperiment table solver " + 
                        (StarredHeltixClient.CONFIG.general.enabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "equipment":
                    StarredHeltixClient.CONFIG.equipment.enabled = !StarredHeltixClient.CONFIG.equipment.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aEquipment display " + 
                        (StarredHeltixClient.CONFIG.equipment.enabled ? "enabled" : "disabled")));
                    return 1;
                    
case "partycommands":
                    StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled = 
                        !StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aParty commands " + 
                        (StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "characterhighlight":
                    StarredHeltixClient.CONFIG.characterHighlight.enabled = !StarredHeltixClient.CONFIG.characterHighlight.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aCharacter highlighter " + 
                        (StarredHeltixClient.CONFIG.characterHighlight.enabled ? "enabled" : "disabled")));
                    return 1;
                    
                case "autosprint":
                    StarredHeltixClient.CONFIG.autoSprint.enabled = !StarredHeltixClient.CONFIG.autoSprint.enabled;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aAuto-sprint " + 
                        (StarredHeltixClient.CONFIG.autoSprint.enabled ? "enabled" : "disabled")));
                    return 1;
                    
                default:
                    context.getSource().sendError(Text.literal("§cUnknown feature: " + featureName));
                    return 0;
            }
        });
        
        builder.then(ClientCommandManager.literal(featureName).then(featureBuilder));
    }
    
    private static String getFeatureStatus(String featureName) {
        switch (featureName) {
            case "inventorywarning":
                return StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "slotlocking":
                return StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "bloodroom":
                return StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "endermenhighlight":
                return StarredHeltixClient.CONFIG.endermenHighlight.endermenHighlightEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "wolfhighlight":
                return StarredHeltixClient.CONFIG.wolfHighlight.wolfHighlightEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "fairysouls":
                return StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "discordrpc":
                return StarredHeltixClient.CONFIG.discordRpc.rpcEnabled? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "woodwormcooldown":
                return StarredHeltixClient.CONFIG.woodwormCooldown.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "hidearmor":
                return StarredHeltixClient.CONFIG.armorHiding.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "experiment":
                return StarredHeltixClient.CONFIG.general.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "equipment":
                return StarredHeltixClient.CONFIG.equipment.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "partycommands":
                return StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "characterhighlight":
                return StarredHeltixClient.CONFIG.characterHighlight.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            case "autosprint":
                return StarredHeltixClient.CONFIG.autoSprint.enabled ? 
                    Text.translatable("starredheltix.command.feature.enabled").getString() : 
                    Text.translatable("starredheltix.command.feature.disabled").getString();
                    
            default:
                return null;
        }
    }
private static void sendHelpMessage(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("starredheltix.command.help.title"));
        source.sendFeedback(Text.literal("§e/starredheltix help §7- ").append(Text.translatable("starredheltix.command.help.toggle")));
        source.sendFeedback(Text.literal("§e/starredheltix update §7- Check for mod updates"));
        source.sendFeedback(Text.literal("§e/starredheltix update open §7- Open the releases page"));
        source.sendFeedback(Text.literal("§e/starredheltix update install §7- Download and install the latest update"));
        source.sendFeedback(Text.literal("§e/starredheltix ready §7- ").append(Text.translatable("starredheltix.command.help.ready")));
        
        source.sendFeedback(Text.literal("§e/starredheltix filter §7- ").append(Text.translatable("starredheltix.command.help.filter")));
        source.sendFeedback(Text.literal("§e/starredheltix reloadconfig §7- ").append(Text.translatable("starredheltix.command.help.reloadconfig")));
        source.sendFeedback(Text.literal("§e/starredheltix debug §7- ").append(Text.translatable("starredheltix.command.help.debug")));
        source.sendFeedback(Text.literal("§e/starredheltix debug toggle §7- ").append(Text.translatable("starredheltix.command.help.debug.toggle")));
        source.sendFeedback(Text.literal("§e/starredheltix experiment §7- ").append(Text.translatable("starredheltix.command.help.experiment")));
        source.sendFeedback(Text.literal("§e/starredheltix config §7- ").append(Text.translatable("starredheltix.command.help.config")));
        source.sendFeedback(Text.literal("§e/starredheltixbloodroom §7- ").append(Text.translatable("starredheltix.command.help.bloodroom")));
        source.sendFeedback(Text.literal("§e/starredheltix endermenhighlight §7- ").append(Text.translatable("starredheltix.command.help.endermenhighlight")));
        source.sendFeedback(Text.literal("§e/starredheltix wolfhighlight §7- ").append(Text.translatable("starredheltix.command.help.wolfhighlight")));
        source.sendFeedback(Text.literal("§e/starredheltix inventorywarning toggle §7- ").append(Text.translatable("starredheltix.command.help.inventorywarning")));
source.sendFeedback(Text.literal("§e/starredheltix slotlocking toggle §7- ").append(Text.translatable("starredheltix.command.help.slotlocking")));
        source.sendFeedback(Text.literal("§e/starredheltix fairysouls §7- ").append(Text.translatable("starredheltix.command.help.fairysouls")));
       source.sendFeedback(Text.literal("§e/starredheltix discordrpc §7- ").append(Text.translatable("starredheltix.command.help.discordrpc")));
        source.sendFeedback(Text.literal("§e/starredheltix woodwormcooldown §7- Toggle Woodworm axe cooldown visualization"));
        source.sendFeedback(Text.literal("§e/starredheltix woodwormcooldown toggle §7- Toggle Woodworm axe cooldown visualization"));
        source.sendFeedback(Text.literal("§e/starredheltix woodwormcooldown percentage <value> §7- Set Woodworm axe cooldown percentage (0-100%)"));
        source.sendFeedback(Text.literal("§e/starredheltix hidearmor §7- Toggle armor hiding"));
        source.sendFeedback(Text.literal("§e/starredheltix equipment §7- Toggle equipment display"));
        source.sendFeedback(Text.literal("§e/starredheltix partycommands §7- Party commands configuration"));
        source.sendFeedback(Text.literal("§e/starredheltix partycommands privatemessage §7- Toggle private message commands"));
        source.sendFeedback(Text.literal("§e/starredheltix characterhighlight §7- Toggle character highlighter"));
        source.sendFeedback(Text.literal("§e/starredheltix autosprint §7- Toggle auto-sprint feature"));
        source.sendFeedback(Text.literal("§e/вход §7- ").append(Text.translatable("starredheltix.command.help.login")));
        source.sendFeedback(Text.literal("§e/яготовлёвал §7- ").append(Text.translatable("starredheltix.command.help.readyphrase")));
        source.sendFeedback(Text.literal("§7 Config options:"));
        source.sendFeedback(Text.literal("§7    - §e/starredheltix config password <password> §7- ").append(Text.translatable("starredheltix.command.help.config.password")));
        source.sendFeedback(Text.literal("§7    - §e/starredheltix config readyphrase <phrase> §7- ").append(Text.translatable("starredheltix.command.help.config.readyphrase")));
    }
}