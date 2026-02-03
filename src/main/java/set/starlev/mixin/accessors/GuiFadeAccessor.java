package set.starlev.mixin.accessors;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiFadeAccessor {
    @Accessor("titleFadeInTime")
    void setTitleFadeInTime(int time);

    @Accessor("titleFadeOutTime")
    void setTitleFadeOutTime(int time);
    
    @Accessor("titleStayTime")
    void setTitleStayTime(int time);
}
