package set.starlev.starredheltix.util.entity;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import set.starlev.starredheltix.client.StarredHeltixClient;
import com.mojang.blaze3d.systems.RenderSystem;

public class WolfHighlighter {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static int debugFrameCounter = 0;

    public static void register() {
        System.out.println("WolfHighlighter: Registering WorldRenderEvents.AFTER_TRANSLUCENT");
        // Using AFTER_TRANSLUCENT for better rendering
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WolfHighlighter::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        // Check if the feature is enabled
        if (!StarredHeltixClient.CONFIG.wolfHighlight.wolfHighlightEnabled) {
            return;
        }

        renderWolfHighlight(context);
    }

    private static void renderWolfHighlight(WorldRenderContext context) {
        if (CLIENT.world == null || CLIENT.player == null) return;

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        // Check if matrices is null
        if (matrices == null) {
            return;
        }
        
        // Increase render distance - use 2x the view distance for better detection
        int renderDistance = CLIENT.options.getViewDistance().getValue() * 32;
        double renderDistanceSq = renderDistance * renderDistance;
        
        int renderedWolves = 0;
        
        // Render all wolves within extended render distance
        for (Entity entity : CLIENT.world.getEntities()) {
            if (entity instanceof WolfEntity wolf) {
                // Check if wolf is alive and should be highlighted
                if (wolf.isAlive() && !wolf.isRemoved()) {
                    // Calculate distance squared to avoid expensive sqrt operation
                    double dx = wolf.getX() - cameraPos.x;
                    double dy = wolf.getY() - cameraPos.y;
                    double dz = wolf.getZ() - cameraPos.z;
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                        
                    // Only render if within extended render distance
                    if (distanceSq <= renderDistanceSq) {
                        // Calculate position relative to camera
                        double x = wolf.getX() - cameraPos.x;
                        double y = wolf.getY() - cameraPos.y;
                        double z = wolf.getZ() - cameraPos.z;

                        // Render highlight
                        renderEntityHighlight(context, x, y, z, wolf.getBoundingBox());
                        renderedWolves++;
                    }
                }
            }
        }
        
        // Debug output - reduce frequency of debug messages
        if (StarredHeltixClient.CONFIG.general.debugMode && debugFrameCounter++ % 1000 == 0) { // Less frequent debug output
            System.out.println("WolfHighlighter: Rendered " + renderedWolves + " wolves");
        }
    }

    private static void renderEntityHighlight(WorldRenderContext context, double x, double y, double z, Box box) {
        MatrixStack matrices = context.matrixStack();
        // Add null check for matrices
        if (matrices == null) {
            return;
        }
        
        matrices.push();
        matrices.translate(x, y, z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // Use bright green color for better visibility
        float red = 0.0f;
        float green = 255.0f;
        float blue = 0.0f;
        float alpha = 0.5f; // Increased alpha for better visibility

        // Calculate box dimensions relative to entity position (0,0,0)
        float boxWidth = (float) (box.maxX - box.minX);
        float boxHeight = (float) (box.maxY - box.minY);
        float boxDepth = (float) (box.maxZ - box.minZ);

        float localMinX = -boxWidth/2;
        float localMinY = 0;
        float localMinZ = -boxDepth/2;
        float localMaxX = boxWidth/2;
        float localMaxZ = boxDepth/2;

        // Enable rendering states
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull(); // Disable culling so we can see the boxes from all angles
        
        // Create outline by rendering a slightly larger box first
        float outlineExpand = 0.05f;
        renderBox(context, 
            localMinX - outlineExpand, localMinY - outlineExpand, localMinZ - outlineExpand,
            localMaxX + outlineExpand, boxHeight + outlineExpand, localMaxZ + outlineExpand,
            0.0f, 0.0f, 0.0f, alpha); // Black outline

        // Then render the normal colored box
        renderBox(context, localMinX, localMinY, localMinZ, localMaxX, boxHeight, localMaxZ, red, green, blue, alpha);

        // Reset render state
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void renderBox(WorldRenderContext context, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        MatrixStack matrices = context.matrixStack();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Front face (minZ)
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);

        // Back face (maxZ)
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);

        // Left face (minX)
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);

        // Right face (maxX)
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);

        // Top face (maxY)
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);

        // Bottom face (minY)
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);

        // Draw the buffer
        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ignored) {
            System.err.println("WolfHighlighter: Error rendering box: " + ignored.getMessage());
        }
    }

    /**
     * Parse a color integer into its RGB float components
     * @param color The color in 0xAARRGGBB format
     * @return Array of [red, green, blue] as floats between 0.0 and 1.0
     */
    private static float[] parseColor(int color) {
        int redInt = (color >> 16) & 0xFF;
        int greenInt = (color >> 8) & 0xFF;
        int blueInt = color & 0xFF;
        
        return new float[] {
            redInt / 255.0f,
            greenInt / 255.0f,
            blueInt / 255.0f
        };
    }
}