package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.flag.FeatureFlagSet;

public record CommonListenerCookie(LevelLoadTracker levelLoadTracker, GameProfile localGameProfile, WorldSessionTelemetryManager telemetryManager, RegistryAccess.Frozen receivedRegistries, FeatureFlagSet enabledFeatures, @Nullable String serverBrand, @Nullable ServerData serverData, @Nullable Screen postDisconnectScreen, Map<ResourceLocation, byte[]> serverCookies, @Nullable ChatComponent.State chatState, Map<String, String> customReportDetails, ServerLinks serverLinks, Map<UUID, PlayerInfo> seenPlayers, boolean seenInsecureChatWarning) {
   public CommonListenerCookie(LevelLoadTracker param1, GameProfile param2, WorldSessionTelemetryManager param3, RegistryAccess.Frozen param4, FeatureFlagSet param5, @Nullable String param6, @Nullable ServerData param7, @Nullable Screen param8, Map<ResourceLocation, byte[]> param9, @Nullable ChatComponent.State param10, Map<String, String> param11, ServerLinks param12, Map<UUID, PlayerInfo> param13, boolean param14) {
      super();
      this.levelLoadTracker = var1;
      this.localGameProfile = var2;
      this.telemetryManager = var3;
      this.receivedRegistries = var4;
      this.enabledFeatures = var5;
      this.serverBrand = var6;
      this.serverData = var7;
      this.postDisconnectScreen = var8;
      this.serverCookies = var9;
      this.chatState = var10;
      this.customReportDetails = var11;
      this.serverLinks = var12;
      this.seenPlayers = var13;
      this.seenInsecureChatWarning = var14;
   }

   public LevelLoadTracker levelLoadTracker() {
      return this.levelLoadTracker;
   }

   public GameProfile localGameProfile() {
      return this.localGameProfile;
   }

   public WorldSessionTelemetryManager telemetryManager() {
      return this.telemetryManager;
   }

   public RegistryAccess.Frozen receivedRegistries() {
      return this.receivedRegistries;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   @Nullable
   public ServerData serverData() {
      return this.serverData;
   }

   @Nullable
   public Screen postDisconnectScreen() {
      return this.postDisconnectScreen;
   }

   public Map<ResourceLocation, byte[]> serverCookies() {
      return this.serverCookies;
   }

   @Nullable
   public ChatComponent.State chatState() {
      return this.chatState;
   }

   public Map<String, String> customReportDetails() {
      return this.customReportDetails;
   }

   public ServerLinks serverLinks() {
      return this.serverLinks;
   }

   public Map<UUID, PlayerInfo> seenPlayers() {
      return this.seenPlayers;
   }

   public boolean seenInsecureChatWarning() {
      return this.seenInsecureChatWarning;
   }
}
