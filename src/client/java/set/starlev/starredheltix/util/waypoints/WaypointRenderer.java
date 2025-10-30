package set.starlev.starredheltix.util.waypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import set.starlev.starredheltix.render.RenderContext;

public class WaypointRenderer {
    public static void renderWaypoint(RenderContext ctx, Waypoint waypoint) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d waypointPos = waypoint.getPosition();
        
        Vec3d offset = waypointPos.subtract(cameraPos);
        double distance = offset.length();
        
        if (distance > 200) return; // Don't render if too far
        
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(offset.x, offset.y + 2, offset.z);
        
        // Face the camera
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        String text = waypoint.getDisplayText();
        TextRenderer textRenderer = client.textRenderer;
        
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Background
        int textWidth = textRenderer.getWidth(text);
        VertexConsumer backgroundConsumer = immediate.getBuffer(RenderLayer.getDebugQuads());
        backgroundConsumer.vertex(matrix, -textWidth / 2f - 1, -1, 0).color(0, 0, 0, 128).normal(0, 1, 0);
        backgroundConsumer.vertex(matrix, textWidth / 2f + 1, -1, 0).color(0, 0, 0, 128).normal(0, 1, 0);
        backgroundConsumer.vertex(matrix, textWidth / 2f + 1, 8, 0).color(0, 0, 0, 128).normal(0, 1, 0);
        backgroundConsumer.vertex(matrix, -textWidth / 2f - 1, 8, 0).color(0, 0, 0, 128).normal(0, 1, 0);
        
        // Text
        textRenderer.draw(text, -textWidth / 2f, 0, 0xFFFFFF, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        
        immediate.draw();
        matrices.pop();
    }
}