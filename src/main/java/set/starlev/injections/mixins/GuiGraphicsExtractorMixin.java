package set.starlev.injections.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
    private static boolean starredheltix$isSecretMenuOpen() {
        try {
            net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().gui.screen();
            if (screen == null) return false;
            return screen.getClass().getName().equals("io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent");
        } catch (Exception e) {
            return false;
        }
    }

    @ModifyVariable(
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component onTextComponent(Component component) {
        if (component == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return component;

        boolean force = starredheltix$isSecretMenuOpen();
        return SecretFunFeatures.processComponent(component, force);
    }

    @ModifyVariable(
        method = "centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component onCenteredTextComponent(Component component) {
        if (component == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return component;

        boolean force = starredheltix$isSecretMenuOpen();
        return SecretFunFeatures.processComponent(component, force);
    }

    @ModifyVariable(
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String onTextString(String text) {
        if (text == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return text;

        Component component = Component.literal(text);
        boolean force = starredheltix$isSecretMenuOpen();
        Component processed = SecretFunFeatures.processComponent(component, force);

        if (processed != component) {
            return set.starlev.utils.detectors.TabListDetector.componentToFormattedString(processed);
        }

        return text;
    }

    @ModifyVariable(
        method = "centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String onCenteredTextString(String text) {
        if (text == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return text;

        Component component = Component.literal(text);
        boolean force = starredheltix$isSecretMenuOpen();
        Component processed = SecretFunFeatures.processComponent(component, force);

        if (processed != component) {
            return set.starlev.utils.detectors.TabListDetector.componentToFormattedString(processed);
        }

        return text;
    }
}
