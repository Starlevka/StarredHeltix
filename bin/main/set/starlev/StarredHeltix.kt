package set.starlev

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object StarredHeltix : ModInitializer {
    private val logger = LoggerFactory.getLogger("starredheltix")

	override fun onInitialize() {
		logger.info("Запуск StarredHeltix...")
	}
}