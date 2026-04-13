package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class MultiPartModel implements BlockStateModel {
   private final MultiPartModel.SharedBakedState shared;
   private final BlockState blockState;
   @Nullable
   private List<BlockStateModel> models;

   MultiPartModel(MultiPartModel.SharedBakedState var1, BlockState var2) {
      super();
      this.shared = var1;
      this.blockState = var2;
   }

   public TextureAtlasSprite particleIcon() {
      return this.shared.particleIcon;
   }

   public void collectParts(RandomSource var1, List<BlockModelPart> var2) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      long var3 = var1.nextLong();
      Iterator var5 = this.models.iterator();

      while(var5.hasNext()) {
         BlockStateModel var6 = (BlockStateModel)var5.next();
         var1.setSeed(var3);
         var6.collectParts(var1, var2);
      }

   }

   private static final class SharedBakedState {
      private final List<MultiPartModel.Selector<BlockStateModel>> selectors;
      final TextureAtlasSprite particleIcon;
      private final Map<BitSet, List<BlockStateModel>> subsets = new ConcurrentHashMap();

      private static BlockStateModel getFirstModel(List<MultiPartModel.Selector<BlockStateModel>> var0) {
         if (var0.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one selector");
         } else {
            return (BlockStateModel)((MultiPartModel.Selector)var0.getFirst()).model();
         }
      }

      public SharedBakedState(List<MultiPartModel.Selector<BlockStateModel>> var1) {
         super();
         this.selectors = var1;
         BlockStateModel var2 = getFirstModel(var1);
         this.particleIcon = var2.particleIcon();
      }

      public List<BlockStateModel> selectModels(BlockState var1) {
         BitSet var2 = new BitSet();

         for(int var3 = 0; var3 < this.selectors.size(); ++var3) {
            if (((MultiPartModel.Selector)this.selectors.get(var3)).condition.test(var1)) {
               var2.set(var3);
            }
         }

         return (List)this.subsets.computeIfAbsent(var2, (var1x) -> {
            Builder var2 = ImmutableList.builder();

            for(int var3 = 0; var3 < this.selectors.size(); ++var3) {
               if (var1x.get(var3)) {
                  var2.add((BlockStateModel)((MultiPartModel.Selector)this.selectors.get(var3)).model);
               }
            }

            return var2.build();
         });
      }
   }

   public static class Unbaked implements BlockStateModel.UnbakedRoot {
      final List<MultiPartModel.Selector<BlockStateModel.Unbaked>> selectors;
      private final ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState> sharedStateKey = new ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState>() {
         public MultiPartModel.SharedBakedState compute(ModelBaker var1) {
            Builder var2 = ImmutableList.builderWithExpectedSize(Unbaked.this.selectors.size());
            Iterator var3 = Unbaked.this.selectors.iterator();

            while(var3.hasNext()) {
               MultiPartModel.Selector var4 = (MultiPartModel.Selector)var3.next();
               var2.add(var4.with(((BlockStateModel.Unbaked)var4.model).bake(var1)));
            }

            return new MultiPartModel.SharedBakedState(var2.build());
         }

         // $FF: synthetic method
         public Object compute(final ModelBaker param1) {
            return this.compute(var1);
         }
      };

      public Unbaked(List<MultiPartModel.Selector<BlockStateModel.Unbaked>> var1) {
         super();
         this.selectors = var1;
      }

      public Object visualEqualityGroup(BlockState var1) {
         IntArrayList var2 = new IntArrayList();

         for(int var3 = 0; var3 < this.selectors.size(); ++var3) {
            if (((MultiPartModel.Selector)this.selectors.get(var3)).condition.test(var1)) {
               var2.add(var3);
            }
         }

         record 1Key(MultiPartModel.Unbaked model, IntList selectors) {
            _Key/* $FF was: 1Key*/(MultiPartModel.Unbaked param1, IntList param2) {
               super();
               this.model = var1;
               this.selectors = var2;
            }

            public MultiPartModel.Unbaked model() {
               return this.model;
            }

            public IntList selectors() {
               return this.selectors;
            }
         }

         return new 1Key(this, var2);
      }

      public void resolveDependencies(ResolvableModel.Resolver var1) {
         this.selectors.forEach((var1x) -> {
            ((BlockStateModel.Unbaked)var1x.model).resolveDependencies(var1);
         });
      }

      public BlockStateModel bake(BlockState var1, ModelBaker var2) {
         MultiPartModel.SharedBakedState var3 = (MultiPartModel.SharedBakedState)var2.compute(this.sharedStateKey);
         return new MultiPartModel(var3, var1);
      }
   }

   public static record Selector<T>(Predicate<BlockState> condition, T model) {
      final Predicate<BlockState> condition;
      final T model;

      public Selector(Predicate<BlockState> param1, T param2) {
         super();
         this.condition = var1;
         this.model = var2;
      }

      public <S> MultiPartModel.Selector<S> with(S var1) {
         return new MultiPartModel.Selector(this.condition, var1);
      }

      public Predicate<BlockState> condition() {
         return this.condition;
      }

      public T model() {
         return this.model;
      }
   }
}
