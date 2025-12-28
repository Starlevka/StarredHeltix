package set.starlev.mixin.accessors;

import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextColor.class)
public interface TextColorAccessor {
    @Accessor("name")
    String getName();

    @Accessor("value")
    int getValue();
}
