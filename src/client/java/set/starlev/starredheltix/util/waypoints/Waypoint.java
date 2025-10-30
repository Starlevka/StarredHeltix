package set.starlev.starredheltix.util.waypoints;

import net.minecraft.util.math.Vec3d;

public class Waypoint {
    private final String playerName;
    private final Vec3d position;
    private final long createdTime;
    private final long duration;
    
    public Waypoint(String playerName, Vec3d position, long duration) {
        this.playerName = playerName;
        this.position = position;
        this.createdTime = System.currentTimeMillis();
        this.duration = duration;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Vec3d getPosition() {
        return position;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - createdTime > duration;
    }
    
    public String getDisplayText() {
        return playerName + " (" + (int)position.x + ", " + (int)position.y + ", " + (int)position.z + ")";
    }
}