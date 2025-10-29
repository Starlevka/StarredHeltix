package set.starlev.starredheltix.util.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import set.starlev.starredheltix.sound.ModSounds;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class ModUpdater {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Starlevka/StarredHeltix/releases/latest";
    private static final String CURRENT_VERSION = getCurrentVersion();
    private static String downloadUrl = null;
    private static String latestVersion = null;
    
    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String response = makeHttpRequest(GITHUB_API_URL);
                JsonObject release = JsonParser.parseString(response).getAsJsonObject();
                
                latestVersion = release.get("tag_name").getAsString();
                
                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    // Find .jar asset
                    JsonArray assets = release.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (name.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                    
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null) {
                        client.execute(() -> {
                            client.player.sendMessage(Text.literal("§6[StarredHeltix] §eДоступна новая версия: " + latestVersion + "! Хотите установить? /starredheltix update install"), false);
                            playUpdateSound();
                        });
                    }
                } else {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null) {
                        client.execute(() -> {
                            client.player.sendMessage(Text.literal("§a[StarredHeltix] У вас уже последняя версия: " + CURRENT_VERSION), false);
                        });
                    }
                }
            } catch (Exception e) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.execute(() -> {
                        client.player.sendMessage(Text.literal("§c[StarredHeltix] Ошибка проверки обновлений: " + e.getMessage()), false);
                    });
                }
            }
        });
    }
    
    public static void downloadUpdate() {
        if (downloadUrl == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[StarredHeltix] Сначала проверьте обновления!"), false);
            }
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.execute(() -> {
                        client.player.sendMessage(Text.literal("§e[StarredHeltix] Скачивание обновления..."), false);
                    });
                }
                
                // Download new version
                Path modsDir = Paths.get("mods");
                String fileName = "starredheltix-" + latestVersion + ".jar";
                Path newModPath = modsDir.resolve(fileName);
                
                downloadFile(downloadUrl, newModPath.toString());
                
                // Schedule old version deletion on game exit
                scheduleOldVersionDeletion();
                
                if (client != null && client.player != null) {
                    client.execute(() -> {
                        client.player.sendMessage(Text.literal("§a[StarredHeltix] Обновление скачано! Перезапустите игру для применения."), false);
                    });
                }
                
            } catch (Exception e) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.execute(() -> {
                        client.player.sendMessage(Text.literal("§c[StarredHeltix] Ошибка скачивания: " + e.getMessage()), false);
                    });
                }
            }
        });
    }
    
    public static void openReleasesPage() {
        try {
            Desktop.getDesktop().browse(URI.create("https://github.com/Starlevka/StarredHeltix/releases"));
        } catch (Exception e) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[StarredHeltix] Не удалось открыть браузер"), false);
            }
        }
    }
    
    public static void openProjectPage() {
        try {
            Desktop.getDesktop().browse(URI.create("https://github.com/Starlevka/StarredHeltix"));
        } catch (Exception e) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[StarredHeltix] Не удалось открыть браузер"), false);
            }
        }
    }
    
    private static void playUpdateSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.UPDATE_AVAILABLE, 1.0F, 1.0F));
        }
    }
    
    private static String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "StarredHeltix-Updater");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    private static void downloadFile(String urlString, String fileName) throws Exception {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.replaceAll("[^0-9.]", "").split("\\.");
        String[] currentParts = current.replaceAll("[^0-9.]", "").split("\\.");
        
        int maxLength = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < maxLength; i++) {
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }
    
    private static String getCurrentVersion() {
        try {
            InputStream stream = ModUpdater.class.getResourceAsStream("/version.txt");
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String version = reader.readLine();
                reader.close();
                return version != null ? version.trim() : "0.0.5";
            }
        } catch (Exception e) {
            // Fallback
        }
        return "0.0.5";
    }
    
    private static void scheduleOldVersionDeletion() {
        // Простое и безопасное решение - просто показываем сообщение
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            System.out.println("[StarredHeltix] Обновление завершено! Не забудьте удалить старую версию мода из папки mods/");
        }));
    }
}