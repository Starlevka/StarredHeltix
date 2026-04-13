package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public record BakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, int lightEmission) {
   public BakedQuad(int[] param1, int param2, Direction param3, TextureAtlasSprite param4, boolean param5, int param6) {
      super();
      this.vertices = var1;
      this.tintIndex = var2;
      this.direction = var3;
      this.sprite = var4;
      this.shade = var5;
      this.lightEmission = var6;
   }

   public boolean isTinted() {
      return this.tintIndex != -1;
   }

   public int[] vertices() {
      return this.vertices;
   }

   public int tintIndex() {
      return this.tintIndex;
   }

   public Direction direction() {
      return this.direction;
   }

   public TextureAtlasSprite sprite() {
      return this.sprite;
   }

   public boolean shade() {
      return this.shade;
   }

   public int lightEmission() {
      return this.lightEmission;
   }
}
