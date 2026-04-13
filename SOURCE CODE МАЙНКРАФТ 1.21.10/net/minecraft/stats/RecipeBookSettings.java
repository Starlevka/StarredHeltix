package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
   public static final StreamCodec<FriendlyByteBuf, RecipeBookSettings> STREAM_CODEC;
   public static final MapCodec<RecipeBookSettings> MAP_CODEC;
   private RecipeBookSettings.TypeSettings crafting;
   private RecipeBookSettings.TypeSettings furnace;
   private RecipeBookSettings.TypeSettings blastFurnace;
   private RecipeBookSettings.TypeSettings smoker;

   public RecipeBookSettings() {
      this(RecipeBookSettings.TypeSettings.DEFAULT, RecipeBookSettings.TypeSettings.DEFAULT, RecipeBookSettings.TypeSettings.DEFAULT, RecipeBookSettings.TypeSettings.DEFAULT);
   }

   private RecipeBookSettings(RecipeBookSettings.TypeSettings var1, RecipeBookSettings.TypeSettings var2, RecipeBookSettings.TypeSettings var3, RecipeBookSettings.TypeSettings var4) {
      super();
      this.crafting = var1;
      this.furnace = var2;
      this.blastFurnace = var3;
      this.smoker = var4;
   }

   @VisibleForTesting
   public RecipeBookSettings.TypeSettings getSettings(RecipeBookType var1) {
      RecipeBookSettings.TypeSettings var10000;
      switch(var1) {
      case CRAFTING:
         var10000 = this.crafting;
         break;
      case FURNACE:
         var10000 = this.furnace;
         break;
      case BLAST_FURNACE:
         var10000 = this.blastFurnace;
         break;
      case SMOKER:
         var10000 = this.smoker;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private void updateSettings(RecipeBookType var1, UnaryOperator<RecipeBookSettings.TypeSettings> var2) {
      switch(var1) {
      case CRAFTING:
         this.crafting = (RecipeBookSettings.TypeSettings)var2.apply(this.crafting);
         break;
      case FURNACE:
         this.furnace = (RecipeBookSettings.TypeSettings)var2.apply(this.furnace);
         break;
      case BLAST_FURNACE:
         this.blastFurnace = (RecipeBookSettings.TypeSettings)var2.apply(this.blastFurnace);
         break;
      case SMOKER:
         this.smoker = (RecipeBookSettings.TypeSettings)var2.apply(this.smoker);
      }

   }

   public boolean isOpen(RecipeBookType var1) {
      return this.getSettings(var1).open;
   }

   public void setOpen(RecipeBookType var1, boolean var2) {
      this.updateSettings(var1, (var1x) -> {
         return var1x.setOpen(var2);
      });
   }

   public boolean isFiltering(RecipeBookType var1) {
      return this.getSettings(var1).filtering;
   }

   public void setFiltering(RecipeBookType var1, boolean var2) {
      this.updateSettings(var1, (var1x) -> {
         return var1x.setFiltering(var2);
      });
   }

   public RecipeBookSettings copy() {
      return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker);
   }

   public void replaceFrom(RecipeBookSettings var1) {
      this.crafting = var1.crafting;
      this.furnace = var1.furnace;
      this.blastFurnace = var1.blastFurnace;
      this.smoker = var1.smoker;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(RecipeBookSettings.TypeSettings.STREAM_CODEC, (var0) -> {
         return var0.crafting;
      }, RecipeBookSettings.TypeSettings.STREAM_CODEC, (var0) -> {
         return var0.furnace;
      }, RecipeBookSettings.TypeSettings.STREAM_CODEC, (var0) -> {
         return var0.blastFurnace;
      }, RecipeBookSettings.TypeSettings.STREAM_CODEC, (var0) -> {
         return var0.smoker;
      }, RecipeBookSettings::new);
      MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(RecipeBookSettings.TypeSettings.CRAFTING_MAP_CODEC.forGetter((var0x) -> {
            return var0x.crafting;
         }), RecipeBookSettings.TypeSettings.FURNACE_MAP_CODEC.forGetter((var0x) -> {
            return var0x.furnace;
         }), RecipeBookSettings.TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter((var0x) -> {
            return var0x.blastFurnace;
         }), RecipeBookSettings.TypeSettings.SMOKER_MAP_CODEC.forGetter((var0x) -> {
            return var0x.smoker;
         })).apply(var0, RecipeBookSettings::new);
      });
   }

   public static record TypeSettings(boolean open, boolean filtering) {
      final boolean open;
      final boolean filtering;
      public static final RecipeBookSettings.TypeSettings DEFAULT = new RecipeBookSettings.TypeSettings(false, false);
      public static final MapCodec<RecipeBookSettings.TypeSettings> CRAFTING_MAP_CODEC = codec("isGuiOpen", "isFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> FURNACE_MAP_CODEC = codec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> BLAST_FURNACE_MAP_CODEC = codec("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> SMOKER_MAP_CODEC = codec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
      public static final StreamCodec<ByteBuf, RecipeBookSettings.TypeSettings> STREAM_CODEC;

      public TypeSettings(boolean param1, boolean param2) {
         super();
         this.open = var1;
         this.filtering = var2;
      }

      public String toString() {
         return "[open=" + this.open + ", filtering=" + this.filtering + "]";
      }

      public RecipeBookSettings.TypeSettings setOpen(boolean var1) {
         return new RecipeBookSettings.TypeSettings(var1, this.filtering);
      }

      public RecipeBookSettings.TypeSettings setFiltering(boolean var1) {
         return new RecipeBookSettings.TypeSettings(this.open, var1);
      }

      private static MapCodec<RecipeBookSettings.TypeSettings> codec(String var0, String var1) {
         return RecordCodecBuilder.mapCodec((var2) -> {
            return var2.group(Codec.BOOL.optionalFieldOf(var0, false).forGetter(RecipeBookSettings.TypeSettings::open), Codec.BOOL.optionalFieldOf(var1, false).forGetter(RecipeBookSettings.TypeSettings::filtering)).apply(var2, RecipeBookSettings.TypeSettings::new);
         });
      }

      public boolean open() {
         return this.open;
      }

      public boolean filtering() {
         return this.filtering;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, RecipeBookSettings.TypeSettings::open, ByteBufCodecs.BOOL, RecipeBookSettings.TypeSettings::filtering, RecipeBookSettings.TypeSettings::new);
      }
   }
}
