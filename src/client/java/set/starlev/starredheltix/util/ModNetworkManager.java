package set.starlev.starredheltix.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModNetworkManager {
    private static final String SERVER_URL = "http://localhost:3000/api"; // Replace with your actual server URL
    private static final Gson GSON = new Gson();
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    // Player discovery callback
    public interface PlayerDiscoveryCallback {
        void onPlayersDiscovered(Map<String, String> players);
        void onError(String error);
    }
    
    /**
     * Registers the current player with the mod network server
     */
    public static void registerPlayer() {
        CompletableFuture.runAsync(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) return;
                
                String playerName = client.player.getName().getString();
                String version = ModVersionRegistry.getPlayerVersion(playerName);
                
                JsonObject json = new JsonObject();
                json.addProperty("playerName", playerName);
                json.addProperty("version", version);
                json.addProperty("timestamp", System.currentTimeMillis());
                
                String response = sendPostRequest(SERVER_URL + "/register", json.toString());
                System.out.println("[ModNetworkManager] Registration response: " + response);
            } catch (Exception e) {
                System.err.println("[ModNetworkManager] Failed to register player: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }
    
    /**
     * Queries the server for players with the mod
     */
    public static void discoverPlayers(PlayerDiscoveryCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String response = sendGetRequest(SERVER_URL + "/players");
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                if (jsonResponse.has("players")) {
                    Map<String, String> players = new HashMap<>();
                    JsonObject playersObj = jsonResponse.getAsJsonObject("players");
                    
                    playersObj.entrySet().forEach(entry -> {
                        players.put(entry.getKey(), entry.getValue().getAsString());
                    });
                    
                    callback.onPlayersDiscovered(players);
                } else {
                    callback.onError("Invalid server response");
                }
            } catch (Exception e) {
                System.err.println("[ModNetworkManager] Failed to discover players: " + e.getMessage());
                e.printStackTrace();
                callback.onError("Network error: " + e.getMessage());
            }
        }, executor);
    }
    
    /**
     * Sends a GET request to the specified URL
     */
    private static String sendGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "StarredHeltix-Mod/1.0");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }
    
    /**
     * Sends a POST request to the specified URL with the given data
     */
    private static String sendPostRequest(String urlString, String data) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "StarredHeltix-Mod/1.0");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }
    
    /**
     * Shuts down the executor service
     */
    public static void shutdown() {
        executor.shutdown();
    }
}