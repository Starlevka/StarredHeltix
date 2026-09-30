package set.starlev.injections.accessors;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Ð”Ð¾Ð±Ð°Ð²Ð»ÐµÐ½Ð¸Ðµ Ð²Ð¸Ð´Ð¶ÐµÑ‚Ð¾Ð² Ð² Ð²Ð°Ð½Ð¸Ð»ÑŒÐ½Ñ‹Ðµ ÑÐºÑ€Ð°Ð½Ñ‹ (ÐºÐ½Ð¾Ð¿ÐºÐ° Â«Ð¥ÐµÐ»Ñ‚Ð¸ÐºÑÂ» Ð² Ð³Ð»Ð°Ð²Ð½Ð¾Ð¼
 * Ð¼ÐµÐ½ÑŽ): addRenderableWidget Ð·Ð°Ñ‰Ð¸Ñ‰Ñ‘Ð½, Ð¿ÑƒÐ±Ð»Ð¸Ñ‡Ð½Ð¾Ð³Ð¾ Ð°Ð½Ð°Ð»Ð¾Ð³Ð° Ð½ÐµÑ‚.
 * Ð¡Ð¸Ð³Ð½Ð°Ñ‚ÑƒÑ€Ð° Ð¾Ð±ÑŠÑÐ²Ð»ÐµÐ½Ð° Ð² ÑÑ‚Ñ‘Ñ€Ñ‚Ð¾Ð¼ Ð²Ð¸Ð´Ðµ (GuiEventListener), ÐºÐ°Ðº Ñƒ Ñ†ÐµÐ»Ð¸.
 */
@Mixin(Screen.class)
public interface ScreenAccessor {

    @Invoker("addRenderableWidget")
    GuiEventListener starredheltix$addRenderableWidget(GuiEventListener widget);
}

