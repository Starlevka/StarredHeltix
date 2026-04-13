package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record BlocksAttacks(float blockDelaySeconds, float disableCooldownScale, List<BlocksAttacks.DamageReduction> damageReductions, BlocksAttacks.ItemDamageFunction itemDamage, Optional<TagKey<DamageType>> bypassedBy, Optional<Holder<SoundEvent>> blockSound, Optional<Holder<SoundEvent>> disableSound) {
   public static final Codec<BlocksAttacks> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("block_delay_seconds", 0.0F).forGetter(BlocksAttacks::blockDelaySeconds), ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("disable_cooldown_scale", 1.0F).forGetter(BlocksAttacks::disableCooldownScale), BlocksAttacks.DamageReduction.CODEC.listOf().optionalFieldOf("damage_reductions", List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F))).forGetter(BlocksAttacks::damageReductions), BlocksAttacks.ItemDamageFunction.CODEC.optionalFieldOf("item_damage", BlocksAttacks.ItemDamageFunction.DEFAULT).forGetter(BlocksAttacks::itemDamage), TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("bypassed_by").forGetter(BlocksAttacks::bypassedBy), SoundEvent.CODEC.optionalFieldOf("block_sound").forGetter(BlocksAttacks::blockSound), SoundEvent.CODEC.optionalFieldOf("disabled_sound").forGetter(BlocksAttacks::disableSound)).apply(var0, BlocksAttacks::new);
   });
   public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks> STREAM_CODEC;

   public BlocksAttacks(float param1, float param2, List<BlocksAttacks.DamageReduction> param3, BlocksAttacks.ItemDamageFunction param4, Optional<TagKey<DamageType>> param5, Optional<Holder<SoundEvent>> param6, Optional<Holder<SoundEvent>> param7) {
      super();
      this.blockDelaySeconds = var1;
      this.disableCooldownScale = var2;
      this.damageReductions = var3;
      this.itemDamage = var4;
      this.bypassedBy = var5;
      this.blockSound = var6;
      this.disableSound = var7;
   }

   public void onBlocked(ServerLevel var1, LivingEntity var2) {
      this.blockSound.ifPresent((var2x) -> {
         var1.playSound((Entity)null, var2.getX(), var2.getY(), var2.getZ(), var2x, var2.getSoundSource(), 1.0F, 0.8F + var1.random.nextFloat() * 0.4F);
      });
   }

   public void disable(ServerLevel var1, LivingEntity var2, float var3, ItemStack var4) {
      int var5 = this.disableBlockingForTicks(var3);
      if (var5 > 0) {
         if (var2 instanceof Player) {
            Player var6 = (Player)var2;
            var6.getCooldowns().addCooldown(var4, var5);
         }

         var2.stopUsingItem();
         this.disableSound.ifPresent((var2x) -> {
            var1.playSound((Entity)null, var2.getX(), var2.getY(), var2.getZ(), var2x, var2.getSoundSource(), 0.8F, 0.8F + var1.random.nextFloat() * 0.4F);
         });
      }

   }

   public void hurtBlockingItem(Level var1, ItemStack var2, LivingEntity var3, InteractionHand var4, float var5) {
      if (var3 instanceof Player) {
         Player var6 = (Player)var3;
         if (!var1.isClientSide()) {
            var6.awardStat(Stats.ITEM_USED.get(var2.getItem()));
         }

         int var7 = this.itemDamage.apply(var5);
         if (var7 > 0) {
            var2.hurtAndBreak(var7, var3, var4.asEquipmentSlot());
         }

      }
   }

   private int disableBlockingForTicks(float var1) {
      float var2 = var1 * this.disableCooldownScale;
      return var2 > 0.0F ? Math.round(var2 * 20.0F) : 0;
   }

   public int blockDelayTicks() {
      return Math.round(this.blockDelaySeconds * 20.0F);
   }

   public float resolveBlockedDamage(DamageSource var1, float var2, double var3) {
      float var5 = 0.0F;

      BlocksAttacks.DamageReduction var7;
      for(Iterator var6 = this.damageReductions.iterator(); var6.hasNext(); var5 += var7.resolve(var1, var2, var3)) {
         var7 = (BlocksAttacks.DamageReduction)var6.next();
      }

      return Mth.clamp(var5, 0.0F, var2);
   }

   public float blockDelaySeconds() {
      return this.blockDelaySeconds;
   }

   public float disableCooldownScale() {
      return this.disableCooldownScale;
   }

   public List<BlocksAttacks.DamageReduction> damageReductions() {
      return this.damageReductions;
   }

   public BlocksAttacks.ItemDamageFunction itemDamage() {
      return this.itemDamage;
   }

   public Optional<TagKey<DamageType>> bypassedBy() {
      return this.bypassedBy;
   }

   public Optional<Holder<SoundEvent>> blockSound() {
      return this.blockSound;
   }

   public Optional<Holder<SoundEvent>> disableSound() {
      return this.disableSound;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, BlocksAttacks::blockDelaySeconds, ByteBufCodecs.FLOAT, BlocksAttacks::disableCooldownScale, BlocksAttacks.DamageReduction.STREAM_CODEC.apply(ByteBufCodecs.list()), BlocksAttacks::damageReductions, BlocksAttacks.ItemDamageFunction.STREAM_CODEC, BlocksAttacks::itemDamage, TagKey.streamCodec(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional), BlocksAttacks::bypassedBy, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), BlocksAttacks::blockSound, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), BlocksAttacks::disableSound, BlocksAttacks::new);
   }

   public static record ItemDamageFunction(float threshold, float base, float factor) {
      public static final Codec<BlocksAttacks.ItemDamageFunction> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("threshold").forGetter(BlocksAttacks.ItemDamageFunction::threshold), Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.ItemDamageFunction::base), Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.ItemDamageFunction::factor)).apply(var0, BlocksAttacks.ItemDamageFunction::new);
      });
      public static final StreamCodec<ByteBuf, BlocksAttacks.ItemDamageFunction> STREAM_CODEC;
      public static final BlocksAttacks.ItemDamageFunction DEFAULT;

      public ItemDamageFunction(float param1, float param2, float param3) {
         super();
         this.threshold = var1;
         this.base = var2;
         this.factor = var3;
      }

      public int apply(float var1) {
         return var1 < this.threshold ? 0 : Mth.floor(this.base + this.factor * var1);
      }

      public float threshold() {
         return this.threshold;
      }

      public float base() {
         return this.base;
      }

      public float factor() {
         return this.factor;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::threshold, ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::base, ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::factor, BlocksAttacks.ItemDamageFunction::new);
         DEFAULT = new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F);
      }
   }

   public static record DamageReduction(float horizontalBlockingAngle, Optional<HolderSet<DamageType>> type, float base, float factor) {
      public static final Codec<BlocksAttacks.DamageReduction> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("horizontal_blocking_angle", 90.0F).forGetter(BlocksAttacks.DamageReduction::horizontalBlockingAngle), RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("type").forGetter(BlocksAttacks.DamageReduction::type), Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.DamageReduction::base), Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.DamageReduction::factor)).apply(var0, BlocksAttacks.DamageReduction::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks.DamageReduction> STREAM_CODEC;

      public DamageReduction(float param1, Optional<HolderSet<DamageType>> param2, float param3, float param4) {
         super();
         this.horizontalBlockingAngle = var1;
         this.type = var2;
         this.base = var3;
         this.factor = var4;
      }

      public float resolve(DamageSource var1, float var2, double var3) {
         if (var3 > (double)(0.017453292F * this.horizontalBlockingAngle)) {
            return 0.0F;
         } else {
            return this.type.isPresent() && !((HolderSet)this.type.get()).contains(var1.typeHolder()) ? 0.0F : Mth.clamp(this.base + this.factor * var2, 0.0F, var2);
         }
      }

      public float horizontalBlockingAngle() {
         return this.horizontalBlockingAngle;
      }

      public Optional<HolderSet<DamageType>> type() {
         return this.type;
      }

      public float base() {
         return this.base;
      }

      public float factor() {
         return this.factor;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::horizontalBlockingAngle, ByteBufCodecs.holderSet(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional), BlocksAttacks.DamageReduction::type, ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::base, ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::factor, BlocksAttacks.DamageReduction::new);
      }
   }
}
