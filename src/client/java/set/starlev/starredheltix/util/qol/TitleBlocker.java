package set.starlev.starredheltix.util.qol;

public class TitleBlocker {
    private static long blockUntil = 0;
    
    public static void blockTitlesFor(long durationMs) {
        blockUntil = System.currentTimeMillis() + durationMs;
    }
    
    public static boolean shouldBlockTitle() {
        return System.currentTimeMillis() < blockUntil;
    }
}