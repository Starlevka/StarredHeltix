package net.minecraft.client.renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

public class PlayerSkinRenderCache {
   public static final RenderType DEFAULT_PLAYER_SKIN_RENDER_TYPE = playerSkinRenderType(DefaultPlayerSkin.getDefaultSkin());
   public static final Duration CACHE_DURATION = Duration.ofMinutes(5L);
   private final LoadingCache<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>> renderInfoCache;
   private final LoadingCache<ResolvableProfile, PlayerSkinRenderCache.RenderInfo> defaultSkinCache;
   final TextureManager textureManager;
   final SkinManager skinManager;
   final ProfileResolver profileResolver;

   public PlayerSkinRenderCache(TextureManager var1, SkinManager var2, ProfileResolver var3) {
      super();
      this.renderInfoCache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_DURATION).build(new CacheLoader<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>>() {
         public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> load(ResolvableProfile var1) {
            return var1.resolveProfile(PlayerSkinRenderCache.this.profileResolver).thenCompose((var2) -> {
               return PlayerSkinRenderCache.this.skinManager.get(var2).thenApply((var3) -> {
                  return var3.map((var3x) -> {
                     return PlayerSkinRenderCache.this.new RenderInfo(var2, var3x, var1.skinPatch());
                  });
               });
            });
         }

         // $FF: synthetic method
         public Object load(final Object param1) throws Exception {
            return this.load((ResolvableProfile)var1);
         }
      });
      this.defaultSkinCache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_DURATION).build(new CacheLoader<ResolvableProfile, PlayerSkinRenderCache.RenderInfo>() {
         public PlayerSkinRenderCache.RenderInfo load(ResolvableProfile var1) {
            GameProfile var2 = var1.partialProfile();
            return PlayerSkinRenderCache.this.new RenderInfo(var2, DefaultPlayerSkin.get(var2), var1.skinPatch());
         }

         // $FF: synthetic method
         public Object load(final Object param1) throws Exception {
            return this.load((ResolvableProfile)var1);
         }
      });
      this.textureManager = var1;
      this.skinManager = var2;
      this.profileResolver = var3;
   }

   public PlayerSkinRenderCache.RenderInfo getOrDefault(ResolvableProfile var1) {
      PlayerSkinRenderCache.RenderInfo var2 = (PlayerSkinRenderCache.RenderInfo)((Optional)this.lookup(var1).getNow(Optional.empty())).orElse((Object)null);
      return var2 != null ? var2 : (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(var1);
   }

   public Supplier<PlayerSkinRenderCache.RenderInfo> createLookup(ResolvableProfile var1) {
      PlayerSkinRenderCache.RenderInfo var2 = (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(var1);
      CompletableFuture var3 = (CompletableFuture)this.renderInfoCache.getUnchecked(var1);
      Optional var4 = (Optional)var3.getNow((Object)null);
      if (var4 != null) {
         PlayerSkinRenderCache.RenderInfo var5 = (PlayerSkinRenderCache.RenderInfo)var4.orElse(var2);
         return () -> {
            return var5;
         };
      } else {
         return () -> {
            return (PlayerSkinRenderCache.RenderInfo)((Optional)var3.getNow(Optional.empty())).orElse(var2);
         };
      }
   }

   public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup(ResolvableProfile var1) {
      return (CompletableFuture)this.renderInfoCache.getUnchecked(var1);
   }

   static RenderType playerSkinRenderType(PlayerSkin var0) {
      return SkullBlockRenderer.getPlayerSkinRenderType(var0.body().texturePath());
   }

   public final class RenderInfo {
      private final GameProfile gameProfile;
      private final PlayerSkin playerSkin;
      @Nullable
      private RenderType itemRenderType;
      @Nullable
      private GpuTextureView textureView;
      @Nullable
      private GlyphRenderTypes glyphRenderTypes;

      public RenderInfo(final GameProfile param2, final PlayerSkin param3, final PlayerSkin.Patch param4) {
         super();
         this.gameProfile = var2;
         this.playerSkin = var3.with(var4);
      }

      public GameProfile gameProfile() {
         return this.gameProfile;
      }

      public PlayerSkin playerSkin() {
         return this.playerSkin;
      }

      public RenderType renderType() {
         if (this.itemRenderType == null) {
            this.itemRenderType = PlayerSkinRenderCache.playerSkinRenderType(this.playerSkin);
         }

         return this.itemRenderType;
      }

      public GpuTextureView textureView() {
         if (this.textureView == null) {
            this.textureView = PlayerSkinRenderCache.this.textureManager.getTexture(this.playerSkin.body().texturePath()).getTextureView();
         }

         return this.textureView;
      }

      public GlyphRenderTypes glyphRenderTypes() {
         if (this.glyphRenderTypes == null) {
            this.glyphRenderTypes = GlyphRenderTypes.createForColorTexture(this.playerSkin.body().texturePath());
         }

         return this.glyphRenderTypes;
      }

      public boolean equals(Object var1) {
         boolean var10000;
         if (this != var1) {
            label28: {
               if (var1 instanceof PlayerSkinRenderCache.RenderInfo) {
                  PlayerSkinRenderCache.RenderInfo var2 = (PlayerSkinRenderCache.RenderInfo)var1;
                  if (this.gameProfile.equals(var2.gameProfile) && this.playerSkin.equals(var2.playerSkin)) {
                     break label28;
                  }
               }

               var10000 = false;
               return var10000;
            }
         }

         var10000 = true;
         return var10000;
      }

      public int hashCode() {
         byte var1 = 1;
         int var2 = 31 * var1 + this.gameProfile.hashCode();
         var2 = 31 * var2 + this.playerSkin.hashCode();
         return var2;
      }
   }
}
