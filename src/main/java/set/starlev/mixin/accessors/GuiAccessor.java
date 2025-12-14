package set.starlev.mixin.accessors;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("overlayMessageString")
    net.minecraft.network.chat.Component getOverlayMessageString();

    @Accessor("toolHighlightTimer")
    int getToolHighlightTimer();

    @Accessor("lastToolHighlight")
    ItemStack getLastToolHighlight();

    @Accessor("title")
    net.minecraft.network.chat.Component getTitle();

    @Accessor("subtitle")
    net.minecraft.network.chat.Component getSubtitle();

    @Accessor("titleTime")
    int getTitleTime();
}
