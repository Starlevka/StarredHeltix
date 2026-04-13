package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;

public class BannerPatternTagsProvider extends KeyTagProvider<BannerPattern> {
   public BannerPatternTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      super(var1, Registries.BANNER_PATTERN, var2);
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.tag(BannerPatternTags.NO_ITEM_REQUIRED).add((Object[])(BannerPatterns.SQUARE_BOTTOM_LEFT, BannerPatterns.SQUARE_BOTTOM_RIGHT, BannerPatterns.SQUARE_TOP_LEFT, BannerPatterns.SQUARE_TOP_RIGHT, BannerPatterns.STRIPE_BOTTOM, BannerPatterns.STRIPE_TOP, BannerPatterns.STRIPE_LEFT, BannerPatterns.STRIPE_RIGHT, BannerPatterns.STRIPE_CENTER, BannerPatterns.STRIPE_MIDDLE, BannerPatterns.STRIPE_DOWNRIGHT, BannerPatterns.STRIPE_DOWNLEFT, BannerPatterns.STRIPE_SMALL, BannerPatterns.CROSS, BannerPatterns.STRAIGHT_CROSS, BannerPatterns.TRIANGLE_BOTTOM, BannerPatterns.TRIANGLE_TOP, BannerPatterns.TRIANGLES_BOTTOM, BannerPatterns.TRIANGLES_TOP, BannerPatterns.DIAGONAL_LEFT, BannerPatterns.DIAGONAL_RIGHT, BannerPatterns.DIAGONAL_LEFT_MIRROR, BannerPatterns.DIAGONAL_RIGHT_MIRROR, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.RHOMBUS_MIDDLE, BannerPatterns.HALF_VERTICAL, BannerPatterns.HALF_HORIZONTAL, BannerPatterns.HALF_VERTICAL_MIRROR, BannerPatterns.HALF_HORIZONTAL_MIRROR, BannerPatterns.BORDER, BannerPatterns.GRADIENT, BannerPatterns.GRADIENT_UP));
      this.tag(BannerPatternTags.PATTERN_ITEM_FLOWER).add((Object)BannerPatterns.FLOWER);
      this.tag(BannerPatternTags.PATTERN_ITEM_CREEPER).add((Object)BannerPatterns.CREEPER);
      this.tag(BannerPatternTags.PATTERN_ITEM_SKULL).add((Object)BannerPatterns.SKULL);
      this.tag(BannerPatternTags.PATTERN_ITEM_MOJANG).add((Object)BannerPatterns.MOJANG);
      this.tag(BannerPatternTags.PATTERN_ITEM_GLOBE).add((Object)BannerPatterns.GLOBE);
      this.tag(BannerPatternTags.PATTERN_ITEM_PIGLIN).add((Object)BannerPatterns.PIGLIN);
      this.tag(BannerPatternTags.PATTERN_ITEM_FLOW).add((Object)BannerPatterns.FLOW);
      this.tag(BannerPatternTags.PATTERN_ITEM_GUSTER).add((Object)BannerPatterns.GUSTER);
      this.tag(BannerPatternTags.PATTERN_ITEM_FIELD_MASONED).add((Object)BannerPatterns.BRICKS);
      this.tag(BannerPatternTags.PATTERN_ITEM_BORDURE_INDENTED).add((Object)BannerPatterns.CURLY_BORDER);
   }
}
