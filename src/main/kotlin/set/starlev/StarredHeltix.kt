package set.starlev

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.gui.screens.Screen
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import set.starlev.config.ConfigManager
import set.starlev.config.Features

class StarredHeltix : ClientModInitializer {
    override fun onInitializeClient() {

        configManager = ConfigManager
        configManager.firstLoad()
        set.starlev.secret.config.SecretMenuManager.load(forceSave = false)

        set.starlev.commands.ConfigCommand.register()
        set.starlev.events.KeyBindHandler.init()
        set.starlev.events.WorldChangeHandler.init()
        set.starlev.utils.GameModeBoost.init()
        set.starlev.features.optimization.BorderlessFullscreen.init()
        set.starlev.features.combat.EntityHighlight.init()
        set.starlev.features.combat.ClickHighlight.init()
        set.starlev.features.combat.BonemerangRange.init()
        set.starlev.features.mining.EndHighlight.init()
        set.starlev.utils.detectors.SkillXpDetector.init()
        set.starlev.features.chat.PartyCommands.init()
        set.starlev.features.chat.CustomBindManager.init()
        set.starlev.features.chat.CommandAliases.init()
        set.starlev.features.chat.NotifierCenter.init()
        set.starlev.features.farming.MouseLock.init()
        set.starlev.features.combat.slayer.SlayerHud.init()
        set.starlev.features.skyblock.UpdateOnJoin.init()
        set.starlev.features.visual.GhostFrameFeature.init()
        set.starlev.features.farming.SeeThroughWindow.init()
        set.starlev.render.ScriptNPCManager.init()
        set.starlev.features.skyblock.InventoryHistoryLog.init()
        set.starlev.features.skyblock.Waypoints.init()
        set.starlev.secret.features.SecretFunFeatures.init()
        set.starlev.features.chat.mod.MacroCheck.init()
        set.starlev.features.chat.mod.StaffGreeting.init()
        set.starlev.features.mining.DwarvenWaypoints.init()
        set.starlev.features.mining.AutoCommissions.init()
        set.starlev.utils.detectors.ContainerDetector.init()
        set.starlev.utils.detectors.MuseumDetector.init()
        set.starlev.features.skyblock.Museum.init()
        // Скан магической силы по открытому контейнеру — после ContainerDetector.init().
        set.starlev.features.skyblock.scoreboard.elements.ScoreboardElementPower.init()
        set.starlev.features.mining.PickaxeCooldownHud.init()
        set.starlev.features.mining.SpeedBoostCooldownHud.init()
        set.starlev.features.farming.RancherSpeedHud.init()
        set.starlev.features.farming.garden.GardenPlotsOverlay.init()
        set.starlev.features.farming.garden.PestHighlight.init()
        set.starlev.features.fishing.GlowingMushroomHighlight.init()
        set.starlev.features.skyblock.InventoryButtonsManager.init()
        set.starlev.features.skyblock.EquipmentOverlay.init()
        // Солверы стола экспериментов: подписки на открытие/закрытие контейнера,
        // обновления слотов (EventBus), рендер подсветок и перехват кликов.
        // init() обязателен — без него подсистема не регистрирует ни одного слушателя.
        set.starlev.events.ContainerEventWire.init()
        set.starlev.features.enchanting.ExperimentSolverManager.init()

        // Инициализировать HUD систему
        set.starlev.render.HudManager.init()

        // Инициализировать данжи и солверы
        set.starlev.features.combat.dungeons.BloodRoomTimer.init()
        set.starlev.features.combat.dungeons.DungeonMapHud.init()
        set.starlev.features.combat.dungeons.solvers.ThreeWeirdos.init()
        set.starlev.features.combat.dungeons.solvers.CreeperBeams.init()
        set.starlev.features.combat.dungeons.solvers.TicTacToe.init()
        set.starlev.features.combat.dungeons.solvers.Waterboard.init()
        set.starlev.features.combat.dungeons.LeapOverlay.init()
        set.starlev.features.combat.dungeons.DeathCounter.init()
        set.starlev.features.combat.dungeons.ScoreCounter.init()
        set.starlev.features.combat.dungeons.Floor4.init()
        set.starlev.secret.features.Povtorayshkins.init()
        set.starlev.utils.DiscordRPC.init()

        // Инициализация JS движка и загрузка модулей
        set.starlev.scripting.SHScriptEngine.init()
        set.starlev.scripting.ModuleLoader.loadAll()

        // Регистрация сохранения при выходе из игры.
        // Порядок важен: сначала JS-модули (сохраняют свой storage),
        // затем геометрия HUD, затем глобальный конфиг.
        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            set.starlev.scripting.SHScriptEngine.shutdown()
            set.starlev.utils.DiscordRPC.shutdown()
            set.starlev.render.HudManager.saveAllLayouts()
            configManager.saveConfig("client-stopping")
            configManager.cancelAutoSave()
            // Прибиваем оставшиеся не-daemon Timer'ы (Watchdog 15с — Timer-1 / HttpClient)
            try {
                Thread.getAllStackTraces().keys.forEach { t ->
                    if (t.name.startsWith("Timer-") && !t.isDaemon) {
                        try { t.interrupt() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            configManager.saveConfig("shutdown-hook")
        })

        // Единственная запись в лог от мода: всё поднято, можно работать.
        LOGGER.info("StarredHeltix v$modVersion инициализирован.")
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("StarredHeltix")
        lateinit var configManager: ConfigManager

        /** Версия мода из Fabric метаданных (fallback — из gradle.properties). */
        @JvmStatic
        val modVersion: String by lazy {
            try {
                net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("starredheltix")
                    .map { it.metadata.version.friendlyString }
                    .orElse("1.0.0")
            } catch (e: Exception) {
                "1.0.0"
            }
        }

        @JvmStatic
        val feature: Features get() = configManager.features

        var screenToOpen: Screen? = null
    }
}
