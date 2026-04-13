package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LevelLoadTracker implements LevelLoadListener {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final long CLIENT_WAIT_TIMEOUT_MS;
   public static final long LEVEL_LOAD_CLOSE_DELAY_MS = 500L;
   private final LevelLoadProgressTracker serverProgressTracker;
   @Nullable
   private ChunkLoadStatusView serverChunkStatusView;
   @Nullable
   private volatile LevelLoadListener.Stage serverStage;
   @Nullable
   private LevelLoadTracker.ClientState clientState;
   private final long closeDelayMs;

   public LevelLoadTracker() {
      this(0L);
   }

   public LevelLoadTracker(long var1) {
      super();
      this.serverProgressTracker = new LevelLoadProgressTracker(true);
      this.closeDelayMs = var1;
   }

   public void setServerChunkStatusView(ChunkLoadStatusView var1) {
      this.serverChunkStatusView = var1;
   }

   public void startClientLoad(LocalPlayer var1, ClientLevel var2, LevelRenderer var3) {
      this.clientState = new LevelLoadTracker.WaitingForServer(var1, var2, var3, Util.getMillis() + CLIENT_WAIT_TIMEOUT_MS);
   }

   public void tickClientLoad() {
      if (this.clientState != null) {
         this.clientState = this.clientState.tick();
      }

   }

   public boolean isLevelReady() {
      LevelLoadTracker.ClientState var4 = this.clientState;
      boolean var9;
      if (var4 instanceof LevelLoadTracker.ClientLevelReady) {
         LevelLoadTracker.ClientLevelReady var3 = (LevelLoadTracker.ClientLevelReady)var4;
         LevelLoadTracker.ClientLevelReady var10000 = var3;

         long var8;
         try {
            var8 = var10000.readyAt();
         } catch (Throwable var7) {
            throw new MatchException(var7.toString(), var7);
         }

         long var5 = var8;
         if (Util.getMillis() >= var5 + this.closeDelayMs) {
            var9 = true;
            return var9;
         }
      }

      var9 = false;
      return var9;
   }

   public void loadingPacketsReceived() {
      if (this.clientState != null) {
         this.clientState = this.clientState.loadingPacketsReceived();
      }

   }

   public void start(LevelLoadListener.Stage var1, int var2) {
      this.serverProgressTracker.start(var1, var2);
      this.serverStage = var1;
   }

   public void update(LevelLoadListener.Stage var1, int var2, int var3) {
      this.serverProgressTracker.update(var1, var2, var3);
   }

   public void finish(LevelLoadListener.Stage var1) {
      this.serverProgressTracker.finish(var1);
   }

   public void updateFocus(ResourceKey<Level> var1, ChunkPos var2) {
      if (this.serverChunkStatusView != null) {
         this.serverChunkStatusView.moveTo(var1, var2);
      }

   }

   @Nullable
   public ChunkLoadStatusView statusView() {
      return this.serverChunkStatusView;
   }

   public float serverProgress() {
      return this.serverProgressTracker.get();
   }

   public boolean hasProgress() {
      return this.serverStage != null;
   }

   static {
      CLIENT_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30L);
   }

   private static record WaitingForServer(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
      WaitingForServer(LocalPlayer param1, ClientLevel param2, LevelRenderer param3, long param4) {
         super();
         this.player = var1;
         this.level = var2;
         this.levelRenderer = var3;
         this.timeoutAfter = var4;
      }

      public LevelLoadTracker.ClientState loadingPacketsReceived() {
         return new LevelLoadTracker.WaitingForPlayerChunk(this.player, this.level, this.levelRenderer, this.timeoutAfter);
      }

      public LocalPlayer player() {
         return this.player;
      }

      public ClientLevel level() {
         return this.level;
      }

      public LevelRenderer levelRenderer() {
         return this.levelRenderer;
      }

      public long timeoutAfter() {
         return this.timeoutAfter;
      }
   }

   interface ClientState {
      default LevelLoadTracker.ClientState tick() {
         return this;
      }

      default LevelLoadTracker.ClientState loadingPacketsReceived() {
         return this;
      }
   }

   private static record ClientLevelReady(long readyAt) implements LevelLoadTracker.ClientState {
      ClientLevelReady(long param1) {
         super();
         this.readyAt = var1;
      }

      public long readyAt() {
         return this.readyAt;
      }
   }

   private static record WaitingForPlayerChunk(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
      WaitingForPlayerChunk(LocalPlayer param1, ClientLevel param2, LevelRenderer param3, long param4) {
         super();
         this.player = var1;
         this.level = var2;
         this.levelRenderer = var3;
         this.timeoutAfter = var4;
      }

      public LevelLoadTracker.ClientState tick() {
         return (LevelLoadTracker.ClientState)(this.isReady() ? new LevelLoadTracker.ClientLevelReady(Util.getMillis()) : this);
      }

      private boolean isReady() {
         if (Util.getMillis() > this.timeoutAfter) {
            LevelLoadTracker.LOGGER.warn("Timed out while waiting for the client to load chunks, letting the player into the world anyway");
            return true;
         } else {
            BlockPos var1 = this.player.blockPosition();
            return !this.level.isOutsideBuildHeight(var1.getY()) && !this.player.isSpectator() && this.player.isAlive() ? this.levelRenderer.isSectionCompiled(var1) : true;
         }
      }

      public LocalPlayer player() {
         return this.player;
      }

      public ClientLevel level() {
         return this.level;
      }

      public LevelRenderer levelRenderer() {
         return this.levelRenderer;
      }

      public long timeoutAfter() {
         return this.timeoutAfter;
      }
   }
}
