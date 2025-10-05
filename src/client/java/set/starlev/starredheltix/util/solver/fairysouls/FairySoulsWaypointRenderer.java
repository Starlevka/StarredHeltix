package set.starlev.starredheltix.util.solver.fairysouls;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FairySoulsWaypointRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Set<BlockPos> foundSouls = ConcurrentHashMap.newKeySet();
    private static final List<BlockPos> nearbySouls = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 5000; // Update every 5 seconds (increased from 1 second)

    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#.#");
    
    public static void register() {
        // Try the same render event as PlayerHeadWaypointRendererWorldRenderEvents.END.register(FairySoulsWaypointRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        // Check if the feature is enabled
        if (!StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled) {
           return;
        }
        
        if (CLIENT.player != null && CLIENT.world != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdate > UPDATE_INTERVAL) {
                updateNearbySouls();
                lastUpdate = currentTime;
            }

            renderWaypoints(context);
        }
    }

    private static void updateNearbySouls() {
        if (CLIENT.player == null || CLIENT.world == null) {
            return;
        }

        nearbySouls.clear();
        BlockPos playerPos = CLIENT.player.getBlockPos();
        int searchRadius = 64; // Reduced from 128 to 64 forbetter performance
        // Limit the number of blocks we check per update to prevent freezing
        int blocksChecked = 0;
        int maxBlocksToCheck = 10000; // Limit to 10k blocks per update

        for (int x = playerPos.getX() - searchRadius;x <= playerPos.getX() + searchRadius; x++) {
            for (int y = playerPos.getY() - searchRadius; y <= playerPos.getY() + searchRadius; y++) {
                for (int z = playerPos.getZ() - searchRadius; z <= playerPos.getZ() + searchRadius; z++) {
                    // Check if we've exceeded our block check limit
                    if (blocksChecked++ > maxBlocksToCheck) {
                        return; // Stop checking more blocks to prevent freezing
                    }
                    
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!foundSouls.contains(pos)) {
                       // Instead of checking every block, first do a quick check if there's a block at this position
                        BlockState state = CLIENT.world.getBlockState(pos);
                        if (state.getBlock() instanceof SkullBlock) {
                           net.minecraft.block.entity.BlockEntity blockEntity = CLIENT.world.getBlockEntity(pos);
                            if (blockEntity instanceof net.minecraft.block.entity.SkullBlockEntity) {
                                if (SkullTextureChecker.hasTargetTexture(blockEntity)) {
                                    nearbySouls.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderWaypoints(WorldRenderContext context) {
        Camera camera =context.camera();
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = camera.getPos();
        
        // Get the view distance from Minecraft settings and convert to blocks
        // View distance in chunks * 16 blocks per chunk
        int viewDistanceBlocks = CLIENT.options.getViewDistance().getValue()* 16;

        // Limit the number of waypoints we render to prevent performance issues
        int renderedWaypoints = 0;
        int maxWaypointsToRender = 50; // Limit to 50 waypoints rendered at once

        for (BlockPos soulPos : nearbySouls) {
           // Check if we've exceeded our render limit
            if (renderedWaypoints++ > maxWaypointsToRender) {
                break; // Stop rendering more waypoints
            }
            
            // Check if the soul is within the view distance
            double dx = soulPos.getX() + 0.5 - cameraPos.x;
            double dy = soulPos.getY() + 0.5 - cameraPos.y;
            double dz = soulPos.getZ() + 0.5 - cameraPos.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            
            // Only render if within view distance(with some buffer)
            if (distanceSq <= (viewDistanceBlocks * viewDistanceBlocks)) {
                double x = soulPos.getX() - cameraPos.x;
                double y = soulPos.getY() - cameraPos.y;
                double z = soulPos.getZ() - cameraPos.z;
                renderWaypoint(context, matrices, x, y, z, soulPos);
            }
        }
    }

    private static void renderWaypoint(WorldRenderContext context, MatrixStack matrices, double x, double y, double z, BlockPos soulPos) {
        matrices.push();
        matrices.translate(x + 0.5, y + 0.5, z + 0.5);

        // Draw cube first
        renderCube(context, matrices);
        
        // Draw distance text
        if (CLIENT.player != null) {
            double distance = Math.sqrt(CLIENT.player.squaredDistanceTo(
                soulPos.getX() + 0.5,
soulPos.getY() + 0.5,
                soulPos.getZ() + 0.5));
            
            renderDistanceText(context, matrices, distance, 1.0f, 0.0f, 1.0f);
            
// Also draw coordinates and "Fairy Soul" text like temporary waypoints
            renderFairySoulText(context, matrices, soulPos, distance);
        }

        matrices.pop();
    }

private static void renderCube(WorldRenderContext context, MatrixStack matrices) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

       float size = 0.3f;
        
        // Bottom face
        buffer.vertex(positionMatrix, -size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
buffer.vertex(positionMatrix, -size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, -size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, -size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);

        // Top face
        buffer.vertex(positionMatrix, -size, size, -size).color(1.0f, 0.0f, 1.0f,1.0f);
        buffer.vertex(positionMatrix, size, size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, size, size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
       buffer.vertex(positionMatrix, -size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, -size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, -size, size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);

        // Vertical lines
        buffer.vertex(positionMatrix, -size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, -size, size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
buffer.vertex(positionMatrix, size, -size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, size, size, -size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        
        buffer.vertex(positionMatrix, -size, -size, size).color(1.0f, 0.0f, 1.0f, 1.0f);
        buffer.vertex(positionMatrix, -size, size, size).color(1.0f, 0.0f, 1.0f, 1.0f);

try{
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException e) {
            // Buffer was empty
        }
    }

    private static void renderDistanceText(WorldRenderContext context, MatrixStack matrices, double distance, float red, float green, float blue) {
        String distanceText = DISTANCE_FORMAT.format(distance) + "m";
        
        matrices.push();
        matrices.translate(0, 0.5, 0); // Position text above the soul location
        matrices.scale(0.025f, -0.025f, 0.025f); // Scaledown the text and flip it upright
        matrices.multiply(context.camera().getRotation()); // Make text always face the camera
        
        // Configure render state for maximum visibility through blocks
        RenderSystem.depthFunc(519); // GL_ALWAYS - Always pass depth test
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        //Render the text using the client's text renderer with shadow
        int textColor = (((int) (red * 255)) << 16) | (((int) (green * 255)) << 8) | ((int) (blue * 255)) | 0xFF000000;
        CLIENT.textRenderer.draw(distanceText, (float) -CLIENT.textRenderer.getWidth(distanceText) / 2, 0, textColor, true, // Enable shadow
                matrices.peek().getPositionMatrix(), context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0,15728880);
        
        // Reset render state
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL - Default depth function
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        
       matrices.pop();
    }

    private static void renderFairySoulText(WorldRenderContext context, MatrixStack matrices, BlockPos soulPos, double distance) {
        matrices.push();
        matrices.translate(0, 1.0, 0); // Position text above the cube
        matrices.scale(0.025f, -0.025f,0.025f); // Scale down the text and flip it upright
        matrices.multiply(context.camera().getRotation()); // Make text always face the camera (2D)
        
        String text = "Fairy Soul (" + soulPos.getX() + ", " + soulPos.getY() + ", " + soulPos.getZ() + ") [" + DISTANCE_FORMAT.format(distance) + " blocks]";
        int textColor = 0xFFFFFF; // White color
        
        // Configure render statefor maximum visibility through blocks
        RenderSystem.depthFunc(519); // GL_ALWAYS - Always pass depth test
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        CLIENT.textRenderer.draw(text, -CLIENT.textRenderer.getWidth(text) / 2, 0, textColor, false, 
                matrices.peek().getPositionMatrix(), context.consumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        
        // Reset render state
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL - Default depth function
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        
        matrices.pop();
    }

public static void markSoulAsFound(BlockPos pos) {
        foundSouls.add(pos);
        nearbySouls.remove(pos);
    }

    public static void addNearbySoul(BlockPos pos) {
        if (!foundSouls.contains(pos)) {
            nearbySouls.add(pos);
        }
    }

public static void removeNearbySoul(BlockPos pos) {
        nearbySouls.remove(pos);
    }

    public static boolean isSoulFound(BlockPos pos) {
        return foundSouls.contains(pos);
    }

    public static int getNearbySoulsCount() {
        return nearbySouls.size();
}
    
    public static int getFoundSoulsCount() {
        return foundSouls.size();
    }
}