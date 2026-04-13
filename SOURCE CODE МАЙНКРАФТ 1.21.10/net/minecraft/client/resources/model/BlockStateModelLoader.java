package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.slf4j.Logger;

public class BlockStateModelLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");

   public BlockStateModelLoader() {
      super();
   }

   public static CompletableFuture<BlockStateModelLoader.LoadedModels> loadBlockStates(ResourceManager var0, Executor var1) {
      Function var2 = BlockStateDefinitions.definitionLocationToBlockStateMapper();
      return CompletableFuture.supplyAsync(() -> {
         return BLOCKSTATE_LISTER.listMatchingResourceStacks(var0);
      }, var1).thenCompose((var2x) -> {
         ArrayList var3 = new ArrayList(var2x.size());
         Iterator var4 = var2x.entrySet().iterator();

         while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            var3.add(CompletableFuture.supplyAsync(() -> {
               ResourceLocation var2x = BLOCKSTATE_LISTER.fileToId((ResourceLocation)var5.getKey());
               StateDefinition var3 = (StateDefinition)var2.apply(var2x);
               if (var3 == null) {
                  LOGGER.debug("Discovered unknown block state definition {}, ignoring", var2x);
                  return null;
               } else {
                  List var4 = (List)var5.getValue();
                  ArrayList var5x = new ArrayList(var4.size());
                  Iterator var6 = var4.iterator();

                  while(var6.hasNext()) {
                     Resource var7 = (Resource)var6.next();

                     try {
                        BufferedReader var8 = var7.openAsReader();

                        try {
                           JsonElement var9 = StrictJsonParser.parse((Reader)var8);
                           BlockModelDefinition var10 = (BlockModelDefinition)BlockModelDefinition.CODEC.parse(JsonOps.INSTANCE, var9).getOrThrow(JsonParseException::new);
                           var5x.add(new BlockStateModelLoader.LoadedBlockModelDefinition(var7.sourcePackId(), var10));
                        } catch (Throwable var13) {
                           if (var8 != null) {
                              try {
                                 var8.close();
                              } catch (Throwable var12) {
                                 var13.addSuppressed(var12);
                              }
                           }

                           throw var13;
                        }

                        if (var8 != null) {
                           var8.close();
                        }
                     } catch (Exception var14) {
                        LOGGER.error("Failed to load blockstate definition {} from pack {}", new Object[]{var2x, var7.sourcePackId(), var14});
                     }
                  }

                  try {
                     return loadBlockStateDefinitionStack(var2x, var3, var5x);
                  } catch (Exception var11) {
                     LOGGER.error("Failed to load blockstate definition {}", var2x, var11);
                     return null;
                  }
               }
            }, var1));
         }

         return Util.sequence(var3).thenApply((var0) -> {
            IdentityHashMap var1 = new IdentityHashMap();
            Iterator var2 = var0.iterator();

            while(var2.hasNext()) {
               BlockStateModelLoader.LoadedModels var3 = (BlockStateModelLoader.LoadedModels)var2.next();
               if (var3 != null) {
                  var1.putAll(var3.models());
               }
            }

            return new BlockStateModelLoader.LoadedModels(var1);
         });
      });
   }

   private static BlockStateModelLoader.LoadedModels loadBlockStateDefinitionStack(ResourceLocation var0, StateDefinition<Block, BlockState> var1, List<BlockStateModelLoader.LoadedBlockModelDefinition> var2) {
      IdentityHashMap var3 = new IdentityHashMap();
      Iterator var4 = var2.iterator();

      while(var4.hasNext()) {
         BlockStateModelLoader.LoadedBlockModelDefinition var5 = (BlockStateModelLoader.LoadedBlockModelDefinition)var4.next();
         var3.putAll(var5.contents.instantiate(var1, () -> {
            String var10000 = String.valueOf(var0);
            return var10000 + "/" + var5.source;
         }));
      }

      return new BlockStateModelLoader.LoadedModels(var3);
   }

   private static record LoadedBlockModelDefinition(String source, BlockModelDefinition contents) {
      final String source;
      final BlockModelDefinition contents;

      LoadedBlockModelDefinition(String param1, BlockModelDefinition param2) {
         super();
         this.source = var1;
         this.contents = var2;
      }

      public String source() {
         return this.source;
      }

      public BlockModelDefinition contents() {
         return this.contents;
      }
   }

   public static record LoadedModels(Map<BlockState, BlockStateModel.UnbakedRoot> models) {
      public LoadedModels(Map<BlockState, BlockStateModel.UnbakedRoot> param1) {
         super();
         this.models = var1;
      }

      public Map<BlockState, BlockStateModel.UnbakedRoot> models() {
         return this.models;
      }
   }
}
