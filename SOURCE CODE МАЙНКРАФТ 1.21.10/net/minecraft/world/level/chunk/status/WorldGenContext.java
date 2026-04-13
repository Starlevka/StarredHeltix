package net.minecraft.world.level.chunk.status;

import java.util.concurrent.Executor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public record WorldGenContext(ServerLevel level, ChunkGenerator generator, StructureTemplateManager structureManager, ThreadedLevelLightEngine lightEngine, Executor mainThreadExecutor, LevelChunk.UnsavedListener unsavedListener) {
   public WorldGenContext(ServerLevel param1, ChunkGenerator param2, StructureTemplateManager param3, ThreadedLevelLightEngine param4, Executor param5, LevelChunk.UnsavedListener param6) {
      super();
      this.level = var1;
      this.generator = var2;
      this.structureManager = var3;
      this.lightEngine = var4;
      this.mainThreadExecutor = var5;
      this.unsavedListener = var6;
   }

   public ServerLevel level() {
      return this.level;
   }

   public ChunkGenerator generator() {
      return this.generator;
   }

   public StructureTemplateManager structureManager() {
      return this.structureManager;
   }

   public ThreadedLevelLightEngine lightEngine() {
      return this.lightEngine;
   }

   public Executor mainThreadExecutor() {
      return this.mainThreadExecutor;
   }

   public LevelChunk.UnsavedListener unsavedListener() {
      return this.unsavedListener;
   }
}
