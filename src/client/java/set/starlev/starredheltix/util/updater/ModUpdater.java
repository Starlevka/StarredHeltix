package set.starlev.starredheltix.util.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModUpdater {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Starlevka/StarredHeltix/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/Starlevka/StarredHeltix/releases";
    private static final long CHECK_INTERVAL = 3600000; // 1 hour in milliseconds
    private static long lastCheckTime = 0;
    private static String latestVersion = null;
    private static String downloadUrl = null;
    
    /**
     * Check for mod updates
     */
    public static void checkForUpdates() {
        // Always check for updates when this method is called (on game start)
        new Thread(() -> {
            try {
                String currentVersion = getCurrentVersion();
                JsonObject latestRelease = getLatestReleaseInfo();
                
                if (latestRelease != null) {
                    latestVersion = latestRelease.get("tag_name").getAsString();
                    JsonArray assets = latestRelease.getAsJsonArray("assets");
                    
                    // Find the JAR file asset
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String assetName = asset.get("name").getAsString();
                        if (assetName.endsWith(".jar") && assetName.contains("starredheltix")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                    
                    if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                        notifyUpdateAvailable(currentVersion, latestVersion);
                    } else if (isUpdatePending()) {
                        // Notify that update was installed
                        notifyUpdateInstalled(currentVersion);
                        clearUpdatePendingFlag();
                        // Clean up old mod files
                        cleanupOldModFiles();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to check for updates: " + e.getMessage());
                e.printStackTrace();
            }
        }, "StarredHeltix-Update-Checker").start();
    }
    
    /**
     * Download and prepare the update for installation on next restart
     */
    public static void downloadUpdate() {
        if (downloadUrl == null || latestVersion == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§6[StarredHeltix] §eDownloading update..."), false);
                }
                
                // Check if we can write to mods directory
                Path modsDir = Paths.get("mods");
                if (!Files.exists(modsDir)) {
                    try {
                        Files.createDirectories(modsDir);
                    } catch (IOException e) {
                        if (client.player != null) {
                            client.player.sendMessage(Text.of("§6[StarredHeltix] §cFailed to create mods directory: " + e.getMessage()), false);
                        }
                        return;
                    }
                }
                
                // Check write permissions
                if (!Files.isWritable(modsDir)) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.of("§6[StarredHeltix] §cCannot write to mods directory. Try running the game as administrator."), false);
                    }
                    return;
                }
                
                // Download the file
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                Path tempFile = modsDir.resolve("starredheltix-update.jar");
                Files.copy(
                    connection.getInputStream(),
                    tempFile,
                    StandardCopyOption.REPLACE_EXISTING
                );
                
                // Rename current mod file to backup
                Path currentMod = findCurrentModFile();
                if (currentMod != null) {
                    Path backupMod = modsDir.resolve("starredheltix-backup.jar");
                    if (Files.exists(backupMod)) {
                        Files.delete(backupMod);
                    }
                    Files.move(currentMod, backupMod);
                }
                
                // Rename downloaded file to proper mod name with version
                String modFileName = "starredheltix-" + latestVersion + ".jar";
                Path newModFile = modsDir.resolve(modFileName);
                Files.move(tempFile, newModFile);
                
                // Set update pending flag
                setUpdatePendingFlag(latestVersion);
                
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§6[StarredHeltix] §aUpdate downloaded! Restart the game to apply."), false);
                    client.player.sendMessage(Text.of("§6[StarredHeltix] §7New file: §f" + modFileName), false);
                }
            } catch (Exception e) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§6[StarredHeltix] §cFailed to download update: " + e.getMessage()), false);
                }
                System.err.println("Failed to download update: " + e.getMessage());
                e.printStackTrace();
            }
        }, "StarredHeltix-Updater").start();
    }
    
    /**
     * Get the current version of the mod
     * @return The current version string
     */
    private static String getCurrentVersion() {
        try {
            // Try to get version from the implementation version first
            String version = StarredHeltixClient.class.getPackage().getImplementationVersion();
            if (version != null && !version.isEmpty()) {
                return version;
            }
            
            // Fallback to reading from gradle properties
            Properties props = new Properties();
            try (InputStream is = ModUpdater.class.getResourceAsStream("/gradle.properties")) {
                if (is != null) {
                    props.load(is);
                    return props.getProperty("mod_version", "unknown");
                }
            }
            
            // Try to read gradle.properties from file system
            Path gradleProps = Paths.get("gradle.properties");
            if (Files.exists(gradleProps)) {
                try (InputStream is = Files.newInputStream(gradleProps)) {
                    props.load(is);
                    return props.getProperty("mod_version", "unknown");
                }
            }
            
            return "unknown";
        } catch (Exception e) {
            System.err.println("Failed to get current version: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Get information about the latest release from GitHub
     * @return JsonObject with release information or null if failed
     */
    private static JsonObject getLatestReleaseInfo() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "StarredHeltix-Updater");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
        } catch (IOException e) {
            System.err.println("Failed to get latest release info: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Notify the user that an update is available
     * @param currentVersion The current version
     * @param latestVersion The latest version
     */
    private static void notifyUpdateAvailable(String currentVersion, String latestVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.of("§6[StarredHeltix] §eUpdate available! Current: " + currentVersion + " -> Latest: " + latestVersion), 
                false
            );
            
            Text updateMessage = Text.of("§6[StarredHeltix] §eClick here to download and install update automatically")
                .copy()
                .styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/starredheltix update install"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§eClick to install update")))
                );
            
            client.player.sendMessage(updateMessage, false);
            
            // Add a clickable link to the releases page as fallback
            client.player.sendMessage(
                Text.of("§6[StarredHeltix] §eOr manually download: §9§n" + GITHUB_RELEASES_URL)
                    .copy()
                    .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB_RELEASES_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§eClick to open releases page")))
                    ),
                false
            );
        }
    }
    
    /**
     * Notify the user that an update was installed
     * @param newVersion The version that was installed
     */
    private static void notifyUpdateInstalled(String newVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.of("§6[StarredHeltix] §aMod successfully updated to version " + newVersion + "!"), 
                false
            );
            client.player.sendMessage(
                Text.of("§6[StarredHeltix] §7Backup of previous version saved as starredheltix-backup.jar"), 
                false
            );
            
            // Notify about cleanup
            client.player.sendMessage(
                Text.of("§6[StarredHeltix] §7Cleaning up old mod files..."), 
                false
            );
        }
    }
    
    /**
     * Clean up old mod files after update
     */
    private static void cleanupOldModFiles() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Path modsDir = Paths.get("mods");
            if (Files.exists(modsDir)) {
                File[] files = modsDir.toFile().listFiles();
                if (files != null) {
                    int cleanedCount = 0;
                    for (File file : files) {
                        if (file.getName().endsWith(".jar") && 
                            file.getName().contains("starredheltix") && 
                            file.getName().contains("backup")) {
                            Files.delete(file.toPath());
                            cleanedCount++;
                        }
                    }
                    
                    if (client.player != null) {
                        if (cleanedCount > 0) {
                            client.player.sendMessage(
                                Text.of("§6[StarredHeltix] §aSuccessfully cleaned up " + cleanedCount + " old mod file(s)!"), 
                                false
                            );
                        } else {
                            client.player.sendMessage(
                                Text.of("§6[StarredHeltix] §7No old mod files to clean up."), 
                                false
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.of("§6[StarredHeltix] §cFailed to clean up old mod files: " + e.getMessage()), 
                    false
                );
            }
            System.err.println("Failed to clean up old mod files: " + e.getMessage());
        }
    }
    
    /**
     * Open the releases page in the default browser
     */
    public static void openReleasesPage() {
        Util.getOperatingSystem().open(URI.create(GITHUB_RELEASES_URL));
    }
    
    /**
     * Set a flag indicating an update is pending
     */
    private static void setUpdatePendingFlag(String version) {
        try {
            Path flagFile = Paths.get("mods", ".starredheltix-update-pending");
            Files.write(flagFile, version.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to set update pending flag: " + e.getMessage());
        }
    }
    
    /**
     * Check if an update is pending
     */
    private static boolean isUpdatePending() {
        Path flagFile = Paths.get("mods", ".starredheltix-update-pending");
        return Files.exists(flagFile);
    }
    
    /**
     * Clear the update pending flag
     */
    private static void clearUpdatePendingFlag() {
        try {
            Path flagFile = Paths.get("mods", ".starredheltix-update-pending");
            if (Files.exists(flagFile)) {
                Files.delete(flagFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to clear update pending flag: " + e.getMessage());
        }
    }
    
    /**
     * Find the current mod file in the mods directory
     */
    private static Path findCurrentModFile() {
        try {
            Path modsDir = Paths.get("mods");
            if (Files.exists(modsDir)) {
                File[] files = modsDir.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".jar") && file.getName().contains("starredheltix") && !file.getName().contains("backup")) {
                            return file.toPath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to find current mod file: " + e.getMessage());
        }
        return null;
    }
}