package set.starlev.injections.accessors;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("commands")
    CommandDispatcher<SharedSuggestionProvider> getCommands();

    @Accessor("suggestionsProvider")
    ClientSuggestionProvider getSuggestionsProvider();
}

