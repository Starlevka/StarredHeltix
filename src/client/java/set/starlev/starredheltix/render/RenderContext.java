package set.starlev.starredheltix.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RenderContext {
    private final MatrixStack matrices;
    private final Camera camera;
    private final float tickDelta;
    
    public RenderContext(MatrixStack matrices, Camera camera, float tickDelta) {
        this.matrices = matrices;
        this.camera = camera;
        this.tickDelta = tickDelta;
    }
    
    public void renderBox(Box box, Color color) {
        renderBox(box, color.r, color.g, color.b, color.a);
    }
    
    public void renderBox(Box box, float red, float green, float blue, float alpha) {
        Vec3d cameraPos = camera.getPos();
        Box offsetBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getLines());
        
        drawBoxOutline(matrices, vertexConsumer, offsetBox, red, green, blue, alpha);
        immediate.draw();
    }
    
    public void renderFilledBox(Box box, float red, float green, float blue, float alpha) {
            Vec3d cameraPos = camera.getPos();
        Box offsetBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getDebugQuads());
        
        drawBoxFilled(matrices, vertexConsumer, offsetBox, red, green, blue, alpha);
        immediate.draw();
    }
    
    public void renderHitbox(Box box, float red, float green, float blue, float alpha) {
        Vec3d cameraPos = camera.getPos();
        Box offsetBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getDebugQuads());
        
        drawHitboxFaces(matrices, vertexConsumer, offsetBox, red, green, blue, alpha);
        immediate.draw();
    }
    
    public void renderLine(Vec3d start, Vec3d end, float red, float green, float blue, float alpha) {
        Vec3d cameraPos = camera.getPos();
        Vec3d offsetStart = start.subtract(cameraPos);
        Vec3d offsetEnd = end.subtract(cameraPos);
        
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getLines());
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float)offsetStart.x, (float)offsetStart.y, (float)offsetStart.z).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float)offsetEnd.x, (float)offsetEnd.y, (float)offsetEnd.z).color(red, green, blue, alpha).normal(0, 1, 0);
        
        immediate.draw();
    }
    
    private void drawBoxOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        
        // Bottom face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        // Top face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        // Vertical edges
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
    }
    
    private void drawBoxFilled(MatrixStack matrices, VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        
        // All 6 faces of the box
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        // Vertical edges
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
    }
    
    private void drawHitboxFaces(MatrixStack matrices, VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        
        // Bottom face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, -1, 0);
        
        // Top face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        
        // North face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        
        // South face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        
        // West face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        
        // East face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).normal(1, 0, 0);
    }
}