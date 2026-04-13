package net.minecraft.world.waypoints;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint implements Waypoint {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
   protected final Either<UUID, String> identifier;
   private final Waypoint.Icon icon;
   private final TrackedWaypoint.Type type;

   TrackedWaypoint(Either<UUID, String> var1, Waypoint.Icon var2, TrackedWaypoint.Type var3) {
      super();
      this.identifier = var1;
      this.icon = var2;
      this.type = var3;
   }

   public Either<UUID, String> id() {
      return this.identifier;
   }

   public abstract void update(TrackedWaypoint var1);

   public void write(ByteBuf var1) {
      FriendlyByteBuf var2 = new FriendlyByteBuf(var1);
      var2.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
      Waypoint.Icon.STREAM_CODEC.encode(var2, this.icon);
      var2.writeEnum(this.type);
      this.writeContents(var1);
   }

   public abstract void writeContents(ByteBuf var1);

   private static TrackedWaypoint read(ByteBuf var0) {
      FriendlyByteBuf var1 = new FriendlyByteBuf(var0);
      Either var2 = var1.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
      Waypoint.Icon var3 = (Waypoint.Icon)Waypoint.Icon.STREAM_CODEC.decode(var1);
      TrackedWaypoint.Type var4 = (TrackedWaypoint.Type)var1.readEnum(TrackedWaypoint.Type.class);
      return (TrackedWaypoint)var4.constructor.apply(var2, var3, var1);
   }

   public static TrackedWaypoint setPosition(UUID var0, Waypoint.Icon var1, Vec3i var2) {
      return new TrackedWaypoint.Vec3iWaypoint(var0, var1, var2);
   }

   public static TrackedWaypoint setChunk(UUID var0, Waypoint.Icon var1, ChunkPos var2) {
      return new TrackedWaypoint.ChunkWaypoint(var0, var1, var2);
   }

   public static TrackedWaypoint setAzimuth(UUID var0, Waypoint.Icon var1, float var2) {
      return new TrackedWaypoint.AzimuthWaypoint(var0, var1, var2);
   }

   public static TrackedWaypoint empty(UUID var0) {
      return new TrackedWaypoint.EmptyWaypoint(var0);
   }

   public abstract double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3);

   public abstract TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3);

   public abstract double distanceSquared(Entity var1);

   public Waypoint.Icon icon() {
      return this.icon;
   }

   private static enum Type {
      EMPTY(TrackedWaypoint.EmptyWaypoint::new),
      VEC3I(TrackedWaypoint.Vec3iWaypoint::new),
      CHUNK(TrackedWaypoint.ChunkWaypoint::new),
      AZIMUTH(TrackedWaypoint.AzimuthWaypoint::new);

      final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

      private Type(final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> param3) {
         this.constructor = var3;
      }

      // $FF: synthetic method
      private static TrackedWaypoint.Type[] $values() {
         return new TrackedWaypoint.Type[]{EMPTY, VEC3I, CHUNK, AZIMUTH};
      }
   }

   private static class Vec3iWaypoint extends TrackedWaypoint {
      private Vec3i vector;

      public Vec3iWaypoint(UUID var1, Waypoint.Icon var2, Vec3i var3) {
         super(Either.left(var1), var2, TrackedWaypoint.Type.VEC3I);
         this.vector = var3;
      }

      public Vec3iWaypoint(Either<UUID, String> var1, Waypoint.Icon var2, FriendlyByteBuf var3) {
         super(var1, var2, TrackedWaypoint.Type.VEC3I);
         this.vector = new Vec3i(var3.readVarInt(), var3.readVarInt(), var3.readVarInt());
      }

      public void update(TrackedWaypoint var1) {
         if (var1 instanceof TrackedWaypoint.Vec3iWaypoint) {
            TrackedWaypoint.Vec3iWaypoint var2 = (TrackedWaypoint.Vec3iWaypoint)var1;
            this.vector = var2.vector;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", var1.getClass());
         }

      }

      public void writeContents(ByteBuf var1) {
         VarInt.write(var1, this.vector.getX());
         VarInt.write(var1, this.vector.getY());
         VarInt.write(var1, this.vector.getZ());
      }

      private Vec3 position(Level var1, PartialTickSupplier var2) {
         Optional var10000 = this.identifier.left();
         Objects.requireNonNull(var1);
         return (Vec3)var10000.map(var1::getEntity).map((var2x) -> {
            return var2x.blockPosition().distManhattan(this.vector) > 3 ? null : var2x.getEyePosition(var2.apply(var2x));
         }).orElseGet(() -> {
            return Vec3.atCenterOf(this.vector);
         });
      }

      public double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3) {
         Vec3 var4 = var2.position().subtract(this.position(var1, var3)).rotateClockwise90();
         float var5 = (float)Mth.atan2(var4.z(), var4.x()) * 57.295776F;
         return (double)Mth.degreesDifference(var2.yaw(), var5);
      }

      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3) {
         Vec3 var4 = var2.projectPointToScreen(this.position(var1, var3));
         boolean var5 = var4.z > 1.0D;
         double var6 = var5 ? -var4.y : var4.y;
         if (var6 < -1.0D) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else if (var6 > 1.0D) {
            return TrackedWaypoint.PitchDirection.UP;
         } else {
            if (var5) {
               if (var4.y > 0.0D) {
                  return TrackedWaypoint.PitchDirection.UP;
               }

               if (var4.y < 0.0D) {
                  return TrackedWaypoint.PitchDirection.DOWN;
               }
            }

            return TrackedWaypoint.PitchDirection.NONE;
         }
      }

      public double distanceSquared(Entity var1) {
         return var1.distanceToSqr(Vec3.atCenterOf(this.vector));
      }
   }

   private static class ChunkWaypoint extends TrackedWaypoint {
      private ChunkPos chunkPos;

      public ChunkWaypoint(UUID var1, Waypoint.Icon var2, ChunkPos var3) {
         super(Either.left(var1), var2, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = var3;
      }

      public ChunkWaypoint(Either<UUID, String> var1, Waypoint.Icon var2, FriendlyByteBuf var3) {
         super(var1, var2, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = new ChunkPos(var3.readVarInt(), var3.readVarInt());
      }

      public void update(TrackedWaypoint var1) {
         if (var1 instanceof TrackedWaypoint.ChunkWaypoint) {
            TrackedWaypoint.ChunkWaypoint var2 = (TrackedWaypoint.ChunkWaypoint)var1;
            this.chunkPos = var2.chunkPos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", var1.getClass());
         }

      }

      public void writeContents(ByteBuf var1) {
         VarInt.write(var1, this.chunkPos.x);
         VarInt.write(var1, this.chunkPos.z);
      }

      private Vec3 position(double var1) {
         return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)var1));
      }

      public double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3) {
         Vec3 var4 = var2.position();
         Vec3 var5 = var4.subtract(this.position(var4.y())).rotateClockwise90();
         float var6 = (float)Mth.atan2(var5.z(), var5.x()) * 57.295776F;
         return (double)Mth.degreesDifference(var2.yaw(), var6);
      }

      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3) {
         double var4 = var2.projectHorizonToScreen();
         if (var4 < -1.0D) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return var4 > 1.0D ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      public double distanceSquared(Entity var1) {
         return var1.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(var1.getBlockY())));
      }
   }

   private static class AzimuthWaypoint extends TrackedWaypoint {
      private float angle;

      public AzimuthWaypoint(UUID var1, Waypoint.Icon var2, float var3) {
         super(Either.left(var1), var2, TrackedWaypoint.Type.AZIMUTH);
         this.angle = var3;
      }

      public AzimuthWaypoint(Either<UUID, String> var1, Waypoint.Icon var2, FriendlyByteBuf var3) {
         super(var1, var2, TrackedWaypoint.Type.AZIMUTH);
         this.angle = var3.readFloat();
      }

      public void update(TrackedWaypoint var1) {
         if (var1 instanceof TrackedWaypoint.AzimuthWaypoint) {
            TrackedWaypoint.AzimuthWaypoint var2 = (TrackedWaypoint.AzimuthWaypoint)var1;
            this.angle = var2.angle;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", var1.getClass());
         }

      }

      public void writeContents(ByteBuf var1) {
         var1.writeFloat(this.angle);
      }

      public double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3) {
         return (double)Mth.degreesDifference(var2.yaw(), this.angle * 57.295776F);
      }

      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3) {
         double var4 = var2.projectHorizonToScreen();
         if (var4 < -1.0D) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return var4 > 1.0D ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      public double distanceSquared(Entity var1) {
         return 1.0D / 0.0;
      }
   }

   static class EmptyWaypoint extends TrackedWaypoint {
      private EmptyWaypoint(Either<UUID, String> var1, Waypoint.Icon var2, FriendlyByteBuf var3) {
         super(var1, var2, TrackedWaypoint.Type.EMPTY);
      }

      EmptyWaypoint(UUID var1) {
         super(Either.left(var1), Waypoint.Icon.NULL, TrackedWaypoint.Type.EMPTY);
      }

      public void update(TrackedWaypoint var1) {
      }

      public void writeContents(ByteBuf var1) {
      }

      public double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3) {
         return 0.0D / 0.0;
      }

      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3) {
         return TrackedWaypoint.PitchDirection.NONE;
      }

      public double distanceSquared(Entity var1) {
         return 1.0D / 0.0;
      }
   }

   public interface Camera {
      float yaw();

      Vec3 position();
   }

   public interface Projector {
      Vec3 projectPointToScreen(Vec3 var1);

      double projectHorizonToScreen();
   }

   public static enum PitchDirection {
      NONE,
      UP,
      DOWN;

      private PitchDirection() {
      }

      // $FF: synthetic method
      private static TrackedWaypoint.PitchDirection[] $values() {
         return new TrackedWaypoint.PitchDirection[]{NONE, UP, DOWN};
      }
   }
}
