package set.starlev.injections.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.injections.accessors.ScreenAccessor;

/**
 * @author Starlev
 * Кнопка «Хелтикс» в главном меню — мгновенное подключение к
 * starlev.heltix.net, минуя список серверов. Иконка 20x20 располагается
 * справа от кнопки «Сетевая игра» (как TW-кнопка в dev-сборках).
 * Адрес захардкожен — подмена на произвольный сервер невозможна.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private static final String SH_SERVER_ADDRESS = "starlev.heltix.net";

    @Unique
    private static final int SH_ICON_BUTTON_SIZE = 20;

    @Inject(method = "init()V", at = @At("RETURN"))
    private void starredheltix$addHeltixButton(CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;

        // Находим ванильную кнопку «Сетевая игра»
        AbstractWidget multiplayer = null;
        for (GuiEventListener child : self.children()) {
            if (child instanceof AbstractWidget widget && "menu.multiplayer".equals(translationKeyOf(widget))) {
                multiplayer = widget;
                break;
            }
        }
        if (multiplayer == null) {
            return;
        }

        ScreenAccessor accessor = (ScreenAccessor) self;
        accessor.starredheltix$addRenderableWidget(new HeltixButton(
            multiplayer.getX() + multiplayer.getWidth() + 2, multiplayer.getY()));
    }

    @Unique
    private static void connectToHeltix(Minecraft minecraft) {
        if (minecraft.level != null) {
            // Экран в игре недостижим; на всякий случай не рвём активное соединение
            return;
        }

        // Соединяемся через сохранённую запись сервера: выбор по ресурспаку
        // (ClientCommonPacketListenerImpl) пишется в экземпляр ServerData,
        // поэтому с одноразовым new ServerData() запрос повторялся каждый раз.
        // В 26.2 список больше не живёт в Minecraft — грузим ServerList вручную.
        ServerList list = new ServerList(minecraft);
        list.load();
        ServerData serverData = list.get(SH_SERVER_ADDRESS);
        if (serverData == null) {
            serverData = new ServerData("Heltix", SH_SERVER_ADDRESS, ServerData.Type.OTHER);
            // Свой захардкоженный сервер — один раз молча принимаем пак,
            // принудительные паки скачиваются независимо от этого флага.
            serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            list.add(serverData, false);
        }
        // saveSingleServer умеет только ЗАМЕНЯТЬ существующие записи (set() по name+ip),
        // поэтому сохраняем весь список — новый сервер попадёт в servers.dat.
        list.save();
        ServerAddress address = ServerAddress.parseString(SH_SERVER_ADDRESS);
        ConnectScreen.startConnecting(new TitleScreen(), minecraft, address, serverData, false, null);
    }

    @Unique
    private static String translationKeyOf(AbstractWidget widget) {
        if (widget.getMessage().getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }

    /**
     * Кнопка с иконкой Heltix: ванильный фон кнопки + текстура по центру.
     */
    @Unique
    private static class HeltixButton extends Button {

        private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("starredheltix", "textures/heltix.png");

        private HeltixButton(int x, int y) {
            super(x, y, SH_ICON_BUTTON_SIZE, SH_ICON_BUTTON_SIZE,
                Component.translatable("starredheltix.menu.heltix"),
                (button) -> connectToHeltix(Minecraft.getInstance()),
                Button.DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("starredheltix.menu.heltix.tooltip")));
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            // Полная текстура 64x64, вписанная в кнопку с отступом 2px
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.getX() + 2, this.getY() + 2,
                0.0F, 0.0F,
                this.getWidth() - 4, this.getHeight() - 4,
                64, 64, 64, 64);
        }
    }
}
