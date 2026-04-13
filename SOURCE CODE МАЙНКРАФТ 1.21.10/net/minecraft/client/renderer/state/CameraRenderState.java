package net.minecraft.client.renderer.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class CameraRenderState {
   public BlockPos blockPos;
   public Vec3 pos;
   public boolean initialized;
   public Vec3 entityPos;
   public Quaternionf orientation;

   public CameraRenderState() {
      super();
      this.blockPos = BlockPos.ZERO;
      this.pos = new Vec3(0.0D, 0.0D, 0.0D);
      this.entityPos = new Vec3(0.0D, 0.0D, 0.0D);
      this.orientation = new Quaternionf();
   }
}
