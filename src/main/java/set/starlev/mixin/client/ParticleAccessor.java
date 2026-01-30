package set.starlev.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double starredheltix$getX();

    @Accessor("y")
    double starredheltix$getY();

    @Accessor("z")
    double starredheltix$getZ();
}
