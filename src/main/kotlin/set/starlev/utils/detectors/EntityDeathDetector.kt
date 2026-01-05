package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.animal.wolf.Wolf

object EntityDeathDetector {
    private val listeners = mutableListOf<(Entity) -> Unit>()

    fun registerListener(listener: (Entity) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Вызывается из LivingEntityMixin при смерти сущности
     */
    fun onEntityDeath(entity: Entity) {
        // Мы отслеживаем смерти только вблизи игрока (в радиусе прорисовки, т.к. Mixin работает на клиенте)
        listeners.forEach { it(entity) }
    }

    /**
     * Проверяет, является ли сущность подходящей для конкретного типа Слеера
     */
    fun isRelevantForSlayer(entity: Entity, slayerType: String): Boolean {
        return when (slayerType.lowercase()) {
            "мститель", "revenant" -> entity is Zombie
            "тарантула", "tarantula" -> entity is Spider || entity is CaveSpider
            "свен", "sven" -> entity is Wolf
            else -> false
        }
    }
}
