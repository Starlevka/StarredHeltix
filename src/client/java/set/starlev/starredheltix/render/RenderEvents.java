package set.starlev.starredheltix.render;

import java.util.ArrayList;
import java.util.List;

public class RenderEvents {
    private static final List<WorldRenderCallback> callbacks = new ArrayList<>();
    
    public static void register(WorldRenderCallback callback) {
        callbacks.add(callback);
    }
    
    public static void fireWorldRender(RenderContext context) {
        for (WorldRenderCallback callback : callbacks) {
            callback.onWorldRender(context);
        }
    }
    
    @FunctionalInterface
    public interface WorldRenderCallback {
        void onWorldRender(RenderContext context);
    }
}