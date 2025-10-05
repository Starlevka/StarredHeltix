package set.starlev.starredheltix.util.solver.fairysouls;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PlayerHeadWaypointRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final List<BlockPos> nearbyHeads = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second

    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#.#");
    
    // Target texture hash for the specific player head
    private static final String TARGET_TEXTURE_HASH = "b96923ad247310007f6ae5d326d847ad53864cf16c3565a181dc8e6b20be2387";

    public static void register() {
        WorldRenderEvents.END.register(PlayerHeadWaypointRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        // Check if the feature should be enabled (using fairy souls config for now)
        if (!StarredHeltixClient.CONFIG.fairySouls.fairySoulsEnabled) {
            return;
        }
        
        if (CLIENT.player != null && CLIENT.world != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdate > UPDATE_INTERVAL) {
                updateNearbyHeads();
                lastUpdate = currentTime;
            }

            renderWaypoints(context);
        }
    }

    private static void updateNearbyHeads() {
        if (CLIENT.player == null || CLIENT.world == null) {
            return;
        }

        nearbyHeads.clear();
        BlockPos playerPos = CLIENT.player.getBlockPos();
        int searchRadius = 32;

        for (int x = playerPos.getX() - searchRadius; x <= playerPos.getX() + searchRadius; x++) {
            for (int y = playerPos.getY() - searchRadius; y <= playerPos.getY() + searchRadius; y++) {
                for (int z = playerPos.getZ() - searchRadius; z <= playerPos.getZ() + searchRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = CLIENT.world.getBlockState(pos);
                    if (state.getBlock() instanceof SkullBlock) {
                        // Check if this skull has the target texture
                        if (CLIENT.world.getBlockEntity(pos) instanceof SkullBlockEntity blockEntity) {
                            String textureHash = SkullTextureChecker.getTextureHash(blockEntity);
                            if (TARGET_TEXTURE_HASH.equals(textureHash)) {
                                nearbyHeads.add(pos);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderWaypoints(WorldRenderContext context) {
        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = camera.getPos();

        for (BlockPos headPos : nearbyHeads) {
            double x = headPos.getX() - cameraPos.x;
            double y = headPos.getY() - cameraPos.y;
            double z = headPos.getZ() - cameraPos.z;
            assert matrices != null;
            renderWaypoint(context, matrices, x, y, z, headPos);
        }
    }

    private static void renderWaypoint(WorldRenderContext context, MatrixStack matrices, double x, double y, double z, BlockPos headPos) {
        matrices.push();
        matrices.translate(x + 0.5, y + 0.5, z + 0.5);

        // Draw distance text only (removed cube rendering)
        if (CLIENT.player != null) {
            double distance = Math.sqrt(CLIENT.player.squaredDistanceTo(
                headPos.getX() + 0.5,
                headPos.getY() + 0.5,
                headPos.getZ() + 0.5));
            
            renderDistanceText(context, matrices, distance, 0.0f, 1.0f, 1.0f);
        }

        matrices.pop();
    }

    private static void renderDistanceText(WorldRenderContext context, MatrixStack matrices, double distance, float red, float green, float blue) {
        String distanceText = DISTANCE_FORMAT.format(distance) + "m";
        
        // Scale text based on distance - farther objects have larger text
        float baseScale = 0.025f;
        // Increase scale with distance, but cap it to prevent it from becoming too large
        float distanceScale = (float) Math.min(1.0, distance / 20.0); // Scale factor increases with distance
        float finalScale = baseScale * (1.0f + distanceScale); // At minimum 1x scale, at distance can be up to 2x
        
        matrices.push();
        matrices.translate(0, 0.5, 0); // Position text above the head location
        matrices.scale(finalScale, -finalScale, finalScale); // Scale down the text and flip it upright
        matrices.multiply(context.camera().getRotation()); // Make text always face the camera
        
        // Configure render state for maximum visibility through blocks
        RenderSystem.depthFunc(519); // GL_ALWAYS - Always pass depth test
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        // Render the text using the client's text renderer with shadow
        int textColor = (((int) (red * 255)) << 16) | (((int) (green * 255)) << 8) | ((int) (blue * 255)) | 0xFF000000;
        CLIENT.textRenderer.draw(distanceText, (float) -CLIENT.textRenderer.getWidth(distanceText) / 2, 0, textColor, true,
                matrices.peek().getPositionMatrix(), context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        
        // Reset render state
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL - Default depth function
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        
        matrices.pop();
    }
    
    // Test method to add some waypoints for debugging
    public static void addTestWaypoints() {
        if (CLIENT.player != null) {
            BlockPos playerPos = CLIENT.player.getBlockPos();
            nearbyHeads.add(playerPos.east(5).up(1));
            nearbyHeads.add(playerPos.north(5).up(1));
            nearbyHeads.add(playerPos.south(5).west(5).up(1));
        }
    }
}