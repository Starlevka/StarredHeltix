package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface ItemOwner {
   Level level();

   Vec3 position();

   float getVisualRotationYInDegrees();

   @Nullable
   default LivingEntity asLivingEntity() {
      return null;
   }

   static ItemOwner offsetFromOwner(ItemOwner var0, Vec3 var1) {
      return new ItemOwner.OffsetFromOwner(var0, var1);
   }

   public static record OffsetFromOwner(ItemOwner owner, Vec3 offset) implements ItemOwner {
      public OffsetFromOwner(ItemOwner param1, Vec3 param2) {
         super();
         this.owner = var1;
         this.offset = var2;
      }

      public Level level() {
         return this.owner.level();
      }

      public Vec3 position() {
         return this.owner.position().add(this.offset);
      }

      public float getVisualRotationYInDegrees() {
         return this.owner.getVisualRotationYInDegrees();
      }

      @Nullable
      public LivingEntity asLivingEntity() {
         return this.owner.asLivingEntity();
      }

      public ItemOwner owner() {
         return this.owner;
      }

      public Vec3 offset() {
         return this.offset;
      }
   }
}
