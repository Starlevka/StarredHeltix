package set.starlev.mixin.accessors;

import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TODO: Реализовать accessor для Scoreboard в 1.21.10
 */
@Mixin(Scoreboard.class)
public interface ScoreboardAccessor {
    // ScoreboardEntry не существует в 1.21.10
}
