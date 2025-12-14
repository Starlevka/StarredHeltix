package set.starlev

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.gui.screens.Screen
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import set.starlev.render.RenderEngine
import set.starlev.config.ConfigManager
import set.starlev.config.Features
import set.starlev.features.combat.solvers.dungeons.ThreeWeirdosSolverObj
import set.starlev.features.misc.WelcomeMessage

class StarredHeltix : ClientModInitializer {
    override fun onInitializeClient() {
        LOGGER.info("Инициализация ЛлЛлЛлЛлЛл StarredHeltix...")
        configManager = ConfigManager
        configManager.firstLoad()

        ModSounds.register()

        set.starlev.commands.ConfigCommand.register()
        set.starlev.events.KeyBindHandler.init()
        set.starlev.features.combat.EntityHighlight.init()
        set.starlev.features.chat.PartyCommands.init()
        set.starlev.features.misc.VotingReminder.init()
        set.starlev.features.misc.CustomBindManager.init()
        WelcomeMessage.register()

        // Register chat listeners
        set.starlev.features.chat.ChatEventsManager.registerIncoming { message ->
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
        set.starlev.features.combat.solvers.dungeons.BloodRoomTimer.register()
        ThreeWeirdosSolverObj.register()

        // Регистрация сохранения конфига при выходе из игры
        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            configManager.saveConfig("client-stopping")
            set.starlev.hud.HudManager.saveAllLayouts()
        }

        LOGGER.info("StarredHeltix инициализирован. Chat Copy Feature готов к использованию.")

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
