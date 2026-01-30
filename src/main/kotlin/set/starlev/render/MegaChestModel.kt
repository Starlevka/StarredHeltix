package set.starlev.render

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer

class MegaChestModel(modelPart: ModelPart) : EntityModel<SlimeRenderState>(modelPart) {
    private val body: ModelPart = modelPart.getChild("body")
    private val lid: ModelPart = body.getChild("lid")
    private val base: ModelPart = body.getChild("base")

    override fun setupAnim(state: SlimeRenderState) {
        super.setupAnim(state)
    }

    companion object {
        fun createModel(): MegaChestModel {
            val meshDefinition = MeshDefinition()
            val partDefinition = meshDefinition.root
            
            val body = partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            // База сундука
            body.addOrReplaceChild(
                "base",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-11.0f, -12.0f, -9.0f, 22.0f, 12.0f, 18.0f),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )
            
            // Крышка
            val lid = body.addOrReplaceChild(
                "lid",
                CubeListBuilder.create().texOffs(0, 30)
                    .addBox(-11.0f, -14.0f, -17.0f, 22.0f, 14.0f, 18.0f),
                PartPose.offset(0.0f, -11.0f, 8.0f)
            )
            
            // Замок
            lid.addOrReplaceChild(
                "knob",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -2.0f, -18.0f, 2.0f, 4.0f, 1.0f),
                PartPose.offset(0.0f, -6.0f, 0.0f)
            )
            
            val modelPart = meshDefinition.root.bake(128, 128)
            return MegaChestModel(modelPart)
        }
    }
}
