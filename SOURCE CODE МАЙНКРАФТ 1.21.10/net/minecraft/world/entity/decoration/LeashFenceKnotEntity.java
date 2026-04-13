package net.minecraft.world.entity.decoration;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends BlockAttachedEntity {
   public static final double OFFSET_Y = 0.375D;

   public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> var1, Level var2) {
      super(var1, var2);
   }

   public LeashFenceKnotEntity(Level var1, BlockPos var2) {
      super(EntityType.LEASH_KNOT, var1, var2);
      this.setPos((double)var2.getX(), (double)var2.getY(), (double)var2.getZ());
   }

   protected void defineSynchedData(SynchedEntityData.Builder var1) {
   }

   protected void recalculateBoundingBox() {
      this.setPosRaw((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.375D, (double)this.pos.getZ() + 0.5D);
      double var1 = (double)this.getType().getWidth() / 2.0D;
      double var3 = (double)this.getType().getHeight();
      this.setBoundingBox(new AABB(this.getX() - var1, this.getY(), this.getZ() - var1, this.getX() + var1, this.getY() + var3, this.getZ() + var1));
   }

   public boolean shouldRenderAtSqrDistance(double var1) {
      return var1 < 1024.0D;
   }

   public void dropItem(ServerLevel var1, @Nullable Entity var2) {
      this.playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
   }

   protected void addAdditionalSaveData(ValueOutput var1) {
   }

   protected void readAdditionalSaveData(ValueInput var1) {
   }

   public InteractionResult interact(Player var1, InteractionHand var2) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         if (var1.getItemInHand(var2).is(Items.SHEARS)) {
            InteractionResult var3 = super.interact(var1, var2);
            if (var3 instanceof InteractionResult.Success) {
               InteractionResult.Success var4 = (InteractionResult.Success)var3;
               if (var4.wasItemInteraction()) {
                  return var3;
               }
            }
         }

         boolean var9 = false;
         List var10 = Leashable.leashableLeashedTo(var1);
         Iterator var5 = var10.iterator();

         while(var5.hasNext()) {
            Leashable var6 = (Leashable)var5.next();
            if (var6.canHaveALeashAttachedTo(this)) {
               var6.setLeashedTo(this, true);
               var9 = true;
            }
         }

         boolean var11 = false;
         if (!var9 && !var1.isSecondaryUseActive()) {
            List var12 = Leashable.leashableLeashedTo(this);
            Iterator var7 = var12.iterator();

            while(var7.hasNext()) {
               Leashable var8 = (Leashable)var7.next();
               if (var8.canHaveALeashAttachedTo(var1)) {
                  var8.setLeashedTo(var1, true);
                  var11 = true;
               }
            }
         }

         if (!var9 && !var11) {
            return super.interact(var1, var2);
         } else {
            this.gameEvent(GameEvent.BLOCK_ATTACH, var1);
            this.playSound(SoundEvents.LEAD_TIED);
            return InteractionResult.SUCCESS;
         }
      }
   }

   public void notifyLeasheeRemoved(Leashable var1) {
      if (Leashable.leashableLeashedTo(this).isEmpty()) {
         this.discard();
      }

   }

   public boolean survives() {
      return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
   }

   public static LeashFenceKnotEntity getOrCreateKnot(Level var0, BlockPos var1) {
      int var2 = var1.getX();
      int var3 = var1.getY();
      int var4 = var1.getZ();
      List var5 = var0.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB((double)var2 - 1.0D, (double)var3 - 1.0D, (double)var4 - 1.0D, (double)var2 + 1.0D, (double)var3 + 1.0D, (double)var4 + 1.0D));
      Iterator var6 = var5.iterator();

      LeashFenceKnotEntity var7;
      do {
         if (!var6.hasNext()) {
            LeashFenceKnotEntity var8 = new LeashFenceKnotEntity(var0, var1);
            var0.addFreshEntity(var8);
            return var8;
         }

         var7 = (LeashFenceKnotEntity)var6.next();
      } while(!var7.getPos().equals(var1));

      return var7;
   }

   public void playPlacementSound() {
      this.playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity var1) {
      return new ClientboundAddEntityPacket(this, 0, this.getPos());
   }

   public Vec3 getRopeHoldPosition(float var1) {
      return this.getPosition(var1).add(0.0D, 0.2D, 0.0D);
   }

   public ItemStack getPickResult() {
      return new ItemStack(Items.LEAD);
   }
}
