package set.starlev.render

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.monster.Slime

class MegaChestRenderer(context: EntityRendererProvider.Context) : 
    MobRenderer<Slime, SlimeRenderState, MegaChestModel>(context, MegaChestModel.createModel(), 0.5f) {
    
    private val TEXTURE = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/entity/chest/mega_chest.png")

    override fun getTextureLocation(state: SlimeRenderState): ResourceLocation {
        return TEXTURE
    }

    override fun createRenderState(): SlimeRenderState {
        return SlimeRenderState()
    }

    override fun extractRenderState(entity: Slime, state: SlimeRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
    }
}
