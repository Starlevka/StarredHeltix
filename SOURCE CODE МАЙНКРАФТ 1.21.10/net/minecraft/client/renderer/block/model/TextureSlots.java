package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class TextureSlots {
   public static final TextureSlots EMPTY = new TextureSlots(Map.of());
   private static final char REFERENCE_CHAR = '#';
   private final Map<String, Material> resolvedValues;

   TextureSlots(Map<String, Material> var1) {
      super();
      this.resolvedValues = var1;
   }

   @Nullable
   public Material getMaterial(String var1) {
      if (isTextureReference(var1)) {
         var1 = var1.substring(1);
      }

      return (Material)this.resolvedValues.get(var1);
   }

   private static boolean isTextureReference(String var0) {
      return var0.charAt(0) == '#';
   }

   public static TextureSlots.Data parseTextureMap(JsonObject var0, ResourceLocation var1) {
      TextureSlots.Data.Builder var2 = new TextureSlots.Data.Builder();
      Iterator var3 = var0.entrySet().iterator();

      while(var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         parseEntry(var1, (String)var4.getKey(), ((JsonElement)var4.getValue()).getAsString(), var2);
      }

      return var2.build();
   }

   private static void parseEntry(ResourceLocation var0, String var1, String var2, TextureSlots.Data.Builder var3) {
      if (isTextureReference(var2)) {
         var3.addReference(var1, var2.substring(1));
      } else {
         ResourceLocation var4 = ResourceLocation.tryParse(var2);
         if (var4 == null) {
            throw new JsonParseException(var2 + " is not valid resource location");
         }

         var3.addTexture(var1, new Material(var0, var4));
      }

   }

   public static record Data(Map<String, TextureSlots.SlotContents> values) {
      final Map<String, TextureSlots.SlotContents> values;
      public static final TextureSlots.Data EMPTY = new TextureSlots.Data(Map.of());

      public Data(Map<String, TextureSlots.SlotContents> param1) {
         super();
         this.values = var1;
      }

      public Map<String, TextureSlots.SlotContents> values() {
         return this.values;
      }

      public static class Builder {
         private final Map<String, TextureSlots.SlotContents> textureMap = new HashMap();

         public Builder() {
            super();
         }

         public TextureSlots.Data.Builder addReference(String var1, String var2) {
            this.textureMap.put(var1, new TextureSlots.Reference(var2));
            return this;
         }

         public TextureSlots.Data.Builder addTexture(String var1, Material var2) {
            this.textureMap.put(var1, new TextureSlots.Value(var2));
            return this;
         }

         public TextureSlots.Data build() {
            return this.textureMap.isEmpty() ? TextureSlots.Data.EMPTY : new TextureSlots.Data(Map.copyOf(this.textureMap));
         }
      }
   }

   public static class Resolver {
      private static final Logger LOGGER = LogUtils.getLogger();
      private final List<TextureSlots.Data> entries = new ArrayList();

      public Resolver() {
         super();
      }

      public TextureSlots.Resolver addLast(TextureSlots.Data var1) {
         this.entries.addLast(var1);
         return this;
      }

      public TextureSlots.Resolver addFirst(TextureSlots.Data var1) {
         this.entries.addFirst(var1);
         return this;
      }

      public TextureSlots resolve(ModelDebugName var1) {
         if (this.entries.isEmpty()) {
            return TextureSlots.EMPTY;
         } else {
            Object2ObjectArrayMap var2 = new Object2ObjectArrayMap();
            Object2ObjectArrayMap var3 = new Object2ObjectArrayMap();
            Iterator var4 = Lists.reverse(this.entries).iterator();

            while(var4.hasNext()) {
               TextureSlots.Data var5 = (TextureSlots.Data)var4.next();
               var5.values.forEach((var2x, var3x) -> {
                  Objects.requireNonNull(var3x);
                  byte var5 = 0;
                  switch(var3x.typeSwitch<invokedynamic>(var3x, var5)) {
                  case 0:
                     TextureSlots.Value var6 = (TextureSlots.Value)var3x;
                     var3.remove(var2x);
                     var2.put(var2x, var6.material());
                     break;
                  case 1:
                     TextureSlots.Reference var7 = (TextureSlots.Reference)var3x;
                     var2.remove(var2x);
                     var3.put(var2x, var7);
                     break;
                  default:
                     throw new MatchException((String)null, (Throwable)null);
                  }

               });
            }

            if (var3.isEmpty()) {
               return new TextureSlots(var2);
            } else {
               boolean var8 = true;

               while(var8) {
                  var8 = false;
                  ObjectIterator var9 = Object2ObjectMaps.fastIterator(var3);

                  while(var9.hasNext()) {
                     it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry var6 = (it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry)var9.next();
                     Material var7 = (Material)var2.get(((TextureSlots.Reference)var6.getValue()).target);
                     if (var7 != null) {
                        var2.put((String)var6.getKey(), var7);
                        var9.remove();
                        var8 = true;
                     }
                  }
               }

               if (!var3.isEmpty()) {
                  LOGGER.warn("Unresolved texture references in {}:\n{}", var1.debugName(), var3.entrySet().stream().map((var0) -> {
                     String var10000 = (String)var0.getKey();
                     return "\t#" + var10000 + "-> #" + ((TextureSlots.Reference)var0.getValue()).target + "\n";
                  }).collect(Collectors.joining()));
               }

               return new TextureSlots(var2);
            }
         }
      }
   }

   static record Reference(String target) implements TextureSlots.SlotContents {
      final String target;

      Reference(String param1) {
         super();
         this.target = var1;
      }

      public String target() {
         return this.target;
      }
   }

   static record Value(Material material) implements TextureSlots.SlotContents {
      Value(Material param1) {
         super();
         this.material = var1;
      }

      public Material material() {
         return this.material;
      }
   }

   public interface SlotContents {
   }
}
