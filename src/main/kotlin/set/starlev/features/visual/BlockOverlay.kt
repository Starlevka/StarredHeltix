package set.starlev.features.visual

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.CollisionContext
import set.starlev.config.ConfigManager
import set.starlev.render.RenderContext
import set.starlev.utils.ColorUtils
import java.awt.Color

import com.mojang.blaze3d.systems.RenderSystem

import net.minecraft.client.renderer.RenderType

object BlockOverlayFeature {
    fun onRenderBlockOverlay(
        poseStack: PoseStack,
        entity: Entity,
        camX: Double,
        camY: Double,
        camZ: Double,
        blockPos: BlockPos,
        blockState: BlockState
    ): Boolean {
        val config = ConfigManager.features.visuals.blockOverlay
        if (!config.enabled) return false

        val world = entity.level()
        val shape = blockState.getShape(world, blockPos, CollisionContext.of(entity))
        if (shape.isEmpty) return false

        val fillColor = ColorUtils.parseColor(config.fillColor)
        val alpha = (fillColor shr 24) and 0xFF
        val red = (fillColor shr 16) and 0xFF
        val green = (fillColor shr 8) and 0xFF
        val blue = fillColor and 0xFF
        
        // Use RenderContext for rendering to avoid RenderSystem issues and reuse mod infrastructure
        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val bufferSource = mc.renderBuffers().bufferSource()
        
        // Create context
        val context = RenderContext(
            poseStack,
            camera,
            0f, // tickDelta (not needed for static blocks)
            bufferSource,
            null // cameraRenderState
        )



        shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            // Inflate slightly to prevent z-fighting (0.002 is usually enough)
            val box = AABB(
                blockPos.x + minX, blockPos.y + minY, blockPos.z + minZ,
                blockPos.x + maxX, blockPos.y + maxY, blockPos.z + maxZ
            ).inflate(0.002)
            
            // Draw Fill
            if (alpha > 0) {
                context.renderBox(
                    box, 
                    red / 255f, 
                    green / 255f, 
                    blue / 255f, 
                    alpha / 255f, 
                    true
                )
            }
        }
        
        // Force flush the debugQuads buffer to prevent render conflicts with other features
        // that use different RenderTypes but share the BufferSource
        try {
            bufferSource.endBatch(RenderType.debugQuads())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    private fun parseColor(colorStr: String): Color {
        val parts = colorStr.split(":")
        if (parts.size >= 5) {
            try {
                val r = (parts[1].toIntOrNull() ?: 0).coerceIn(0, 255)
                val g = (parts[2].toIntOrNull() ?: 255).coerceIn(0, 255)
                val b = (parts[3].toIntOrNull() ?: 0).coerceIn(0, 255)
                val a = (parts[4].toIntOrNull() ?: 80).coerceIn(0, 255)
                return Color(r, g, b, a)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return Color(0, 255, 0, 80)
    }
}
