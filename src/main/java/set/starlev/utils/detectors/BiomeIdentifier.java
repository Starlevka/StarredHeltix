package set.starlev.utils.detectors;

import net.minecraft.core.BlockPos;

/**
 * Интерфейс для определения биома через Mixin.
 */
public interface BiomeIdentifier {
    /**
     * Получить ID биома по координатам.
     * @param pos Позиция
     * @return ID биома (например, "minecraft:plains")
     */
    String starlev$getBiomeId(BlockPos pos);
}
