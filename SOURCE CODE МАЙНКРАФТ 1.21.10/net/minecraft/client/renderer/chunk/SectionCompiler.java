package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SectionCompiler {
   private final BlockRenderDispatcher blockRenderer;
   private final BlockEntityRenderDispatcher blockEntityRenderer;

   public SectionCompiler(BlockRenderDispatcher var1, BlockEntityRenderDispatcher var2) {
      super();
      this.blockRenderer = var1;
      this.blockEntityRenderer = var2;
   }

   public SectionCompiler.Results compile(SectionPos var1, RenderSectionRegion var2, VertexSorting var3, SectionBufferBuilderPack var4) {
      SectionCompiler.Results var5 = new SectionCompiler.Results();
      BlockPos var6 = var1.origin();
      BlockPos var7 = var6.offset(15, 15, 15);
      VisGraph var8 = new VisGraph();
      PoseStack var9 = new PoseStack();
      ModelBlockRenderer.enableCaching();
      EnumMap var10 = new EnumMap(ChunkSectionLayer.class);
      RandomSource var11 = RandomSource.create();
      ObjectArrayList var12 = new ObjectArrayList();
      Iterator var13 = BlockPos.betweenClosed(var6, var7).iterator();

      while(var13.hasNext()) {
         BlockPos var14 = (BlockPos)var13.next();
         BlockState var15 = var2.getBlockState(var14);
         if (var15.isSolidRender()) {
            var8.setOpaque(var14);
         }

         if (var15.hasBlockEntity()) {
            BlockEntity var16 = var2.getBlockEntity(var14);
            if (var16 != null) {
               this.handleBlockEntity(var5, var16);
            }
         }

         FluidState var21 = var15.getFluidState();
         ChunkSectionLayer var17;
         BufferBuilder var18;
         if (!var21.isEmpty()) {
            var17 = ItemBlockRenderTypes.getRenderLayer(var21);
            var18 = this.getOrBeginLayer(var10, var4, var17);
            this.blockRenderer.renderLiquid(var14, var2, var18, var15, var21);
         }

         if (var15.getRenderShape() == RenderShape.MODEL) {
            var17 = ItemBlockRenderTypes.getChunkRenderType(var15);
            var18 = this.getOrBeginLayer(var10, var4, var17);
            var11.setSeed(var15.getSeed(var14));
            this.blockRenderer.getBlockModel(var15).collectParts(var11, var12);
            var9.pushPose();
            var9.translate((float)SectionPos.sectionRelative(var14.getX()), (float)SectionPos.sectionRelative(var14.getY()), (float)SectionPos.sectionRelative(var14.getZ()));
            this.blockRenderer.renderBatched(var15, var14, var2, var9, var18, true, var12);
            var9.popPose();
            var12.clear();
         }
      }

      var13 = var10.entrySet().iterator();

      while(var13.hasNext()) {
         Entry var19 = (Entry)var13.next();
         ChunkSectionLayer var20 = (ChunkSectionLayer)var19.getKey();
         MeshData var22 = ((BufferBuilder)var19.getValue()).build();
         if (var22 != null) {
            if (var20 == ChunkSectionLayer.TRANSLUCENT) {
               var5.transparencyState = var22.sortQuads(var4.buffer(var20), var3);
            }

            var5.renderedLayers.put(var20, var22);
         }
      }

      ModelBlockRenderer.clearCache();
      var5.visibilitySet = var8.resolve();
      return var5;
   }

   private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> var1, SectionBufferBuilderPack var2, ChunkSectionLayer var3) {
      BufferBuilder var4 = (BufferBuilder)var1.get(var3);
      if (var4 == null) {
         ByteBufferBuilder var5 = var2.buffer(var3);
         var4 = new BufferBuilder(var5, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
         var1.put(var3, var4);
      }

      return var4;
   }

   private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results var1, E var2) {
      BlockEntityRenderer var3 = this.blockEntityRenderer.getRenderer(var2);
      if (var3 != null && !var3.shouldRenderOffScreen()) {
         var1.blockEntities.add(var2);
      }

   }

   public static final class Results {
      public final List<BlockEntity> blockEntities = new ArrayList();
      public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap(ChunkSectionLayer.class);
      public VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      public MeshData.SortState transparencyState;

      public Results() {
         super();
      }

      public void release() {
         this.renderedLayers.values().forEach(MeshData::close);
      }
   }
}
