package set.starlev.registry

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.multiplayer.ClientLevel
import set.starlev.features.visual.GhostMegaChestMagmaCube

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey

object EntityRegistry {
    private val MAGMA_LOC = ResourceLocation.fromNamespaceAndPath("starredheltix", "mega_chest_magma")

    val MEGA_CHEST_MAGMA: EntityType<GhostMegaChestMagmaCube> = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        MAGMA_LOC,
        EntityType.Builder.of({ type, level -> GhostMegaChestMagmaCube(type as EntityType<GhostMegaChestMagmaCube>, level as ClientLevel) }, MobCategory.MISC)
            .sized(0.66f, 0.66f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, MAGMA_LOC))
    )

    fun init() {
    }
}
