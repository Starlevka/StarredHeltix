package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private static final int DEFAULT_MAX = 100;
   private final ResourceLocation id;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;

   public CustomBossEvent(ResourceLocation var1, Component var2) {
      super(var2, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.id = var1;
      this.setProgress(0.0F);
   }

   public ResourceLocation getTextId() {
      return this.id;
   }

   public void addPlayer(ServerPlayer var1) {
      super.addPlayer(var1);
      this.players.add(var1.getUUID());
   }

   public void addOfflinePlayer(UUID var1) {
      this.players.add(var1);
   }

   public void removePlayer(ServerPlayer var1) {
      super.removePlayer(var1);
      this.players.remove(var1.getUUID());
   }

   public void removeAllPlayers() {
      super.removeAllPlayers();
      this.players.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMax() {
      return this.max;
   }

   public void setValue(int var1) {
      this.value = var1;
      this.setProgress(Mth.clamp((float)var1 / (float)this.max, 0.0F, 1.0F));
   }

   public void setMax(int var1) {
      this.max = var1;
      this.setProgress(Mth.clamp((float)this.value / (float)var1, 0.0F, 1.0F));
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName()).withStyle((var1) -> {
         return var1.withColor(this.getColor().getFormatting()).withHoverEvent(new HoverEvent.ShowText(Component.literal(this.getTextId().toString()))).withInsertion(this.getTextId().toString());
      });
   }

   public boolean setPlayers(Collection<ServerPlayer> var1) {
      HashSet var2 = Sets.newHashSet();
      HashSet var3 = Sets.newHashSet();
      Iterator var4 = this.players.iterator();

      UUID var5;
      boolean var6;
      Iterator var7;
      while(var4.hasNext()) {
         var5 = (UUID)var4.next();
         var6 = false;
         var7 = var1.iterator();

         while(var7.hasNext()) {
            ServerPlayer var8 = (ServerPlayer)var7.next();
            if (var8.getUUID().equals(var5)) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var2.add(var5);
         }
      }

      var4 = var1.iterator();

      ServerPlayer var9;
      while(var4.hasNext()) {
         var9 = (ServerPlayer)var4.next();
         var6 = false;
         var7 = this.players.iterator();

         while(var7.hasNext()) {
            UUID var12 = (UUID)var7.next();
            if (var9.getUUID().equals(var12)) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var3.add(var9);
         }
      }

      for(var4 = var2.iterator(); var4.hasNext(); this.players.remove(var5)) {
         var5 = (UUID)var4.next();
         Iterator var11 = this.getPlayers().iterator();

         while(var11.hasNext()) {
            ServerPlayer var10 = (ServerPlayer)var11.next();
            if (var10.getUUID().equals(var5)) {
               this.removePlayer(var10);
               break;
            }
         }
      }

      var4 = var3.iterator();

      while(var4.hasNext()) {
         var9 = (ServerPlayer)var4.next();
         this.addPlayer(var9);
      }

      return !var2.isEmpty() || !var3.isEmpty();
   }

   public static CustomBossEvent load(ResourceLocation var0, CustomBossEvent.Packed var1) {
      CustomBossEvent var2 = new CustomBossEvent(var0, var1.name);
      var2.setVisible(var1.visible);
      var2.setValue(var1.value);
      var2.setMax(var1.max);
      var2.setColor(var1.color);
      var2.setOverlay(var1.overlay);
      var2.setDarkenScreen(var1.darkenScreen);
      var2.setPlayBossMusic(var1.playBossMusic);
      var2.setCreateWorldFog(var1.createWorldFog);
      Set var10000 = var1.players;
      Objects.requireNonNull(var2);
      var10000.forEach(var2::addOfflinePlayer);
      return var2;
   }

   public CustomBossEvent.Packed pack() {
      return new CustomBossEvent.Packed(this.getName(), this.isVisible(), this.getValue(), this.getMax(), this.getColor(), this.getOverlay(), this.shouldDarkenScreen(), this.shouldPlayBossMusic(), this.shouldCreateWorldFog(), Set.copyOf(this.players));
   }

   public void onPlayerConnect(ServerPlayer var1) {
      if (this.players.contains(var1.getUUID())) {
         this.addPlayer(var1);
      }

   }

   public void onPlayerDisconnect(ServerPlayer var1) {
      super.removePlayer(var1);
   }

   public static record Packed(Component name, boolean visible, int value, int max, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playBossMusic, boolean createWorldFog, Set<UUID> players) {
      final Component name;
      final boolean visible;
      final int value;
      final int max;
      final BossEvent.BossBarColor color;
      final BossEvent.BossBarOverlay overlay;
      final boolean darkenScreen;
      final boolean playBossMusic;
      final boolean createWorldFog;
      final Set<UUID> players;
      public static final Codec<CustomBossEvent.Packed> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ComponentSerialization.CODEC.fieldOf("Name").forGetter(CustomBossEvent.Packed::name), Codec.BOOL.optionalFieldOf("Visible", false).forGetter(CustomBossEvent.Packed::visible), Codec.INT.optionalFieldOf("Value", 0).forGetter(CustomBossEvent.Packed::value), Codec.INT.optionalFieldOf("Max", 100).forGetter(CustomBossEvent.Packed::max), BossEvent.BossBarColor.CODEC.optionalFieldOf("Color", BossEvent.BossBarColor.WHITE).forGetter(CustomBossEvent.Packed::color), BossEvent.BossBarOverlay.CODEC.optionalFieldOf("Overlay", BossEvent.BossBarOverlay.PROGRESS).forGetter(CustomBossEvent.Packed::overlay), Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(CustomBossEvent.Packed::darkenScreen), Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(CustomBossEvent.Packed::playBossMusic), Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(CustomBossEvent.Packed::createWorldFog), UUIDUtil.CODEC_SET.optionalFieldOf("Players", Set.of()).forGetter(CustomBossEvent.Packed::players)).apply(var0, CustomBossEvent.Packed::new);
      });

      public Packed(Component param1, boolean param2, int param3, int param4, BossEvent.BossBarColor param5, BossEvent.BossBarOverlay param6, boolean param7, boolean param8, boolean param9, Set<UUID> param10) {
         super();
         this.name = var1;
         this.visible = var2;
         this.value = var3;
         this.max = var4;
         this.color = var5;
         this.overlay = var6;
         this.darkenScreen = var7;
         this.playBossMusic = var8;
         this.createWorldFog = var9;
         this.players = var10;
      }

      public Component name() {
         return this.name;
      }

      public boolean visible() {
         return this.visible;
      }

      public int value() {
         return this.value;
      }

      public int max() {
         return this.max;
      }

      public BossEvent.BossBarColor color() {
         return this.color;
      }

      public BossEvent.BossBarOverlay overlay() {
         return this.overlay;
      }

      public boolean darkenScreen() {
         return this.darkenScreen;
      }

      public boolean playBossMusic() {
         return this.playBossMusic;
      }

      public boolean createWorldFog() {
         return this.createWorldFog;
      }

      public Set<UUID> players() {
         return this.players;
      }
   }
}
