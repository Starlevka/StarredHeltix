package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;

public record RegionDataDto(RealmsRegion region, ServiceQuality serviceQuality) implements ReflectionBasedSerialization {
   public RegionDataDto(RealmsRegion param1, ServiceQuality param2) {
      super();
      this.region = var1;
      this.serviceQuality = var2;
   }

   @SerializedName("regionName")
   public RealmsRegion region() {
      return this.region;
   }

   @SerializedName("serviceQuality")
   public ServiceQuality serviceQuality() {
      return this.serviceQuality;
   }
}
