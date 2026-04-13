package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder extends SavedData {
   public static final double MAX_SIZE = 5.9999968E7D;
   public static final double MAX_CENTER_COORDINATE = 2.9999984E7D;
   public static final Codec<WorldBorder> CODEC;
   public static final SavedDataType<WorldBorder> TYPE;
   private final List<BorderChangeListener> listeners = Lists.newArrayList();
   double damagePerBlock = 0.2D;
   double safeZone = 5.0D;
   int warningTime = 15;
   int warningBlocks = 5;
   double centerX;
   double centerZ;
   int absoluteMaxSize = 29999984;
   WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.9999968E7D);

   public WorldBorder() {
      super();
   }

   public boolean isWithinBounds(BlockPos var1) {
      return this.isWithinBounds((double)var1.getX(), (double)var1.getZ());
   }

   public boolean isWithinBounds(Vec3 var1) {
      return this.isWithinBounds(var1.x, var1.z);
   }

   public boolean isWithinBounds(ChunkPos var1) {
      return this.isWithinBounds((double)var1.getMinBlockX(), (double)var1.getMinBlockZ()) && this.isWithinBounds((double)var1.getMaxBlockX(), (double)var1.getMaxBlockZ());
   }

   public boolean isWithinBounds(AABB var1) {
      return this.isWithinBounds(var1.minX, var1.minZ, var1.maxX - 9.999999747378752E-6D, var1.maxZ - 9.999999747378752E-6D);
   }

   private boolean isWithinBounds(double var1, double var3, double var5, double var7) {
      return this.isWithinBounds(var1, var3) && this.isWithinBounds(var5, var7);
   }

   public boolean isWithinBounds(double var1, double var3) {
      return this.isWithinBounds(var1, var3, 0.0D);
   }

   public boolean isWithinBounds(double var1, double var3, double var5) {
      return var1 >= this.getMinX() - var5 && var1 < this.getMaxX() + var5 && var3 >= this.getMinZ() - var5 && var3 < this.getMaxZ() + var5;
   }

   public BlockPos clampToBounds(BlockPos var1) {
      return this.clampToBounds((double)var1.getX(), (double)var1.getY(), (double)var1.getZ());
   }

   public BlockPos clampToBounds(Vec3 var1) {
      return this.clampToBounds(var1.x(), var1.y(), var1.z());
   }

   public BlockPos clampToBounds(double var1, double var3, double var5) {
      return BlockPos.containing(this.clampVec3ToBound(var1, var3, var5));
   }

   public Vec3 clampVec3ToBound(Vec3 var1) {
      return this.clampVec3ToBound(var1.x, var1.y, var1.z);
   }

   public Vec3 clampVec3ToBound(double var1, double var3, double var5) {
      return new Vec3(Mth.clamp(var1, this.getMinX(), this.getMaxX() - 9.999999747378752E-6D), var3, Mth.clamp(var5, this.getMinZ(), this.getMaxZ() - 9.999999747378752E-6D));
   }

   public double getDistanceToBorder(Entity var1) {
      return this.getDistanceToBorder(var1.getX(), var1.getZ());
   }

   public VoxelShape getCollisionShape() {
      return this.extent.getCollisionShape();
   }

   public double getDistanceToBorder(double var1, double var3) {
      double var5 = var3 - this.getMinZ();
      double var7 = this.getMaxZ() - var3;
      double var9 = var1 - this.getMinX();
      double var11 = this.getMaxX() - var1;
      double var13 = Math.min(var9, var11);
      var13 = Math.min(var13, var5);
      return Math.min(var13, var7);
   }

   public boolean isInsideCloseToBorder(Entity var1, AABB var2) {
      double var3 = Math.max(Mth.absMax(var2.getXsize(), var2.getZsize()), 1.0D);
      return this.getDistanceToBorder(var1) < var3 * 2.0D && this.isWithinBounds(var1.getX(), var1.getZ(), var3);
   }

   public BorderStatus getStatus() {
      return this.extent.getStatus();
   }

   public double getMinX() {
      return this.extent.getMinX();
   }

   public double getMinZ() {
      return this.extent.getMinZ();
   }

   public double getMaxX() {
      return this.extent.getMaxX();
   }

   public double getMaxZ() {
      return this.extent.getMaxZ();
   }

   public double getCenterX() {
      return this.centerX;
   }

   public double getCenterZ() {
      return this.centerZ;
   }

   public void setCenter(double var1, double var3) {
      this.centerX = var1;
      this.centerZ = var3;
      this.extent.onCenterChange();
      this.setDirty();
      Iterator var5 = this.getListeners().iterator();

      while(var5.hasNext()) {
         BorderChangeListener var6 = (BorderChangeListener)var5.next();
         var6.onSetCenter(this, var1, var3);
      }

   }

   public double getSize() {
      return this.extent.getSize();
   }

   public long getLerpTime() {
      return this.extent.getLerpTime();
   }

   public double getLerpTarget() {
      return this.extent.getLerpTarget();
   }

   public void setSize(double var1) {
      this.extent = new WorldBorder.StaticBorderExtent(var1);
      this.setDirty();
      Iterator var3 = this.getListeners().iterator();

      while(var3.hasNext()) {
         BorderChangeListener var4 = (BorderChangeListener)var3.next();
         var4.onSetSize(this, var1);
      }

   }

   public void lerpSizeBetween(double var1, double var3, long var5) {
      this.extent = (WorldBorder.BorderExtent)(var1 == var3 ? new WorldBorder.StaticBorderExtent(var3) : new WorldBorder.MovingBorderExtent(var1, var3, var5));
      this.setDirty();
      Iterator var7 = this.getListeners().iterator();

      while(var7.hasNext()) {
         BorderChangeListener var8 = (BorderChangeListener)var7.next();
         var8.onLerpSize(this, var1, var3, var5);
      }

   }

   protected List<BorderChangeListener> getListeners() {
      return Lists.newArrayList(this.listeners);
   }

   public void addListener(BorderChangeListener var1) {
      this.listeners.add(var1);
   }

   public void removeListener(BorderChangeListener var1) {
      this.listeners.remove(var1);
   }

   public void setAbsoluteMaxSize(int var1) {
      this.absoluteMaxSize = var1;
      this.extent.onAbsoluteMaxSizeChange();
   }

   public int getAbsoluteMaxSize() {
      return this.absoluteMaxSize;
   }

   public double getSafeZone() {
      return this.safeZone;
   }

   public void setSafeZone(double var1) {
      this.safeZone = var1;
      this.setDirty();
      Iterator var3 = this.getListeners().iterator();

      while(var3.hasNext()) {
         BorderChangeListener var4 = (BorderChangeListener)var3.next();
         var4.onSetSafeZone(this, var1);
      }

   }

   public double getDamagePerBlock() {
      return this.damagePerBlock;
   }

   public void setDamagePerBlock(double var1) {
      this.damagePerBlock = var1;
      this.setDirty();
      Iterator var3 = this.getListeners().iterator();

      while(var3.hasNext()) {
         BorderChangeListener var4 = (BorderChangeListener)var3.next();
         var4.onSetDamagePerBlock(this, var1);
      }

   }

   public double getLerpSpeed() {
      return this.extent.getLerpSpeed();
   }

   public int getWarningTime() {
      return this.warningTime;
   }

   public void setWarningTime(int var1) {
      this.warningTime = var1;
      this.setDirty();
      Iterator var2 = this.getListeners().iterator();

      while(var2.hasNext()) {
         BorderChangeListener var3 = (BorderChangeListener)var2.next();
         var3.onSetWarningTime(this, var1);
      }

   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }

   public void setWarningBlocks(int var1) {
      this.warningBlocks = var1;
      this.setDirty();
      Iterator var2 = this.getListeners().iterator();

      while(var2.hasNext()) {
         BorderChangeListener var3 = (BorderChangeListener)var2.next();
         var3.onSetWarningBlocks(this, var1);
      }

   }

   public void tick() {
      this.extent = this.extent.update();
   }

   public void applySettings(WorldBorder.Settings var1) {
      this.setCenter(var1.centerX(), var1.centerZ());
      this.setDamagePerBlock(var1.damagePerBlock());
      this.setSafeZone(var1.safeZone());
      this.setWarningBlocks(var1.warningBlocks());
      this.setWarningTime(var1.warningTime());
      if (var1.lerpTime() > 0L) {
         this.lerpSizeBetween(var1.size(), var1.lerpTarget(), var1.lerpTime());
      } else {
         this.setSize(var1.size());
      }

   }

   static {
      CODEC = WorldBorder.Settings.CODEC.xmap(WorldBorder.Settings::toWorldBorder, WorldBorder.Settings::new);
      TYPE = new SavedDataType("world_border", (var0) -> {
         return WorldBorder.Settings.DEFAULT.toWorldBorder();
      }, (var0) -> {
         return CODEC;
      }, DataFixTypes.SAVED_DATA_WORLD_BORDER);
   }

   private class StaticBorderExtent implements WorldBorder.BorderExtent {
      private final double size;
      private double minX;
      private double minZ;
      private double maxX;
      private double maxZ;
      private VoxelShape shape;

      public StaticBorderExtent(final double param2) {
         super();
         this.size = var2;
         this.updateBox();
      }

      public double getMinX() {
         return this.minX;
      }

      public double getMaxX() {
         return this.maxX;
      }

      public double getMinZ() {
         return this.minZ;
      }

      public double getMaxZ() {
         return this.maxZ;
      }

      public double getSize() {
         return this.size;
      }

      public BorderStatus getStatus() {
         return BorderStatus.STATIONARY;
      }

      public double getLerpSpeed() {
         return 0.0D;
      }

      public long getLerpTime() {
         return 0L;
      }

      public double getLerpTarget() {
         return this.size;
      }

      private void updateBox() {
         this.minX = Mth.clamp(WorldBorder.this.getCenterX() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.minZ = Mth.clamp(WorldBorder.this.getCenterZ() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.maxX = Mth.clamp(WorldBorder.this.getCenterX() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.maxZ = Mth.clamp(WorldBorder.this.getCenterZ() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.shape = Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), -1.0D / 0.0, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), 1.0D / 0.0, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
      }

      public void onAbsoluteMaxSizeChange() {
         this.updateBox();
      }

      public void onCenterChange() {
         this.updateBox();
      }

      public WorldBorder.BorderExtent update() {
         return this;
      }

      public VoxelShape getCollisionShape() {
         return this.shape;
      }
   }

   private interface BorderExtent {
      double getMinX();

      double getMaxX();

      double getMinZ();

      double getMaxZ();

      double getSize();

      double getLerpSpeed();

      long getLerpTime();

      double getLerpTarget();

      BorderStatus getStatus();

      void onAbsoluteMaxSizeChange();

      void onCenterChange();

      WorldBorder.BorderExtent update();

      VoxelShape getCollisionShape();
   }

   class MovingBorderExtent implements WorldBorder.BorderExtent {
      private final double from;
      private final double to;
      private final long lerpEnd;
      private final long lerpBegin;
      private final double lerpDuration;

      MovingBorderExtent(final double param2, final double param4, final long param6) {
         super();
         this.from = var2;
         this.to = var4;
         this.lerpDuration = (double)var6;
         this.lerpBegin = Util.getMillis();
         this.lerpEnd = this.lerpBegin + var6;
      }

      public double getMinX() {
         return Mth.clamp(WorldBorder.this.getCenterX() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMinZ() {
         return Mth.clamp(WorldBorder.this.getCenterZ() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMaxX() {
         return Mth.clamp(WorldBorder.this.getCenterX() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMaxZ() {
         return Mth.clamp(WorldBorder.this.getCenterZ() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getSize() {
         double var1 = (double)(Util.getMillis() - this.lerpBegin) / this.lerpDuration;
         return var1 < 1.0D ? Mth.lerp(var1, this.from, this.to) : this.to;
      }

      public double getLerpSpeed() {
         return Math.abs(this.from - this.to) / (double)(this.lerpEnd - this.lerpBegin);
      }

      public long getLerpTime() {
         return this.lerpEnd - Util.getMillis();
      }

      public double getLerpTarget() {
         return this.to;
      }

      public BorderStatus getStatus() {
         return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
      }

      public void onCenterChange() {
      }

      public void onAbsoluteMaxSizeChange() {
      }

      public WorldBorder.BorderExtent update() {
         if (this.getLerpTime() <= 0L) {
            WorldBorder.this.setDirty();
            return WorldBorder.this.new StaticBorderExtent(this.to);
         } else {
            return this;
         }
      }

      public VoxelShape getCollisionShape() {
         return Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), -1.0D / 0.0, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), 1.0D / 0.0, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
      }
   }

   public static record Settings(double centerX, double centerZ, double damagePerBlock, double safeZone, int warningBlocks, int warningTime, double size, long lerpTime, double lerpTarget) {
      public static final WorldBorder.Settings DEFAULT = new WorldBorder.Settings(0.0D, 0.0D, 0.2D, 5.0D, 5, 15, 5.9999968E7D, 0L, 0.0D);
      public static final Codec<WorldBorder.Settings> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.doubleRange(-2.9999984E7D, 2.9999984E7D).fieldOf("center_x").forGetter(WorldBorder.Settings::centerX), Codec.doubleRange(-2.9999984E7D, 2.9999984E7D).fieldOf("center_z").forGetter(WorldBorder.Settings::centerZ), Codec.DOUBLE.fieldOf("damage_per_block").forGetter(WorldBorder.Settings::damagePerBlock), Codec.DOUBLE.fieldOf("safe_zone").forGetter(WorldBorder.Settings::safeZone), Codec.INT.fieldOf("warning_blocks").forGetter(WorldBorder.Settings::warningBlocks), Codec.INT.fieldOf("warning_time").forGetter(WorldBorder.Settings::warningTime), Codec.DOUBLE.fieldOf("size").forGetter(WorldBorder.Settings::size), Codec.LONG.fieldOf("lerp_time").forGetter(WorldBorder.Settings::lerpTime), Codec.DOUBLE.fieldOf("lerp_target").forGetter(WorldBorder.Settings::lerpTarget)).apply(var0, WorldBorder.Settings::new);
      });

      public Settings(WorldBorder var1) {
         this(var1.centerX, var1.centerZ, var1.damagePerBlock, var1.safeZone, var1.warningBlocks, var1.warningTime, var1.extent.getSize(), var1.extent.getLerpTime(), var1.extent.getLerpTarget());
      }

      public Settings(double param1, double param3, double param5, double param7, int param9, int param10, double param11, long param13, double param15) {
         super();
         this.centerX = var1;
         this.centerZ = var3;
         this.damagePerBlock = var5;
         this.safeZone = var7;
         this.warningBlocks = var9;
         this.warningTime = var10;
         this.size = var11;
         this.lerpTime = var13;
         this.lerpTarget = var15;
      }

      public WorldBorder toWorldBorder() {
         WorldBorder var1 = new WorldBorder();
         var1.applySettings(this);
         return var1;
      }

      public double centerX() {
         return this.centerX;
      }

      public double centerZ() {
         return this.centerZ;
      }

      public double damagePerBlock() {
         return this.damagePerBlock;
      }

      public double safeZone() {
         return this.safeZone;
      }

      public int warningBlocks() {
         return this.warningBlocks;
      }

      public int warningTime() {
         return this.warningTime;
      }

      public double size() {
         return this.size;
      }

      public long lerpTime() {
         return this.lerpTime;
      }

      public double lerpTarget() {
         return this.lerpTarget;
      }
   }
}
