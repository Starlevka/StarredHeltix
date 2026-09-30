package set.starlev.duck;

import net.minecraft.core.BlockPos;

/**
 * Duck-интерфейс для определения биома через Mixin.
 * <p>
 * ВАЖНО: должен находиться ВНЕ mixin-пакета ({@code set.starlev.injections.*}),
 * потому что Mixin запрещает прямую загрузку классов из mixin-пакета,
 * не зарегистрированных в конфиге миксинов (IllegalClassLoadError).
 * Реализуется в {@code ClientLevelMixin}.
 */
public interface BiomeIdentifier {
    /**
     * Получить ID биома по координатам.
     * @param pos Позиция
     * @return ID биома (например, "minecraft:plains")
     */
    String starlev$getBiomeId(BlockPos pos);
}