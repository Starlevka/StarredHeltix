package com.modfast.mixin;

import com.mojang.datafixers.DataFixer;
import com.modfast.util.LazyDataFixer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.datafixer.Schemas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClientDFUMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/datafixer/Schemas;getFixer()Lcom/mojang/datafixers/DataFixer;"))
    private DataFixer deferDfuInit() {
        // Defer DFU building! It takes ~4 seconds on the main thread
        // We will return a wrapper that only actually initializes Schemas if a chunk/player must be data-fixed
        return new LazyDataFixer(Schemas::getFixer);
    }
}
