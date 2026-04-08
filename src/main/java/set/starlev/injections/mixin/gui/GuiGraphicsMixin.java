package set.starlev.injections.mixin.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    private static boolean starredheltix$isSecretMenuOpen() {
        try {
            net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return false;
            return screen.getClass().getName().equals("io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent");
        } catch (Exception e) {
            return false;
        }
    }
    
    @ModifyVariable(
        method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component onDrawString(Component component) {
        if (component == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return component;
        
        boolean force = starredheltix$isSecretMenuOpen();
        return SecretFunFeatures.processComponent(component, force);
    }

    @ModifyVariable(
        method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component onDrawCenteredString(Component component) {
        if (component == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return component;
        
        boolean force = starredheltix$isSecretMenuOpen();
        return SecretFunFeatures.processComponent(component, force);
    }

    @ModifyVariable(
        method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String onDrawString(String text) {
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
        method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String onDrawCenteredString(String text) {
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
        method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private java.util.List<net.minecraft.network.chat.Component> onRenderTooltip(java.util.List<net.minecraft.network.chat.Component> lines) {
        if (lines == null || !SecretMenuManager.INSTANCE.isConfigInitialized()) return lines;
        
        java.util.List<net.minecraft.network.chat.Component> processed = new java.util.ArrayList<>(lines.size());
        boolean force = starredheltix$isSecretMenuOpen();
        for (net.minecraft.network.chat.Component line : lines) {
            processed.add(SecretFunFeatures.processComponent(line, force));
        }
        return processed;
    }
}
