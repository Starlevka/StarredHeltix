package set.starlev.starredheltix.util.entity;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import set.starlev.starredheltix.client.StarredHeltixClient;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharacterHighlighter {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    // Pattern to match the [Персонаж] prefix
    private static final Pattern CHARACTER_PREFIX_PATTERN =Pattern.compile("^\\[Персонаж]");
    
    // Pattern to match character name (everything between [Персонаж] and the colon)
    private static final Pattern CHARACTER_NAME_PATTERN = Pattern.compile("^\\[Персонаж]\\s*([^:]+):\\s*(.*)$");
    
    // List of validanswers that should trigger highlighting
    private static final Set<String> VALID_ANSWERS = new HashSet<>(Arrays.asList(
        "Они оба говорят правду. Также, награда в сундуке...",
        "В моем сундуке находится награда, и я говорю правду!",
        "По крайней мере один из них лжёт",
        "Награды нет ни в одном из сундуков.",
        "Награда не в моём сундуке!"
    ));
    
    // Store highlighted entities and their expiration time
    private static final Map<Entity, Long> highlightedEntities = new HashMap<>();
    
    // Store thelast detected character who told the truth
    private static String truthfulCharacter = "";
    
    public static void register() {
        // Register for chat messages
        ClientReceiveMessageEvents.GAME.register(CharacterHighlighter::onChatMessage);
        
        // Register render event for entity highlighting
        WorldRenderEvents.AFTER_TRANSLUCENT.register(CharacterHighlighter::onWorldRender);
    }
    
    private static void onChatMessage(Text message, boolean overlay) {
        // Check if this feature is enabled
        if (!StarredHeltixClient.CONFIG.general.enabled || !StarredHeltixClient.CONFIG.characterHighlight.enabled) return;
        
       String messageString = message.getString();
        
        // First check if this message starts with [Персонаж]
        if (CHARACTER_PREFIX_PATTERN.matcher(messageString).find()) {
            // Then extract the character name and message
            Matcher nameMatcher = CHARACTER_NAME_PATTERN.matcher(messageString);
            if (nameMatcher.matches()){
                String characterName = nameMatcher.group(1).trim();
                String characterMessage = nameMatcher.group(2).trim();
                
                // Check if this is one of the valid answers
                if (VALID_ANSWERS.contains(characterMessage)) {
                    // Display in chat who is telling the truth
                    if(client.player != null) {
                        client.player.sendMessage(
                            Text.literal("[StarredHeltix] ")
                                .formatted(Formatting.GREEN)
                                .append(Text.literal(characterName + " говорит правду!"))
                                .formatted(Formatting.BOLD), 
                            false
                        );
                    }
                    
                    // Playa sound effect
                    if (client.player != null) {
                        client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                    }
                    
                    // Store the truthful character
                    truthfulCharacter = characterName;
                    
                    // Highlight the character
                    highlightCharacter(characterName);
                }
            }
        }
    }
    
    private static void highlightCharacter(String characterName) {
        if (client.world == null || client.player == null) return;
        
        // Clear previous highlights
        highlightedEntities.clear();
        
        // Find entities with matching names
        for (Entity entity: client.world.getEntities()) {
            // Check if it's a player entity
            if (entity instanceof PlayerEntity) {
                String playerName = entity.getName().getString();
                if (playerName.equals(characterName)) {
                    // Highlight this player
                    long highlightDuration = StarredHeltixClient.CONFIG.characterHighlight.highlightDurationMs;
                    highlightedEntities.put(entity, System.currentTimeMillis() + highlightDuration);
                    break;
                }
            }
            // Could also check for NPCs or other entity types here
        }
    }
    
    private static void onWorldRender(WorldRenderContext context) {
        // Check if the feature is enabled
       if (!StarredHeltixClient.CONFIG.general.enabled || !StarredHeltixClient.CONFIG.characterHighlight.enabled) {
            return;
        }
        
        renderHighlightedEntities(context);
    }
    
    private static void renderHighlightedEntities(WorldRenderContext context) {
        if (client.world == null || client.player == null) return;
        
        // Clean up expired highlights
        long currentTime = System.currentTimeMillis();
        highlightedEntities.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        
        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        // Checkif matrices is null
        if (matrices == null) {
            return;
        }
        
        // Increase render distance - use 2x the view distance for better detection
        int renderDistance = client.options.getViewDistance().getValue() * 32;
        double renderDistanceSq = renderDistance * renderDistance;
        
        int renderedEntities = 0;
        
        // Render all highlighted entities within extended render distance
        for (Map.Entry<Entity, Long> entry : highlightedEntities.entrySet()) {
            Entity entity = entry.getKey();
            
            // Check if entity is alive and should be highlighted
            if (entity.isAlive()&& !entity.isRemoved()) {
                // Calculate distance squared to avoid expensive sqrt operation
                double dx = entity.getX() - cameraPos.x;
                double dy = entity.getY() - cameraPos.y;
                double dz = entity.getZ() - cameraPos.z;
                double distanceSq = dx * dx + dy *dy + dz * dz;
                    
                // Only render if within extended render distance
                if (distanceSq <= renderDistanceSq) {
                    // Calculate position relative to camera
                    double x = entity.getX() - cameraPos.x;
                    double y = entity.getY() - cameraPos.y;
                    double z = entity.getZ() - cameraPos.z;

                    // Render highlight
                    renderEntityHighlight(context, x, y, z, entity.getBoundingBox());
                    renderedEntities++;
                }
            }
        }
    }
    
    private static void renderEntityHighlight(WorldRenderContext context, double x, double y, double z, Box box){
        MatrixStack matrices = context.matrixStack();
        // Add null check for matrices
        if (matrices == null) {
            return;
        }
        
        matrices.push();
        matrices.translate(x, y, z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // Use color from configfor highlighting characters
        int color = StarredHeltixClient.CONFIG.characterHighlight.highlightColor;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = 0.4f;

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
            localMinX - outlineExpand, localMinY -outlineExpand, localMinZ - outlineExpand,
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

// Bottomface (minY)
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red,green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);

// Draw the buffer
        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ignored) {
            // Ignore rendering errors
        }
    }
    
    // Method to check if an entity should be highlighted
    public static boolean isEntityHighlighted(Entity entity) {
        // Clean upexpired highlights
        long currentTime = System.currentTimeMillis();
        highlightedEntities.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        
        return highlightedEntities.containsKey(entity);
    }
    
    // Method to get highlight color from config
    public static int getHighlightColor() {
        return StarredHeltixClient.CONFIG.characterHighlight.highlightColor;
    }
    
    // Get the name of the truthful character
    public static String getTruthfulCharacter() {
        return truthfulCharacter;
    }
}