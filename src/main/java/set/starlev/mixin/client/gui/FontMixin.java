package set.starlev.mixin.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.utils.CacheManager;

import java.util.List;

@Mixin(Font.class)
public class FontMixin {

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void onWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null || text.isEmpty()) return;
        
        Integer cached = CacheManager.INSTANCE.getCachedTextWidth(text);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("RETURN"))
    private void onWidthReturn(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null || text.isEmpty()) return;
        CacheManager.INSTANCE.cacheTextWidth(text, cir.getReturnValue());
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void onWidthFormatted(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        if (text == null) return;
        String string = text.getString();
        if (string.isEmpty()) return;

        Integer cached = CacheManager.INSTANCE.getCachedTextWidth(string);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("RETURN"))
    private void onWidthFormattedReturn(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        if (text == null) return;
        String string = text.getString();
        if (string.isEmpty()) return;
        CacheManager.INSTANCE.cacheTextWidth(string, cir.getReturnValue());
    }

    @Inject(method = "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void onSplit(FormattedText text, int maxWidth, CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
        if (text == null) return;
        String string = text.getString();
        List<FormattedCharSequence> cached = CacheManager.INSTANCE.getCachedLayout(string, maxWidth);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", at = @At("RETURN"))
    private void onSplitReturn(FormattedText text, int maxWidth, CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
        if (text == null) return;
        CacheManager.INSTANCE.cacheLayout(text.getString(), maxWidth, cir.getReturnValue());
    }
}
