package set.starlev.starredheltix.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent VOTING_REMINDER = registerSoundEvent("voting_reminder");
    public static final SoundEvent UPDATE_AVAILABLE = registerSoundEvent("update_available");
    
    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of("starredheltix", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    
    public static void registerSounds() {
        // Sounds are registered when the class is loaded
    }
}