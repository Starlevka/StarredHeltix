package set.starlev.starredheltix.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.chat.MessageFilterManager;
import set.starlev.starredheltix.util.qol.VotingReminder;
import set.starlev.starredheltix.util.binds.CustomBindManager;
import set.starlev.starredheltix.util.updater.ModUpdater;
import set.starlev.starredheltix.util.ModVersionRegistry;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class StarredHeltixCommands {

    public static void registerCommands() {
        // Register the main command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("starredheltix");
            
            // Add update command
            builder.then(ClientCommandManager.literal("update")
               .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("§eПроверка обновлений..."));
                    ModUpdater.checkForUpdates();
                    return 1;
                })

                .then(ClientCommandManager.literal("open")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§eОткрытие страницы проекта..."));
                        ModUpdater.openProjectPage();
                        return 1;
                   }))
                .then(ClientCommandManager.literal("install")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§eЗагрузка и установка обновления..."));
                 ModUpdater.downloadUpdate();
                        return 1;
                    })
                )
            );
            
            //QOL commands
            registerQolCommands(builder);
            
            // Utility commands
            registerUtilityCommands(builder);
            
            // Debug commands
            registerDebugCommands(builder);

            // Test commands
            registerTestCommands(builder);

            // Partycommands
            registerPartyCommands(builder);
            // Configuration commands
            registerConfigCommands(builder);

            // Blood room commands
            registerBloodRoomCommands(builder);

            // Custom binds commands
            registerBindsCommands(builder);

            // Feature-specific commands with help and toggle functionality
            registerFeatureCommands(builder);

            dispatcher.register(builder);
        });
        
        // Register sh_check command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> checkBuilder = ClientCommandManager.literal("sh_check");
            checkBuilder.then(ClientCommandManager.argument("player", StringArgumentType.word())
                .executes(context -> {
                    String targetPlayer = StringArgumentType.getString(context, "player");
                    MinecraftClient client = MinecraftClient.getInstance();
                    
                    if (client.player != null) {
                        String playerName = client.player.getName().getString();
                        
                        // Check if current player is a moderator
                        String[] moderators = {"Starlev", "ZurGames", "MegaChromeX"};
                        boolean isModerator = false;
                        for (String moderator : moderators) {
                            if (moderator.equalsIgnoreCase(playerName)) {
                                isModerator = true;
                                break;
                            }
                        }
                        
                        if (!isModerator) {
                            context.getSource().sendError(Text.literal("§cУ вас нет прав на использование этой команды"));
                            return 0;
                        }
                        
                        String status;
                        if (client.player.getName().getString().equalsIgnoreCase(targetPlayer)) {
                            status = "§aИМЕЕТ МОД (v0.0.6 - ВЫ)";
                        } else {
                            String playerVersion = ModVersionRegistry.getPlayerVersion(targetPlayer);
                            if (playerVersion != null) {
                                status = "§aИМЕЕТ МОД (" + playerVersion + ")";
                            } else {
                                status = "§cНЕ ОБНАРУЖЕН";
                            }
                        }
                        context.getSource().sendFeedback(Text.literal("§e[ПРОВЕРКА] §fИгрок §e" + targetPlayer + "§f: " + status));
                    }
                    
                    return 1;
                })
            );
            dispatcher.register(checkBuilder);
        });
        
        //Register standalonecommands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Login command - /вход
            LiteralArgumentBuilder<FabricClientCommandSource> loginBuilder = ClientCommandManager.literal("вход");
            loginBuilder.executes(context -> {
                if (!StarredHeltixClient.CONFIG.loginPassword.isEmpty()) {
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("login " + StarredHeltixClient.CONFIG.loginPassword);
                    context.getSource().sendFeedback(Text.literal("§aУспешный вход в систему"));
                } else {
                    context.getSource().sendError(Text.literal("§cПароль для входа не установлен. Используйте /starredheltix config password <пароль>"));
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
                    context.getSource().sendFeedback(Text.literal("§aСообщение о готовности отправлено"));
        } else {
                    context.getSource().sendError(Text.literal("§cЧат отключен в настройках"));
                }
                return 1;
            });
            dispatcher.register(readyBuilder);
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /d command - sends /dh
            LiteralArgumentBuilder<FabricClientCommandSource> dBuilder = ClientCommandManager.literal("d");
            dBuilder.executes(context -> {
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("dh");
                return 1;
            });
            dispatcher.register(dBuilder);
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /в command - sends /dh
            LiteralArgumentBuilder<FabricClientCommandSource> vBuilder = ClientCommandManager.literal("в");
            vBuilder.executes(context -> {
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("dh");
                return 1;
            });
            dispatcher.register(vBuilder);
        });
    }

    private static void registerQolCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        //Toggle command - enables/disables the mod
        builder.then(ClientCommandManager.literal("toggle")
            .executes(context -> {
                StarredHeltixClient.CONFIG.general.enabled = !StarredHeltixClient.CONFIG.general.enabled;
                StarredHeltixClient.CONFIG.save();
                String status = StarredHeltixClient.CONFIG.general.enabled ?
                    "§a§l✓ ВКЛЮЧЕНО" :
                    "§c§l✗ ВЫКЛЮЧЕНО";
                context.getSource().sendFeedback(Text.literal("§6§l[StarredHeltix] §f" + status));
                return 1;
            })
        );

        // Filter commands - manage message filters
        LiteralArgumentBuilder<FabricClientCommandSource> filterBuilder = ClientCommandManager.literal("filter");
        filterBuilder.then(ClientCommandManager.literal("add")
            .then(ClientCommandManager.argument("prefix", StringArgumentType.greedyString())
                .executes(context -> {
                    String prefix = StringArgumentType.getString(context, "prefix");
                    // Generate a new ID for the filter
                    AtomicInteger newId = new AtomicInteger(0);
                    MessageFilterManager.getFilters().keySet().forEach(id -> {
                        if (id >= newId.get()) {
                            newId.set(id + 1);
                        }
                    });
                    int id = newId.get();
                    MessageFilterManager.addFilter(id, prefix);
                    context.getSource().sendFeedback(Text.literal("§aФильтр добавлен с ID " + id + " для префикса: " + prefix));
                    return 1;
                })
            )
            .executes(context -> {
                context.getSource().sendError(Text.literal("§cИспользование: /starredheltix filter add <префикс>"));
                return 1;
            })
        );
        filterBuilder.then(ClientCommandManager.literal("remove")
            .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(0))
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    if (MessageFilterManager.getFilters().containsKey(id)) {
                        MessageFilterManager.removeFilter(id);
                        context.getSource().sendFeedback(Text.literal("§aФильтр с ID " + id + " удален"));
                    } else {
                        context.getSource().sendError(Text.literal("§cФильтр с ID " + id + " не найден"));
                    }
                    return 1;
                })
            )
            .executes(context -> {
                context.getSource().sendError(Text.literal("§cИспользование: /starredheltix filter remove <id>"));
                return 1;
            })
        );
        filterBuilder.then(ClientCommandManager.literal("list")
            .executes(context -> {
                Map<Integer, String> filters = MessageFilterManager.getFilters();
                if (filters.isEmpty()) {
                    context.getSource().sendFeedback(Text.literal("§eФильтры ненастроены"));
                } else {
                    context.getSource().sendFeedback(Text.literal("§a=== Фильтры сообщений ==="));
                    filters.forEach((id, prefix) -> context.getSource().sendFeedback(Text.literal("§e" + id + ": §f" + prefix)));
                }
                return 1;
            })
        );
        filterBuilder.then(ClientCommandManager.literal("clear")
            .executes(context -> {
                int count = MessageFilterManager.getFilters().size();
                MessageFilterManager.getFilters().clear();
                StarredHeltixClient.CONFIG.save();
                context.getSource().sendFeedback(Text.literal("§aОчищено " + count + " фильтров"));
                return 1;
            })
        );
            
        builder.then(filterBuilder);

        // Voting reminder commands
        LiteralArgumentBuilder<FabricClientCommandSource> votingBuilder = ClientCommandManager.literal("voting");
        votingBuilder.then(ClientCommandManager.literal("toggle")
            .executes(context -> {
                VotingReminder.setVotingReminderEnabled(!VotingReminder.isVotingReminderEnabled());
                boolean enabled = VotingReminder.isVotingReminderEnabled();
                String status = enabled ? "§a§l✓ ВКЛЮЧЕНЫ" : "§c§l✗ ВЫКЛЮЧЕНЫ";
                context.getSource().sendFeedback(Text.literal("§6§l[Голосование] §f" + status));
                return 1;
            })
        );
        votingBuilder.then(ClientCommandManager.literal("dayreset")
            .executes(context -> {
               VotingReminder.forceDayReset();
               context.getSource().sendFeedback(Text.literal("§6День для напоминаний о голосовании сброшен"));
                // Сразу проверяем, нужно ли показать напоминание
                VotingReminder.checkAndShowReminder();
                return 1;
            })

        );

        builder.then(votingBuilder);
        

    }

    private static void registerUtilityCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        //Reload config command
       builder.then(ClientCommandManager.literal("reloadconfig")
            .executes(context -> {
                try {
                    StarredHeltixClient.reloadConfig();
                    return 1;
                } catch (Exception e) {
                    context.getSource().sendError(Text.literal("§cОшибка при перезагрузке конфигурации: " + e.getMessage()));
                    return 0;
                }
            })
        );
    }

    private static void registerDebugCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Debug command
        builder.then(ClientCommandManager.literal("debug")
            .executes(context -> {
               MinecraftClient client = MinecraftClient.getInstance();
               if (client.player != null) {
                    // Get nearby entities
                    int endermanCount = 0;
                    int wolfCount = 0;
                    if (client.world != null) {
                        for (Entity entity : client.world.getEntities()) {
                            if (entity instanceof EndermanEntity) {
                               endermanCount++;
                            } else if (entity instanceof WolfEntity) {
                                wolfCount++;
                            }
                        }
                    }
                    context.getSource().sendFeedback(Text.literal("§a=== Отладочная информация StarredHeltix ==="));
                    context.getSource().sendFeedback(Text.literal("§eИгрок: §f" + client.player.getName().getString()));
                    context.getSource().sendFeedback(Text.literal("§eМир: §f" + (client.world != null ? client.world.getRegistryKey().getValue() : "null")));
                    context.getSource().sendFeedback(Text.literal("§eСущности: §f(Эндермены: " + endermanCount + ", Волки:" + wolfCount + ")"));
                    context.getSource().sendFeedback(Text.literal("§eFPS: §f" + client.getCurrentFps()));
                    
                    // Show registered players with mod
                    Map<String, String> registeredPlayers = ModVersionRegistry.getAllRegisteredPlayers();
                    context.getSource().sendFeedback(Text.literal("§eИгроки с модом: §f" + registeredPlayers.size()));
                    registeredPlayers.forEach((name, version) -> 
                        context.getSource().sendFeedback(Text.literal("§f- " + name + " (v" + version + ")"))
                    );

                }
                return 1;
            })
            .then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                   StarredHeltixClient.CONFIG.general.debugMode = !StarredHeltixClient.CONFIG.general.debugMode;
                    StarredHeltixClient.CONFIG.save();
                    String status = StarredHeltixClient.CONFIG.general.debugMode ?
                        "§aвключено":
                        "§cвыключено";
                    context.getSource().sendFeedback(Text.literal("§aРежим отладки: " + status));
                    if (StarredHeltixClient.CONFIG.general.debugMode) {
                        context.getSource().sendFeedback(Text.literal("§eВывод отладки ChatEventsManager включен. Проверьте консоль для обнаружения сообщений."));
                    }
                    return 1;
                })
            )
        );
    }

    private static void registerTestCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Ability cooldown commands
        builder.then(ClientCommandManager.literal("abilitycooldown")
            .executes(context -> {
                boolean enabled = StarredHeltixClient.CONFIG.abilityCooldown.enabled;
                String status = enabled ? "§aenabled" : "§cdisabled";
               context.getSource().sendFeedback(Text.literal("§6§l[Таймеры способностей] §f" + status));
                context.getSource().sendFeedback(Text.literal("§7Киркобулус: " + (StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled ? "§aвключено" : "§cвыключено")+ " §7(" + StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusCooldown + "с)"));
                context.getSource().sendFeedback(Text.literal("§7Увеличение скорости копания: " + (StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled ? "§aвключено": "§cвыключено")+ " §7(" + StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostCooldown + "с)"));
                return 1;
            })
            .then(ClientCommandManager.literal("toggle")
               .executes(context -> {
                   StarredHeltixClient.CONFIG.abilityCooldown.enabled = !StarredHeltixClient.CONFIG.abilityCooldown.enabled;
                    StarredHeltixClient.CONFIG.save();
                    String status = StarredHeltixClient.CONFIG.abilityCooldown.enabled ? "§a§l✓ ВКЛЮЧЕНЫ" : "§c§l✗ ВЫКЛЮЧЕНЫ";
                    context.getSource().sendFeedback(Text.literal("§6§l[Таймеры способностей] §f" + status));
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("kirkobulus")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled = !StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled;
                   StarredHeltixClient.CONFIG.save();
                    String status = StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled ? "§aenabled" : "§cdisabled";
                    context.getSource().sendFeedback(Text.literal("§6Обнаружение Киркобулус " + status));
                    return 1;
               })
            )
            .then(ClientCommandManager.literal("miningspeedboost")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled = !StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled;
                    StarredHeltixClient.CONFIG.save();
                   String status = StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled ? "§aenabled" : "§cdisabled";
                    context.getSource().sendFeedback(Text.literal("§6Обнаружение Увеличение скорости копания " + status));
                    return 1;
               })
            )
        );
        
        // TreeCap cooldown commands
        builder.then(ClientCommandManager.literal("treecap")
            .executes(context -> {
                boolean enabled = StarredHeltixClient.CONFIG.treecapCooldown.enabled;
                int percentage = StarredHeltixClient.CONFIG.treecapCooldown.cooldownPercentage;
                String status = enabled ? "§a§l✓ ВКЛЮЧЕН" : "§c§l✗ ВЫКЛЮЧЕН";
                context.getSource().sendFeedback(Text.literal("§6§l[Таймер Древоточеца] §f" + status));
                context.getSource().sendFeedback(Text.literal("§7Процент уменьшения: §e" + percentage + "%"));
                return 1;
            })
            .then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                    StarredHeltixClient.CONFIG.treecapCooldown.enabled = !StarredHeltixClient.CONFIG.treecapCooldown.enabled;
                    StarredHeltixClient.CONFIG.save();
                    String status = StarredHeltixClient.CONFIG.treecapCooldown.enabled ? "§a§l✓ ВКЛЮЧЕН" : "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Таймер Древоточеца] §f" + status));
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("percentage")
                .then(ClientCommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
                    .executes(context -> {
                        int percentage = IntegerArgumentType.getInteger(context, "percent");
                        StarredHeltixClient.CONFIG.treecapCooldown.cooldownPercentage = percentage;
                        StarredHeltixClient.CONFIG.save();
                        context.getSource().sendFeedback(Text.literal("§6§l[Древоточец] §fПроцент уменьшения: §e" + percentage + "%"));
                        return 1;
                    })
                )
            )
        );
    }

   private static void registerPartyCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Party commands toggle with individual command toggles
       LiteralArgumentBuilder<FabricClientCommandSource> partyBuilder = getFabricClientCommandSourceLiteralArgumentBuilder();

       // Individual party command toggles
        partyBuilder.then(ClientCommandManager.literal("promote")
            .executes(context -> {
                StarredHeltixClient.CONFIG.partyCommands.partyPromoteEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyPromoteEnabled;
                StarredHeltixClient.CONFIG.save();
                String status = StarredHeltixClient.CONFIG.partyCommands.partyPromoteEnabled ? 
                    "§a§l✓ ВКЛЮЧЕНА" : "§c§l✗ ВЫКЛЮЧЕНА";
                context.getSource().sendFeedback(Text.literal("§6§l[!promote] §f" + status));
                return 1;
            })
        );
        
        partyBuilder.then(ClientCommandManager.literal("kick")
            .executes(context -> {
                StarredHeltixClient.CONFIG.partyCommands.partyKickEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyKickEnabled;
                StarredHeltixClient.CONFIG.save();
                String status = StarredHeltixClient.CONFIG.partyCommands.partyKickEnabled ? 
                    "§a§l✓ ВКЛЮЧЕНА" : "§c§l✗ ВЫКЛЮЧЕНА";
                context.getSource().sendFeedback(Text.literal("§6§l[!kick] §f" + status));
                return 1;
            })
        );
        
        partyBuilder.then(ClientCommandManager.literal("invite")
            .executes(context -> {
                StarredHeltixClient.CONFIG.partyCommands.partyInviteEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyInviteEnabled;
                StarredHeltixClient.CONFIG.save();
                String status = StarredHeltixClient.CONFIG.partyCommands.partyInviteEnabled ? 
                    "§a§l✓ ВКЛЮЧЕНА" : "§c§l✗ ВЫКЛЮЧЕНА";
                context.getSource().sendFeedback(Text.literal("§6§l[!invite] §f" + status));
                return 1;
            })
        );
        
        builder.then(partyBuilder);
   }

    private static @NotNull LiteralArgumentBuilder<FabricClientCommandSource> getFabricClientCommandSourceLiteralArgumentBuilder() {
        LiteralArgumentBuilder<FabricClientCommandSource> partyBuilder = ClientCommandManager.literal("partycommands");

        partyBuilder.executes(context -> {
            StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled;
            StarredHeltixClient.CONFIG.save();
            String status = StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ?
                "§a§l✓ ВКЛЮЧЕНЫ" : "§c§l✗ ВЫКЛЮЧЕНЫ";
            context.getSource().sendFeedback(Text.literal("§6§l[Команды пати] §f" + status));
            return 1;
        });
        return partyBuilder;
    }

    private static void registerConfigCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
       // Configuration commands
        LiteralArgumentBuilder<FabricClientCommandSource> configBuilder = ClientCommandManager.literal("config");
        
        configBuilder.then(ClientCommandManager.literal("password")
           .then(ClientCommandManager.argument("password", StringArgumentType.word())
                .executes(context -> {
                    StarredHeltixClient.CONFIG.loginPassword = StringArgumentType.getString(context, "password");
                   StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aПароль для входа установлен"));
                    return 1;
                })
            )
        );
        configBuilder.then(ClientCommandManager.literal("readyphrase")
            .then(ClientCommandManager.argument("phrase", StringArgumentType.greedyString())
                .executes(context -> {
                    String phrase = StringArgumentType.getString(context, "phrase");
                    StarredHeltixClient.CONFIG.partyCommands.customReadyPhrase = phrase;
                    StarredHeltixClient.CONFIG.save();
                    context.getSource().sendFeedback(Text.literal("§aФраза готовности установлена"));
                    return 1;
                })
            )
        );
        
        builder.then(configBuilder);
   }



  private static void registerBloodRoomCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
       // Blood room timer command
       builder.then(ClientCommandManager.literal("bloodroom")
            .executes(context -> {
               StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled = !StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled;
               StarredHeltixClient.CONFIG.save();
               String status = StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ?
                    "§aвключено" :
                    "§cвыключено";
                context.getSource().sendFeedback(Text.literal("§aТаймер кровавой комнаты: " + status));
                return 1;
           })
        );
    }

   private static void registerFeatureCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        registerFeatureCommand(builder, "inventorywarning", "Toggles inventory full warning notifications", "starredheltix.command.help.inventorywarning");
        registerFeatureCommand(builder, "slotlocking", "Toggles slot locking functionality", "starredheltix.command.help.slotlocking.toggle");
        registerFeatureCommand(builder, "bloodroom", "Toggles blood room timer", "starredheltix.command.help.bloodroom.toggle");
        registerFeatureCommand(builder, "partycommands", "Configures party commands", "starredheltix.command.help.partycommands");
        registerFeatureCommand(builder, "fishingnotification", "Toggles fishing notification", "starredheltix.command.help.fishingnotification");
        registerFeatureCommand(builder, "threeweirdos", "Toggles three weirdos solver","starredheltix.command.help.threeweirdos");
        registerFeatureCommand(builder, "autosprint", "Toggles auto-sprint feature", "starredheltix.command.help.autosprint");
        registerFeatureCommand(builder, "titleblocking", "Toggles title blocking for super rare messages", "starredheltix.command.help.titleblocking");
        registerFeatureCommand(builder, "autopartychat", "Toggles auto party chat for ! commands", "starredheltix.command.help.autopartychat");
        registerFeatureCommand(builder, "endermanhighlighter", "Toggles enderman highlighting", "starredheltix.command.help.endermanhighlighter");
        registerFeatureCommand(builder, "wolfhighlighter", "Toggles wolf highlighting", "starredheltix.command.help.wolfhighlighter");
        registerFeatureCommand(builder, "waypoints", "Toggles waypoints from party coords", "starredheltix.command.help.waypoints");
   }

    private static void registerFeatureCommand(LiteralArgumentBuilder<FabricClientCommandSource> builder, String featureName, String description, String toggleCommandKey) {
        LiteralArgumentBuilder<FabricClientCommandSource> featureBuilder = ClientCommandManager.literal(featureName);
        
        //Toggle functionality
        featureBuilder.executes(context -> {
            switch(featureName) {
               case "inventorywarning":
                    StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled = !StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled;
                    StarredHeltixClient.CONFIG.save();
                   String status = StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled ?
                        "§a§l✓ ВКЛЮЧЕНО":
                       "§c§l✗ ВЫКЛЮЧЕНО";
                    context.getSource().sendFeedback(Text.literal("§6§l[Предупреждение инвентаря] §f" + status));
                    return 1;
                    
               case "slotlocking":
                    StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled = !StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled ?
                        "§a§l✓ ВКЛЮЧЕНО":
                        "§c§l✗ ВЫКЛЮЧЕНО";
                  context.getSource().sendFeedback(Text.literal("§6§l[Блокировка слотов] §f" + status));
                    return 1;
                    
                case "bloodroom":
                    StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled = !StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled;
                 StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Кровавая комната] §f" + status));
                   return 1;
                    

                case "partycommands":
                    StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled = !StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled;
                   StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                       "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Команды пати] §f" + status));
                    return 1;
                    
                case "fishingnotification":
                    StarredHeltixClient.CONFIG.fishingNotification.enabled = !StarredHeltixClient.CONFIG.fishingNotification.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.fishingNotification.enabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Уведомления рыбалки] §f" + status));
                    return 1;

                case "autosprint":
                    StarredHeltixClient.CONFIG.autoSprint.enabled = !StarredHeltixClient.CONFIG.autoSprint.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.autoSprint.enabled ?
                       "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Авто-Спринт] §f" + status));
                    return 1;

                case "threeweirdos":
                    StarredHeltixClient.CONFIG.threeWeirdos.enabled = !StarredHeltixClient.CONFIG.threeWeirdos.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.threeWeirdos.enabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Три незнакомца] §f" + status));
                    return 1;

                case "titleblocking":
                    StarredHeltixClient.CONFIG.titleBlocking.enabled = !StarredHeltixClient.CONFIG.titleBlocking.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.titleBlocking.enabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Блокировка Title] §f" + status));
                    return 1;

                case "autopartychat":
                    StarredHeltixClient.CONFIG.partyCommands.autoPartyChat = !StarredHeltixClient.CONFIG.partyCommands.autoPartyChat;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.partyCommands.autoPartyChat ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Авто пати чат] §f" + status));
                    return 1;

                case "endermanhighlighter":
                    StarredHeltixClient.CONFIG.endermanHighlighter.enabled = !StarredHeltixClient.CONFIG.endermanHighlighter.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.endermanHighlighter.enabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Подсветка эндерменов] §f" + status));
                    return 1;

                case "wolfhighlighter":
                    StarredHeltixClient.CONFIG.wolfHighlighter.enabled = !StarredHeltixClient.CONFIG.wolfHighlighter.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.wolfHighlighter.enabled ?
                        "§a§l✓ ВКЛЮЧЕН" :
                        "§c§l✗ ВЫКЛЮЧЕН";
                    context.getSource().sendFeedback(Text.literal("§6§l[Подсветка волков] §f" + status));
                    return 1;

                case "waypoints":
                    StarredHeltixClient.CONFIG.waypoints.enabled = !StarredHeltixClient.CONFIG.waypoints.enabled;
                    StarredHeltixClient.CONFIG.save();
                    status = StarredHeltixClient.CONFIG.waypoints.enabled ?
                        "§a§l✓ ВКЛЮЧЕНЫ" :
                        "§c§l✗ ВЫКЛЮЧЕНЫ";
                    context.getSource().sendFeedback(Text.literal("§6§l[Вейпоинты] §f" + status));
                    return 1;

               default:
                    context.getSource().sendError(Text.literal("Неизвестная функция: " + featureName));
                    return 0;
            }
        });
        builder.then(featureBuilder);
    }
    
    private static String getFeatureStatus(String featureName) {
        switch (featureName) {
            case "inventorywarning":
                return StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled ? "включено" : 
                   "выключено";
                    
            case "slotlocking":
                return StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled ? 
                 "включено" : "выключено";
                    
            case "bloodroom":
                return StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled ? 
                    "включено" : "выключено";
                    

                    
            case "partycommands":
                return StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled ? 
                    "включено" : 
                  "выключено";
                    
            case "autosprint":
                return StarredHeltixClient.CONFIG.autoSprint.enabled ? "включено" : 
                   "выключено";

            case "threeweirdos":
                return StarredHeltixClient.CONFIG.threeWeirdos.enabled ?
                    "включено" : "выключено";

            case "titleblocking":
                return StarredHeltixClient.CONFIG.titleBlocking.enabled ?
                    "включено" : "выключено";

            case "autopartychat":
                return StarredHeltixClient.CONFIG.partyCommands.autoPartyChat ?
                    "включено" : "выключено";

            default:
               return null;
        }
    }

   private static void registerBindsCommands(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        // Custom binds commands
       LiteralArgumentBuilder<FabricClientCommandSource> bindsBuilder = literal("binds");
        bindsBuilder.then(literal("create")
           .then(argument("name", StringArgumentType.string())
                .then(argument("command", StringArgumentType.string())
                    .executes(context -> createBind(
                        context.getSource(),
                        StringArgumentType.getString(context,"name"),
                        StringArgumentType.getString(context, "command")
                    ))
                )
           )
        );
        bindsBuilder.then(literal("delete")
            .then(argument("name", StringArgumentType.string())
                .executes(context -> deleteBind(
                    context.getSource(),
                    StringArgumentType.getString(context, "name")
                ))
            )
        );
        bindsBuilder.then(literal("list")
            .executes(context -> listBinds(context.getSource()))
       );
        bindsBuilder.then(literal("setkey")
            .then(argument("name", StringArgumentType.string())
                .then(argument("key", StringArgumentType.string())
                    .executes(context -> setBindKey(
                        context.getSource(),
                        StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "key")
                    ))
                )
            )
        );

        builder.then(bindsBuilder);
    }

    private static int createBind(FabricClientCommandSource source, String name, String command) {
        if (!command.startsWith("/")) {
            source.sendError(Text.literal("§cКоманда должна начинаться с '/'"));
            return 0;
        }

        boolean success = CustomBindManager.createBind(name, command);
       return success ? 1 : 0;
   }

    private static int deleteBind(FabricClientCommandSource source, String name) {
        boolean success = CustomBindManager.deleteBind(name);
        return success ? 1 : 0;
    }

    private static int listBinds(FabricClientCommandSource source){
        CustomBindManager.listBinds();
       return 1;
   }
   
    private static int setBindKey(FabricClientCommandSource source, String name, String key) {
        boolean success = CustomBindManager.setBindKey(name, key);
        return success ? 1 : 0;
    }
    
    private static boolean isVersionAtLeast(String version, String minVersion) {
        String[] versionParts = version.split("\\.");
        String[] minVersionParts = minVersion.split("\\.");
        
        for (int i = 0; i < Math.max(versionParts.length, minVersionParts.length); i++) {
            int versionPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int minVersionPart = i < minVersionParts.length ? Integer.parseInt(minVersionParts[i]) : 0;
            
            if (versionPart > minVersionPart) {
                return true;
            } else if (versionPart < minVersionPart) {
                return false;
            }
        }
        
        return true;
    }
}