package set.starlev.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    
    @ModifyVariable(
        method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component onDrawString(Component component) {
        if (component == null) return null;
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return component;
        
        // В GuiGraphics эффекты применяются только если они форсированы точечно в коде мода
        return SecretFunFeatures.processComponent(component, false);
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
        // В GuiGraphics эффекты применяются только если они форсированы точечно в коде мода
        Component processed = SecretFunFeatures.processComponent(component, false);
        
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
        for (net.minecraft.network.chat.Component line : lines) {
            // В GuiGraphics эффекты применяются только если они форсированы точечно в коде мода
            processed.add(SecretFunFeatures.processComponent(line, false));
        }
        return processed;
    }
}
