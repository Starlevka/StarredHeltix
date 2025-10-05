package set.starlev.starredheltix.util.solver.fairysouls;

import com.mojang.authlib.properties.Property;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;

import java.util.Base64;

public class SkullTextureChecker {
    // The target texture hash for fairy souls
    private static final String TARGET_TEXTURE_HASH = "dc4f66936b35643509814571198912402305fa9e1f3191769378011915eb9167";
    
    public static boolean hasTargetTexture(net.minecraft.block.entity.BlockEntity blockEntity) {
        String textureHash = getTextureHash(blockEntity);
        return textureHash != null && textureHash.equals(TARGET_TEXTURE_HASH);
    }
    
    public static String getTextureHash(net.minecraft.block.entity.BlockEntity blockEntity) {
        if (!(blockEntity instanceof SkullBlockEntity skullEntity)) {
            return null;
        }
        
        ProfileComponent profileComponent = skullEntity.getOwner();
        if (profileComponent == null) {
            return null;
        }
        
        com.mojang.authlib.GameProfile profile = profileComponent.gameProfile();
        if (profile == null) {
            return null;
        }
        
        if (!profile.getProperties().containsKey("textures")) {
            return null;
        }
        
        for (Property property : profile.getProperties().get("textures")) {
            assert property != null;
            String value = property.value();
            if (value != null && !value.isEmpty()) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(value));
                    
                    
                    // Extract the hash from the URL - try multiple patterns
                    // Pattern 1: http://textures.minecraft.net/texture/
                    int start = decoded.indexOf("http://textures.minecraft.net/texture/");
                    if (start != -1) {
                        start += "http://textures.minecraft.net/texture/".length();
                        int end = decoded.indexOf("\"", start);
                        if (end != -1) {
                            return decoded.substring(start, end);
                        }
                    }
                    
                    // Pattern 2: https://textures.minecraft.net/texture/
                    start = decoded.indexOf("https://textures.minecraft.net/texture/");
                    if (start != -1) {
                        start += "https://textures.minecraft.net/texture/".length();
                        int end = decoded.indexOf("\"", start);
                        if (end != -1) {
                            return decoded.substring(start, end);
                        }
                    }
                    
                    // Pattern 3: Just the hash in the JSON
                    start = decoded.indexOf("\"value\":\"");
                    if (start != -1) {
                        start += "\"value\":\"".length();
                        int end = decoded.indexOf("\"", start);
                        if (end != -1) {
                            return decoded.substring(start, end);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return null;
    }
}