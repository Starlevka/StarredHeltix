package net.minecraft.nbt;

public interface PrimitiveTag extends Tag {
   default Tag copy() {
      return this;
   }
}
