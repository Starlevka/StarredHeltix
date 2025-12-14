package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object BiomeDetector {

    /**
     * Получить биом, в котором находится игрок
     * @return Имя текущего биома
     */
    fun getPlayerBiome(): String {
        val client = Minecraft.getInstance()
        val player = client.player ?: return "Неизвестный"
        val level = client.level ?: return "Неизвестный"
        
        return try {
            val biomeHolder = level.getBiome(player.blockPosition())
            val key = biomeHolder.unwrapKey()
            
            if (key.isPresent) {
                val id = key.get().location()
                // Пробуем получить переведенное имя
                val translatable = Component.translatable("biome.${id.namespace}.${id.path}")
                translatable.string
            } else {
                "Неизвестный"
            }
        } catch (e: Exception) {
            "Неизвестный"
        }
    }
}
