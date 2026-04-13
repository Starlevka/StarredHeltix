package net.minecraft.client.resources.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class AtlasManager implements PreparableReloadListener, MaterialSet, AutoCloseable {
   private static final List<AtlasManager.AtlasConfig> KNOWN_ATLASES;
   public static final PreparableReloadListener.StateKey<AtlasManager.PendingStitchResults> PENDING_STITCH;
   private final Map<ResourceLocation, AtlasManager.AtlasEntry> atlasByTexture = new HashMap();
   private final Map<ResourceLocation, AtlasManager.AtlasEntry> atlasById = new HashMap();
   private Map<Material, TextureAtlasSprite> materialLookup = Map.of();
   private int maxMipmapLevels;

   public AtlasManager(TextureManager var1, int var2) {
      super();
      Iterator var3 = KNOWN_ATLASES.iterator();

      while(var3.hasNext()) {
         AtlasManager.AtlasConfig var4 = (AtlasManager.AtlasConfig)var3.next();
         TextureAtlas var5 = new TextureAtlas(var4.textureId);
         var1.register(var4.textureId, var5);
         AtlasManager.AtlasEntry var6 = new AtlasManager.AtlasEntry(var5, var4);
         this.atlasByTexture.put(var4.textureId, var6);
         this.atlasById.put(var4.definitionLocation, var6);
      }

      this.maxMipmapLevels = var2;
   }

   public TextureAtlas getAtlasOrThrow(ResourceLocation var1) {
      AtlasManager.AtlasEntry var2 = (AtlasManager.AtlasEntry)this.atlasById.get(var1);
      if (var2 == null) {
         throw new IllegalArgumentException("Invalid atlas id: " + String.valueOf(var1));
      } else {
         return var2.atlas();
      }
   }

   public void forEach(BiConsumer<ResourceLocation, TextureAtlas> var1) {
      this.atlasById.forEach((var1x, var2) -> {
         var1.accept(var1x, var2.atlas);
      });
   }

   public void updateMaxMipLevel(int var1) {
      this.maxMipmapLevels = var1;
   }

   public void close() {
      this.materialLookup = Map.of();
      this.atlasById.values().forEach(AtlasManager.AtlasEntry::close);
      this.atlasById.clear();
      this.atlasByTexture.clear();
   }

   public TextureAtlasSprite get(Material var1) {
      TextureAtlasSprite var2 = (TextureAtlasSprite)this.materialLookup.get(var1);
      if (var2 != null) {
         return var2;
      } else {
         ResourceLocation var3 = var1.atlasLocation();
         AtlasManager.AtlasEntry var4 = (AtlasManager.AtlasEntry)this.atlasByTexture.get(var3);
         if (var4 == null) {
            throw new IllegalArgumentException("Invalid atlas texture id: " + String.valueOf(var3));
         } else {
            return var4.atlas().missingSprite();
         }
      }
   }

   public void prepareSharedState(PreparableReloadListener.SharedState var1) {
      int var2 = this.atlasById.size();
      ArrayList var3 = new ArrayList(var2);
      HashMap var4 = new HashMap(var2);
      ArrayList var5 = new ArrayList(var2);
      this.atlasById.forEach((var3x, var4x) -> {
         CompletableFuture var5x = new CompletableFuture();
         var4.put(var3x, var5x);
         var3.add(new AtlasManager.PendingStitch(var4x, var5x));
         var5.add(var5x.thenCompose(SpriteLoader.Preparations::readyForUpload));
      });
      CompletableFuture var6 = CompletableFuture.allOf((CompletableFuture[])var5.toArray((var0) -> {
         return new CompletableFuture[var0];
      }));
      var1.set(PENDING_STITCH, new AtlasManager.PendingStitchResults(var3, var4, var6));
   }

   public CompletableFuture<Void> reload(PreparableReloadListener.SharedState var1, Executor var2, PreparableReloadListener.PreparationBarrier var3, Executor var4) {
      AtlasManager.PendingStitchResults var5 = (AtlasManager.PendingStitchResults)var1.get(PENDING_STITCH);
      ResourceManager var6 = var1.resourceManager();
      var5.pendingStitches.forEach((var3x) -> {
         var3x.entry.scheduleLoad(var6, var2, this.maxMipmapLevels).whenComplete((var1, var2x) -> {
            if (var1 != null) {
               var3x.preparations.complete(var1);
            } else {
               var3x.preparations.completeExceptionally(var2x);
            }

         });
      });
      CompletableFuture var10000 = var5.allReadyToUpload;
      Objects.requireNonNull(var3);
      return var10000.thenCompose(var3::wait).thenAcceptAsync((var2x) -> {
         this.materialLookup = var5.joinAndUpload();
      }, var4);
   }

   static {
      KNOWN_ATLASES = List.of(new AtlasManager.AtlasConfig(Sheets.ARMOR_TRIMS_SHEET, AtlasIds.ARMOR_TRIMS, false), new AtlasManager.AtlasConfig(Sheets.BANNER_SHEET, AtlasIds.BANNER_PATTERNS, false), new AtlasManager.AtlasConfig(Sheets.BED_SHEET, AtlasIds.BEDS, false), new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_BLOCKS, AtlasIds.BLOCKS, true), new AtlasManager.AtlasConfig(Sheets.CHEST_SHEET, AtlasIds.CHESTS, false), new AtlasManager.AtlasConfig(Sheets.DECORATED_POT_SHEET, AtlasIds.DECORATED_POT, false), new AtlasManager.AtlasConfig(Sheets.GUI_SHEET, AtlasIds.GUI, false, Set.of(GuiMetadataSection.TYPE)), new AtlasManager.AtlasConfig(Sheets.MAP_DECORATIONS_SHEET, AtlasIds.MAP_DECORATIONS, false), new AtlasManager.AtlasConfig(Sheets.PAINTINGS_SHEET, AtlasIds.PAINTINGS, false), new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_PARTICLES, AtlasIds.PARTICLES, false), new AtlasManager.AtlasConfig(Sheets.SHIELD_SHEET, AtlasIds.SHIELD_PATTERNS, false), new AtlasManager.AtlasConfig(Sheets.SHULKER_SHEET, AtlasIds.SHULKER_BOXES, false), new AtlasManager.AtlasConfig(Sheets.SIGN_SHEET, AtlasIds.SIGNS, false));
      PENDING_STITCH = new PreparableReloadListener.StateKey();
   }

   public static record AtlasConfig(ResourceLocation textureId, ResourceLocation definitionLocation, boolean createMipmaps, Set<MetadataSectionType<?>> additionalMetadata) {
      final ResourceLocation textureId;
      final ResourceLocation definitionLocation;
      final boolean createMipmaps;
      final Set<MetadataSectionType<?>> additionalMetadata;

      public AtlasConfig(ResourceLocation var1, ResourceLocation var2, boolean var3) {
         this(var1, var2, var3, Set.of());
      }

      public AtlasConfig(ResourceLocation param1, ResourceLocation param2, boolean param3, Set<MetadataSectionType<?>> param4) {
         super();
         this.textureId = var1;
         this.definitionLocation = var2;
         this.createMipmaps = var3;
         this.additionalMetadata = var4;
      }

      public ResourceLocation textureId() {
         return this.textureId;
      }

      public ResourceLocation definitionLocation() {
         return this.definitionLocation;
      }

      public boolean createMipmaps() {
         return this.createMipmaps;
      }

      public Set<MetadataSectionType<?>> additionalMetadata() {
         return this.additionalMetadata;
      }
   }

   private static record AtlasEntry(TextureAtlas atlas, AtlasManager.AtlasConfig config) implements AutoCloseable {
      final TextureAtlas atlas;
      final AtlasManager.AtlasConfig config;

      AtlasEntry(TextureAtlas param1, AtlasManager.AtlasConfig param2) {
         super();
         this.atlas = var1;
         this.config = var2;
      }

      public void close() {
         this.atlas.clearTextureData();
      }

      CompletableFuture<SpriteLoader.Preparations> scheduleLoad(ResourceManager var1, Executor var2, int var3) {
         return SpriteLoader.create(this.atlas).loadAndStitch(var1, this.config.definitionLocation, this.config.createMipmaps ? var3 : 0, var2, this.config.additionalMetadata);
      }

      public TextureAtlas atlas() {
         return this.atlas;
      }

      public AtlasManager.AtlasConfig config() {
         return this.config;
      }
   }

   public static class PendingStitchResults {
      final List<AtlasManager.PendingStitch> pendingStitches;
      private final Map<ResourceLocation, CompletableFuture<SpriteLoader.Preparations>> stitchFuturesById;
      final CompletableFuture<?> allReadyToUpload;

      PendingStitchResults(List<AtlasManager.PendingStitch> var1, Map<ResourceLocation, CompletableFuture<SpriteLoader.Preparations>> var2, CompletableFuture<?> var3) {
         super();
         this.pendingStitches = var1;
         this.stitchFuturesById = var2;
         this.allReadyToUpload = var3;
      }

      public Map<Material, TextureAtlasSprite> joinAndUpload() {
         HashMap var1 = new HashMap();
         this.pendingStitches.forEach((var1x) -> {
            var1x.joinAndUpload(var1);
         });
         return var1;
      }

      public CompletableFuture<SpriteLoader.Preparations> get(ResourceLocation var1) {
         return (CompletableFuture)Objects.requireNonNull((CompletableFuture)this.stitchFuturesById.get(var1));
      }
   }

   private static record PendingStitch(AtlasManager.AtlasEntry entry, CompletableFuture<SpriteLoader.Preparations> preparations) {
      final AtlasManager.AtlasEntry entry;
      final CompletableFuture<SpriteLoader.Preparations> preparations;

      PendingStitch(AtlasManager.AtlasEntry param1, CompletableFuture<SpriteLoader.Preparations> param2) {
         super();
         this.entry = var1;
         this.preparations = var2;
      }

      public void joinAndUpload(Map<Material, TextureAtlasSprite> var1) {
         SpriteLoader.Preparations var2 = (SpriteLoader.Preparations)this.preparations.join();
         this.entry.atlas.upload(var2);
         var2.regions().forEach((var2x, var3) -> {
            var1.put(new Material(this.entry.config.textureId, var2x), var3);
         });
      }

      public AtlasManager.AtlasEntry entry() {
         return this.entry;
      }

      public CompletableFuture<SpriteLoader.Preparations> preparations() {
         return this.preparations;
      }
   }
}
