package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.Optional;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

public record JukeboxPlayablePredicate(Optional<HolderSet<JukeboxSong>> song) implements SingleComponentItemPredicate<JukeboxPlayable> {
   public static final Codec<JukeboxPlayablePredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(RegistryCodecs.homogeneousList(Registries.JUKEBOX_SONG).optionalFieldOf("song").forGetter(JukeboxPlayablePredicate::song)).apply(var0, JukeboxPlayablePredicate::new);
   });

   public JukeboxPlayablePredicate(Optional<HolderSet<JukeboxSong>> param1) {
      super();
      this.song = var1;
   }

   public DataComponentType<JukeboxPlayable> componentType() {
      return DataComponents.JUKEBOX_PLAYABLE;
   }

   public boolean matches(JukeboxPlayable var1) {
      if (!this.song.isPresent()) {
         return true;
      } else {
         boolean var2 = false;
         Iterator var3 = ((HolderSet)this.song.get()).iterator();

         while(var3.hasNext()) {
            Holder var4 = (Holder)var3.next();
            Optional var5 = var4.unwrapKey();
            if (!var5.isEmpty() && var5.equals(var1.song().key())) {
               var2 = true;
               break;
            }
         }

         return var2;
      }
   }

   public static JukeboxPlayablePredicate any() {
      return new JukeboxPlayablePredicate(Optional.empty());
   }

   public Optional<HolderSet<JukeboxSong>> song() {
      return this.song;
   }
}
