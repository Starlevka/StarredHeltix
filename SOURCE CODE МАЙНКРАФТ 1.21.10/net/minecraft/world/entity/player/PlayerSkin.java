package net.minecraft.world.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlayerSkin(ClientAsset.Texture body, @Nullable ClientAsset.Texture cape, @Nullable ClientAsset.Texture elytra, PlayerModelType model, boolean secure) {
   public PlayerSkin(ClientAsset.Texture param1, @Nullable ClientAsset.Texture param2, @Nullable ClientAsset.Texture param3, PlayerModelType param4, boolean param5) {
      super();
      this.body = var1;
      this.cape = var2;
      this.elytra = var3;
      this.model = var4;
      this.secure = var5;
   }

   public static PlayerSkin insecure(ClientAsset.Texture var0, @Nullable ClientAsset.Texture var1, @Nullable ClientAsset.Texture var2, PlayerModelType var3) {
      return new PlayerSkin(var0, var1, var2, var3, false);
   }

   public PlayerSkin with(PlayerSkin.Patch var1) {
      return var1.equals(PlayerSkin.Patch.EMPTY) ? this : insecure((ClientAsset.Texture)DataFixUtils.orElse(var1.body, this.body), (ClientAsset.Texture)DataFixUtils.orElse(var1.cape, this.cape), (ClientAsset.Texture)DataFixUtils.orElse(var1.elytra, this.elytra), (PlayerModelType)var1.model.orElse(this.model));
   }

   public ClientAsset.Texture body() {
      return this.body;
   }

   @Nullable
   public ClientAsset.Texture cape() {
      return this.cape;
   }

   @Nullable
   public ClientAsset.Texture elytra() {
      return this.elytra;
   }

   public PlayerModelType model() {
      return this.model;
   }

   public boolean secure() {
      return this.secure;
   }

   public static record Patch(Optional<ClientAsset.ResourceTexture> body, Optional<ClientAsset.ResourceTexture> cape, Optional<ClientAsset.ResourceTexture> elytra, Optional<PlayerModelType> model) {
      final Optional<ClientAsset.ResourceTexture> body;
      final Optional<ClientAsset.ResourceTexture> cape;
      final Optional<ClientAsset.ResourceTexture> elytra;
      final Optional<PlayerModelType> model;
      public static final PlayerSkin.Patch EMPTY = new PlayerSkin.Patch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
      public static final MapCodec<PlayerSkin.Patch> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ClientAsset.ResourceTexture.CODEC.optionalFieldOf("texture").forGetter(PlayerSkin.Patch::body), ClientAsset.ResourceTexture.CODEC.optionalFieldOf("cape").forGetter(PlayerSkin.Patch::cape), ClientAsset.ResourceTexture.CODEC.optionalFieldOf("elytra").forGetter(PlayerSkin.Patch::elytra), PlayerModelType.CODEC.optionalFieldOf("model").forGetter(PlayerSkin.Patch::model)).apply(var0, PlayerSkin.Patch::create);
      });
      public static final StreamCodec<ByteBuf, PlayerSkin.Patch> STREAM_CODEC;

      public Patch(Optional<ClientAsset.ResourceTexture> param1, Optional<ClientAsset.ResourceTexture> param2, Optional<ClientAsset.ResourceTexture> param3, Optional<PlayerModelType> param4) {
         super();
         this.body = var1;
         this.cape = var2;
         this.elytra = var3;
         this.model = var4;
      }

      public static PlayerSkin.Patch create(Optional<ClientAsset.ResourceTexture> var0, Optional<ClientAsset.ResourceTexture> var1, Optional<ClientAsset.ResourceTexture> var2, Optional<PlayerModelType> var3) {
         return var0.isEmpty() && var1.isEmpty() && var2.isEmpty() && var3.isEmpty() ? EMPTY : new PlayerSkin.Patch(var0, var1, var2, var3);
      }

      public Optional<ClientAsset.ResourceTexture> body() {
         return this.body;
      }

      public Optional<ClientAsset.ResourceTexture> cape() {
         return this.cape;
      }

      public Optional<ClientAsset.ResourceTexture> elytra() {
         return this.elytra;
      }

      public Optional<PlayerModelType> model() {
         return this.model;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerSkin.Patch::body, ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerSkin.Patch::cape, ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerSkin.Patch::elytra, PlayerModelType.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerSkin.Patch::model, PlayerSkin.Patch::create);
      }
   }
}
