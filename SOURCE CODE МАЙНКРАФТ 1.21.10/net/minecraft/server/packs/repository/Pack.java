package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;

public class Pack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;
   private final Pack.ResourcesSupplier resources;
   private final Pack.Metadata metadata;
   private final PackSelectionConfig selectionConfig;

   @Nullable
   public static Pack readMetaAndCreate(PackLocationInfo var0, Pack.ResourcesSupplier var1, PackType var2, PackSelectionConfig var3) {
      PackFormat var4 = SharedConstants.getCurrentVersion().packVersion(var2);
      Pack.Metadata var5 = readPackMetadata(var0, var1, var4, var2);
      return var5 != null ? new Pack(var0, var1, var5, var3) : null;
   }

   public Pack(PackLocationInfo var1, Pack.ResourcesSupplier var2, Pack.Metadata var3, PackSelectionConfig var4) {
      super();
      this.location = var1;
      this.resources = var2;
      this.metadata = var3;
      this.selectionConfig = var4;
   }

   @Nullable
   public static Pack.Metadata readPackMetadata(PackLocationInfo var0, Pack.ResourcesSupplier var1, PackFormat var2, PackType var3) {
      try {
         PackResources var4 = var1.openPrimary(var0);

         FeatureFlagsMetadataSection var6;
         label61: {
            Pack.Metadata var11;
            try {
               PackMetadataSection var5 = (PackMetadataSection)var4.getMetadataSection(PackMetadataSection.forPackType(var3));
               if (var5 == null) {
                  var5 = (PackMetadataSection)var4.getMetadataSection(PackMetadataSection.FALLBACK_TYPE);
               }

               if (var5 == null) {
                  LOGGER.warn("Missing metadata in pack {}", var0.id());
                  var6 = null;
                  break label61;
               }

               var6 = (FeatureFlagsMetadataSection)var4.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
               FeatureFlagSet var7 = var6 != null ? var6.flags() : FeatureFlagSet.of();
               PackCompatibility var8 = PackCompatibility.forVersion(var5.supportedFormats(), var2);
               OverlayMetadataSection var9 = (OverlayMetadataSection)var4.getMetadataSection(OverlayMetadataSection.forPackType(var3));
               List var10 = var9 != null ? var9.overlaysForVersion(var2) : List.of();
               var11 = new Pack.Metadata(var5.description(), var8, var7, var10);
            } catch (Throwable var13) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (var4 != null) {
               var4.close();
            }

            return var11;
         }

         if (var4 != null) {
            var4.close();
         }

         return var6;
      } catch (Exception var14) {
         LOGGER.warn("Failed to read pack {} metadata", var0.id(), var14);
         return null;
      }
   }

   public PackLocationInfo location() {
      return this.location;
   }

   public Component getTitle() {
      return this.location.title();
   }

   public Component getDescription() {
      return this.metadata.description();
   }

   public Component getChatLink(boolean var1) {
      return this.location.createChatLink(var1, this.metadata.description);
   }

   public PackCompatibility getCompatibility() {
      return this.metadata.compatibility();
   }

   public FeatureFlagSet getRequestedFeatures() {
      return this.metadata.requestedFeatures();
   }

   public PackResources open() {
      return this.resources.openFull(this.location, this.metadata);
   }

   public String getId() {
      return this.location.id();
   }

   public PackSelectionConfig selectionConfig() {
      return this.selectionConfig;
   }

   public boolean isRequired() {
      return this.selectionConfig.required();
   }

   public boolean isFixedPosition() {
      return this.selectionConfig.fixedPosition();
   }

   public Pack.Position getDefaultPosition() {
      return this.selectionConfig.defaultPosition();
   }

   public PackSource getPackSource() {
      return this.location.source();
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof Pack)) {
         return false;
      } else {
         Pack var2 = (Pack)var1;
         return this.location.equals(var2.location);
      }
   }

   public int hashCode() {
      return this.location.hashCode();
   }

   public interface ResourcesSupplier {
      PackResources openPrimary(PackLocationInfo var1);

      PackResources openFull(PackLocationInfo var1, Pack.Metadata var2);
   }

   public static record Metadata(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays) {
      final Component description;

      public Metadata(Component param1, PackCompatibility param2, FeatureFlagSet param3, List<String> param4) {
         super();
         this.description = var1;
         this.compatibility = var2;
         this.requestedFeatures = var3;
         this.overlays = var4;
      }

      public Component description() {
         return this.description;
      }

      public PackCompatibility compatibility() {
         return this.compatibility;
      }

      public FeatureFlagSet requestedFeatures() {
         return this.requestedFeatures;
      }

      public List<String> overlays() {
         return this.overlays;
      }
   }

   public static enum Position {
      TOP,
      BOTTOM;

      private Position() {
      }

      public <T> int insert(List<T> var1, T var2, Function<T, PackSelectionConfig> var3, boolean var4) {
         Pack.Position var5 = var4 ? this.opposite() : this;
         int var6;
         PackSelectionConfig var7;
         if (var5 == BOTTOM) {
            for(var6 = 0; var6 < var1.size(); ++var6) {
               var7 = (PackSelectionConfig)var3.apply(var1.get(var6));
               if (!var7.fixedPosition() || var7.defaultPosition() != this) {
                  break;
               }
            }

            var1.add(var6, var2);
            return var6;
         } else {
            for(var6 = var1.size() - 1; var6 >= 0; --var6) {
               var7 = (PackSelectionConfig)var3.apply(var1.get(var6));
               if (!var7.fixedPosition() || var7.defaultPosition() != this) {
                  break;
               }
            }

            var1.add(var6 + 1, var2);
            return var6 + 1;
         }
      }

      public Pack.Position opposite() {
         return this == TOP ? BOTTOM : TOP;
      }

      // $FF: synthetic method
      private static Pack.Position[] $values() {
         return new Pack.Position[]{TOP, BOTTOM};
      }
   }
}
