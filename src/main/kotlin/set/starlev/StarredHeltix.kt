package set.starlev

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.gui.screens.Screen
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import set.starlev.render.RenderEngine
import set.starlev.config.ConfigManager
import set.starlev.config.Features
import set.starlev.features.combat.dungeons.solvers.ThreeWeirdos
import set.starlev.features.misc.MouseLock
import set.starlev.features.misc.WelcomeMessage

import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.world.entity.monster.Monster
import set.starlev.registry.EntityRegistry
import set.starlev.render.MegaChestRenderer

class StarredHeltix : ClientModInitializer {
    override fun onInitializeClient() {
        LOGGER.info("Инициализация ЛлЛлЛлЛлЛл StarredHeltix...")
        
        EntityRegistry.init()
        EntityRenderers.register(EntityRegistry.MEGA_CHEST_MAGMA, ::MegaChestRenderer)

        // Регистрация атрибутов для кастомных сущностей (необходимо для работы ИИ и предотвращения крашей)
        net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry.register(EntityRegistry.MEGA_CHEST_MAGMA, Monster.createMonsterAttributes())

        configManager = ConfigManager
        configManager.firstLoad()
        set.starlev.secret.config.SecretMenuManager.load(forceSave = false)

        ModSounds.register()

        set.starlev.commands.ConfigCommand.register()
        set.starlev.events.KeyBindHandler.init()
        set.starlev.features.combat.EntityHighlight.init()
        set.starlev.utils.detectors.SkillXpDetector.init()
        set.starlev.features.chat.PartyCommands.init()
        set.starlev.secret.features.AutoResponder.init()
        set.starlev.features.combat.dungeons.DeathCounter.init()
        set.starlev.features.chat.CustomBindManager.init()
        set.starlev.features.misc.MouseLock.init()
        set.starlev.features.combat.slayer.AutoSlayer.init()
        set.starlev.features.combat.slayer.SlayerHud.init()
        set.starlev.features.combat.slayer.SlayerScoreboard.init()
        WelcomeMessage.init()
        set.starlev.features.visual.GhostFrameFeature.init()
        // set.starlev.features.visual.GhostNPCHandler.init() - Удалено из меню
        set.starlev.features.visual.MegaChestNPCHandler.init()
        set.starlev.features.visual.Fullbright.init()
        set.starlev.features.visual.InventoryHistoryLog.init()
        set.starlev.secret.features.SecretFunFeatures.init()
        set.starlev.features.chat.mod.MacroCheck.init()
        set.starlev.features.mining.DwarvenWaypoints.init()
        set.starlev.features.mining.AutoCommissions.init()
        set.starlev.utils.detectors.ContainerDetector.init()
        set.starlev.utils.detectors.ActionBarDetector
        set.starlev.utils.detectors.MuseumDetector.init()
        set.starlev.features.skyblock.Museum.init()

        // Register chat listeners
        set.starlev.features.chat.ChatEventsManager.registerIncoming { message ->
            // AutoResponder
            set.starlev.secret.features.AutoResponder.onChatMessage(message)

            // Detect mining ability usage using patterns
            val pickaxePattern = Regex(".*Вы использовали Киркобулус!.*")
            val speedPattern = Regex(".*Вы использовали Увеличение скорости копания!.*")

            if (pickaxePattern.matches(message)) {
                set.starlev.features.mining.PickaxeCooldownHud.onPickaxeBoostUsed()
            } else if (speedPattern.matches(message)) {
                set.starlev.features.mining.SpeedBoostCooldownHud.onSpeedBoostUsed()
            }
        }
        
        // Инициализировать event listener для TreeCapCooldown (регистрирует TreeCapBlockBreakMixin с проверкой логов)
        set.starlev.features.foraging.TreeCapCooldown
        
        // Инициализировать HUD систему
        set.starlev.hud.HudManager.init()
        
        // Инициализировать солверы подземелий
        set.starlev.features.combat.dungeons.BloodRoomTimer.init()
        set.starlev.features.combat.dungeons.solvers.ThreeWeirdos.init()
        set.starlev.features.combat.dungeons.solvers.CreeperBeams.init()
        set.starlev.features.combat.dungeons.ScoreCounter.init()
        set.starlev.features.fishing.LegendaryFishingNotifier.init()
        
        // Регистрация сохранения конфига при выходе из игры
        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            configManager.saveConfig("client-stopping")
            set.starlev.hud.HudManager.saveAllLayouts()
        }

        LOGGER.info("StarredHeltix инициализирован.")

        Runtime.getRuntime().addShutdownHook(Thread {
            configManager.saveConfig("shutdown-hook")
        })
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("StarredHeltix")
        lateinit var configManager: ConfigManager

        @JvmStatic
        val feature: Features get() = configManager.features

        var screenToOpen: Screen? = null
    }
}
