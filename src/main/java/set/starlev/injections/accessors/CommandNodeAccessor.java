package set.starlev.injections.accessors;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * @author Starlev
 * @reason Ð”Ð¾ÑÑ‚ÑƒÐ¿ Ðº Ð¿Ñ€Ð¸Ð²Ð°Ñ‚Ð½Ñ‹Ð¼ ÐºÐ°Ñ€Ñ‚Ð°Ð¼ CommandNode Ð´Ð»Ñ Ð´Ð¸Ð½Ð°Ð¼Ð¸Ñ‡ÐµÑÐºÐ¾Ð³Ð¾ ÑÐ½ÑÑ‚Ð¸Ñ
 *         ÐºÐ¾Ð¼Ð°Ð½Ð´ JS-Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹ Ñ ÐºÐ»Ð¸ÐµÐ½Ñ‚ÑÐºÐ¾Ð³Ð¾ dispatcher (Brigadier Ð½Ðµ Ð¸Ð¼ÐµÐµÑ‚
 *         Ð¿ÑƒÐ±Ð»Ð¸Ñ‡Ð½Ð¾Ð³Ð¾ API ÑƒÐ´Ð°Ð»ÐµÐ½Ð¸Ñ Ð´Ð¾Ñ‡ÐµÑ€Ð½Ð¸Ñ… ÑƒÐ·Ð»Ð¾Ð²).
 */
@Mixin(CommandNode.class)
public interface CommandNodeAccessor<S> {
    @Accessor("children")
    Map<String, CommandNode<S>> shGetChildren();

    @Accessor("literals")
    Map<String, LiteralCommandNode<S>> shGetLiterals();
}

