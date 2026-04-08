package set.starlev.injections.mixin.world;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            if (SecretFunFeatures.INSTANCE.isCustomWeatherEnabled()) {
                set.starlev.secret.config.SecretConfig.WeatherMode mode = SecretFunFeatures.INSTANCE.getWeatherType();
                if (mode == set.starlev.secret.config.SecretConfig.WeatherMode.RAIN || mode == set.starlev.secret.config.SecretConfig.WeatherMode.THUNDER) {
                    cir.setReturnValue(1.0f);
                } else {
                    cir.setReturnValue(0.0f);
                }
            } else if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
                cir.setReturnValue(1.0f);
            }
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            if (SecretFunFeatures.INSTANCE.isCustomWeatherEnabled()) {
                set.starlev.secret.config.SecretConfig.WeatherMode mode = SecretFunFeatures.INSTANCE.getWeatherType();
                if (mode == set.starlev.secret.config.SecretConfig.WeatherMode.THUNDER) {
                    cir.setReturnValue(1.0f);
                } else {
                    cir.setReturnValue(0.0f);
                }
            }
        }
    }

    @Inject(method = "isRaining()Z", at = @At("HEAD"), cancellable = true)
    private void onIsRaining(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            if (SecretFunFeatures.INSTANCE.isCustomWeatherEnabled()) {
                set.starlev.secret.config.SecretConfig.WeatherMode mode = SecretFunFeatures.INSTANCE.getWeatherType();
                cir.setReturnValue(mode == set.starlev.secret.config.SecretConfig.WeatherMode.RAIN || mode == set.starlev.secret.config.SecretConfig.WeatherMode.THUNDER);
            } else if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isThundering()Z", at = @At("HEAD"), cancellable = true)
    private void onIsThundering(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            if (SecretFunFeatures.INSTANCE.isCustomWeatherEnabled()) {
                set.starlev.secret.config.SecretConfig.WeatherMode mode = SecretFunFeatures.INSTANCE.getWeatherType();
                cir.setReturnValue(mode == set.starlev.secret.config.SecretConfig.WeatherMode.THUNDER);
            }
        }
    }
}
