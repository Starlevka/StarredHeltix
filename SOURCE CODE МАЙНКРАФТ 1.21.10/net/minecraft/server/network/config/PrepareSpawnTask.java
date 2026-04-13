package net.minecraft.server.network.config;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class PrepareSpawnTask implements ConfigurationTask {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("prepare_spawn");
   public static final int PREPARE_CHUNK_RADIUS = 3;
   final MinecraftServer server;
   final NameAndId nameAndId;
   final LevelLoadListener loadListener;
   @Nullable
   private PrepareSpawnTask.State state;

   public PrepareSpawnTask(MinecraftServer var1, NameAndId var2) {
      super();
      this.server = var1;
      this.nameAndId = var2;
      this.loadListener = var1.getLevelLoadListener();
   }

   public void start(Consumer<Packet<?>> var1) {
      ProblemReporter.ScopedCollector var2 = new ProblemReporter.ScopedCollector(LOGGER);

      try {
         Optional var3 = this.server.getPlayerList().loadPlayerData(this.nameAndId).map((var2x) -> {
            return TagValueInput.create(var2, this.server.registryAccess(), (CompoundTag)var2x);
         });
         ServerPlayer.SavedPosition var4 = (ServerPlayer.SavedPosition)var3.flatMap((var0) -> {
            return var0.read(ServerPlayer.SavedPosition.MAP_CODEC);
         }).orElse(ServerPlayer.SavedPosition.EMPTY);
         LevelData.RespawnData var5 = this.server.getWorldData().overworldData().getRespawnData();
         Optional var10000 = var4.dimension();
         MinecraftServer var10001 = this.server;
         Objects.requireNonNull(var10001);
         ServerLevel var6 = (ServerLevel)var10000.map(var10001::getLevel).orElseGet(() -> {
            ServerLevel var2 = this.server.getLevel(var5.dimension());
            return var2 != null ? var2 : this.server.overworld();
         });
         CompletableFuture var7 = (CompletableFuture)var4.position().map(CompletableFuture::completedFuture).orElseGet(() -> {
            return PlayerSpawnFinder.findSpawn(var6, var5.pos());
         });
         Vec2 var8 = (Vec2)var4.rotation().orElse(new Vec2(var5.yaw(), var5.pitch()));
         this.state = new PrepareSpawnTask.Preparing(var6, var7, var8);
      } catch (Throwable var10) {
         try {
            var2.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      var2.close();
   }

   public boolean tick() {
      PrepareSpawnTask.State var1 = this.state;
      byte var2 = 0;
      boolean var10000;
      PrepareSpawnTask.Ready var4;
      switch(var1.typeSwitch<invokedynamic>(var1, var2)) {
      case -1:
         var10000 = false;
         break;
      case 0:
         PrepareSpawnTask.Preparing var3 = (PrepareSpawnTask.Preparing)var1;
         var4 = var3.tick();
         if (var4 != null) {
            this.state = var4;
            var10000 = true;
         } else {
            var10000 = false;
         }
         break;
      case 1:
         var4 = (PrepareSpawnTask.Ready)var1;
         var10000 = true;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ServerPlayer spawnPlayer(Connection var1, CommonListenerCookie var2) {
      PrepareSpawnTask.State var4 = this.state;
      if (var4 instanceof PrepareSpawnTask.Ready) {
         PrepareSpawnTask.Ready var3 = (PrepareSpawnTask.Ready)var4;
         return var3.spawn(var1, var2);
      } else {
         throw new IllegalStateException("Player spawn was not ready");
      }
   }

   public void keepAlive() {
      PrepareSpawnTask.State var2 = this.state;
      if (var2 instanceof PrepareSpawnTask.Ready) {
         PrepareSpawnTask.Ready var1 = (PrepareSpawnTask.Ready)var2;
         var1.keepAlive();
      }

   }

   public void close() {
      PrepareSpawnTask.State var2 = this.state;
      if (var2 instanceof PrepareSpawnTask.Preparing) {
         PrepareSpawnTask.Preparing var1 = (PrepareSpawnTask.Preparing)var2;
         var1.cancel();
      }

      this.state = null;
   }

   public ConfigurationTask.Type type() {
      return TYPE;
   }

   private final class Preparing implements PrepareSpawnTask.State {
      private final ServerLevel spawnLevel;
      private final CompletableFuture<Vec3> spawnPosition;
      private final Vec2 spawnAngle;
      @Nullable
      private CompletableFuture<?> chunkLoadFuture;
      private final ChunkLoadCounter chunkLoadCounter = new ChunkLoadCounter();

      Preparing(final ServerLevel param2, final CompletableFuture<Vec3> param3, final Vec2 param4) {
         super();
         this.spawnLevel = var2;
         this.spawnPosition = var3;
         this.spawnAngle = var4;
      }

      public void cancel() {
         this.spawnPosition.cancel(false);
      }

      @Nullable
      public PrepareSpawnTask.Ready tick() {
         if (!this.spawnPosition.isDone()) {
            return null;
         } else {
            Vec3 var1 = (Vec3)this.spawnPosition.join();
            if (this.chunkLoadFuture == null) {
               ChunkPos var2 = new ChunkPos(BlockPos.containing(var1));
               this.chunkLoadCounter.track(this.spawnLevel, () -> {
                  this.chunkLoadFuture = this.spawnLevel.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN, var2, 3);
               });
               PrepareSpawnTask.this.loadListener.start(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS, this.chunkLoadCounter.totalChunks());
               PrepareSpawnTask.this.loadListener.updateFocus(this.spawnLevel.dimension(), var2);
            }

            PrepareSpawnTask.this.loadListener.update(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS, this.chunkLoadCounter.readyChunks(), this.chunkLoadCounter.totalChunks());
            if (!this.chunkLoadFuture.isDone()) {
               return null;
            } else {
               PrepareSpawnTask.this.loadListener.finish(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS);
               return PrepareSpawnTask.this.new Ready(this.spawnLevel, var1, this.spawnAngle);
            }
         }
      }
   }

   private interface State {
   }

   final class Ready implements PrepareSpawnTask.State {
      private final ServerLevel spawnLevel;
      private final Vec3 spawnPosition;
      private final Vec2 spawnAngle;

      Ready(final ServerLevel param2, final Vec3 param3, final Vec2 param4) {
         super();
         this.spawnLevel = var2;
         this.spawnPosition = var3;
         this.spawnAngle = var4;
      }

      public void keepAlive() {
         this.spawnLevel.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, new ChunkPos(BlockPos.containing(this.spawnPosition)), 3);
      }

      public ServerPlayer spawn(Connection var1, CommonListenerCookie var2) {
         ChunkPos var3 = new ChunkPos(BlockPos.containing(this.spawnPosition));
         this.spawnLevel.waitForEntities(var3, 3);
         ServerPlayer var4 = new ServerPlayer(PrepareSpawnTask.this.server, this.spawnLevel, var2.gameProfile(), var2.clientInformation());
         ProblemReporter.ScopedCollector var5 = new ProblemReporter.ScopedCollector(var4.problemPath(), PrepareSpawnTask.LOGGER);

         ServerPlayer var7;
         try {
            Optional var6 = PrepareSpawnTask.this.server.getPlayerList().loadPlayerData(PrepareSpawnTask.this.nameAndId).map((var2x) -> {
               return TagValueInput.create(var5, PrepareSpawnTask.this.server.registryAccess(), (CompoundTag)var2x);
            });
            Objects.requireNonNull(var4);
            var6.ifPresent(var4::load);
            var4.snapTo(this.spawnPosition, this.spawnAngle.x, this.spawnAngle.y);
            PrepareSpawnTask.this.server.getPlayerList().placeNewPlayer(var1, var4, var2);
            var6.ifPresent((var1x) -> {
               var4.loadAndSpawnEnderPearls(var1x);
               var4.loadAndSpawnParentVehicle(var1x);
            });
            var7 = var4;
         } catch (Throwable var9) {
            try {
               var5.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         var5.close();
         return var7;
      }
   }
}
