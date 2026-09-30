package set.starlev.events

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import set.starlev.StarredHeltix
import set.starlev.features.mining.EndHighlight
import set.starlev.features.combat.EntityHighlight
import set.starlev.features.combat.dungeons.Floor4
import set.starlev.utils.CacheManager
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.EntityNameCache
import set.starlev.utils.detectors.LocationDetector

/**
 * Централизованный сброс кэшей и состояния детекторов при смене мира/сервера.
 * Предотвращает утечки памяти и показывает устаревшие данные из прошлого лобби.
 */
object WorldChangeHandler {

    /** Каждый сброс изолирован: исключение в одном не должно обрывать остальные. */
    private fun safe(name: String, block: () -> Unit) {
        try { block() } catch (e: Exception) {
            StarredHeltix.LOGGER.error("[WorldChange] Сброс '$name' падает: ${e.message}")
        }
    }

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            resetAll("join")
            // Мут переживает реконнект: обнуляется только при переходе на ДРУГОЙ сервер.
            safe("ModerationManager/join") {
                set.starlev.features.chat.mod.ModerationManager.onJoin(client.currentServer?.ip)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            resetAll("disconnect")
            safe("ModerationManager/disconnect") {
                set.starlev.features.chat.mod.ModerationManager.onDisconnect()
            }
        }
    }

    @JvmStatic
    fun resetAll(reason: String) {
        safe("LocationDetector") { LocationDetector.resetCache() }
        safe("DungeonDetector") { DungeonDetector.resetCache() }
        safe("GardenDetector") { set.starlev.features.farming.garden.GardenDetector.resetCache() }
        safe("CacheManager") { CacheManager.clearAll() }
        safe("EntityNameCache") { EntityNameCache.clear() }
        safe("EntityHighlight") { EntityHighlight.onWorldChange() }
        safe("ClickHighlight") { set.starlev.features.combat.ClickHighlight.onWorldChange() }
        safe("ModuleGlowRegistry") { set.starlev.scripting.ModuleGlowRegistry.onWorldChange() }
        safe("Floor4") { Floor4.onWorldChange() }
        safe("GreenWalls") { set.starlev.features.combat.dungeons.GreenWalls.onWorldChange() }
        safe("BloodRoomTimer") { set.starlev.features.combat.dungeons.BloodRoomTimer.reset() }
        safe("EndHighlight") { EndHighlight.clearCache() }
        safe("GlowingMushroomHighlight") { set.starlev.features.fishing.GlowingMushroomHighlight.onWorldChange() }
        safe("NpcDialogueOverlay") { set.starlev.features.skyblock.NpcDialogueOverlay.clearOnServerChange() }
        // Макро-чек гасим всегда. Мут НЕ трогаем здесь: он живёт в рамках сервера и должен
        // переживать реконнект (иначе бан слетает при перезаходе) — чистит его ModerationManager.onJoin.
        safe("MacroCheck") { set.starlev.features.chat.mod.MacroCheck.onWorldChange() }
        safe("SkillXpDetector") { set.starlev.utils.detectors.SkillXpDetector.onWorldChange() }
        safe("SlayerHud") { set.starlev.features.combat.slayer.SlayerHud.onWorldChange() }
        safe("PetOverlay") { set.starlev.features.skyblock.PetOverlay.onWorldChange() }
        safe("Museum") { set.starlev.features.skyblock.Museum.clear() }
        safe("CommissionsHud") { set.starlev.features.mining.CommissionsHud.onWorldChange() }
        safe("PlotsHud") { set.starlev.features.farming.garden.GardenPlotsOverlay.onWorldChange() }
        // Солверы данжей.
        safe("ThreeWeirdos") { set.starlev.features.combat.dungeons.solvers.ThreeWeirdos.fullReset() }
        safe("ThreeWeirdosChest") { set.starlev.features.combat.dungeons.solvers.ThreeWeirdosChest.reset() }
        safe("Waterboard") { set.starlev.features.combat.dungeons.solvers.Waterboard.reset() }
        safe("CreeperBeams") { set.starlev.features.combat.dungeons.solvers.CreeperBeams.onWorldChange() }
        safe("TicTacToe") { set.starlev.features.combat.dungeons.solvers.TicTacToe.onWorldChange() }
        safe("LeapOverlay") { set.starlev.features.combat.dungeons.LeapOverlay.onWorldChange() }
        StarredHeltix.LOGGER.info("[WorldChange] Кэши сброшены (reason=$reason)")
    }
}
