package net.minecraft.client.renderer.blockentity.state;

import java.util.ArrayList;
import java.util.List;

public class BeaconRenderState extends BlockEntityRenderState {
   public float animationTime;
   public float beamRadiusScale;
   public List<BeaconRenderState.Section> sections = new ArrayList();

   public BeaconRenderState() {
      super();
   }

   public static record Section(int color, int height) {
      public Section(int param1, int param2) {
         super();
         this.color = var1;
         this.height = var2;
      }

      public int color() {
         return this.color;
      }

      public int height() {
         return this.height;
      }
   }
}
