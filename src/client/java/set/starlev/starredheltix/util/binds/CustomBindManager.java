package set.starlev.starredheltix.util.binds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashMap;
import java.util.Map;

public class CustomBindManager {
    private static final Map<String, CustomBind> binds = new HashMap<>();
    private static final Map<String, Integer> bindKeys = new HashMap<>();
    private static final Map<Integer, Boolean> keyStates = new HashMap<>();
    private static boolean initialized = false;

    public static class CustomBind {
        public final String name;
        public final String command;
        public int keyCode;

        public CustomBind(String name, String command) {
            this.name = name;
            this.command = command;
            this.keyCode = GLFW.GLFW_KEY_UNKNOWN;
        }
    }

    public static void initialize() {
        if (!initialized) {
            loadBindsFromConfig();
            ClientTickEvents.END_CLIENT_TICK.register(CustomBindManager::onClientTick);
            initialized = true;
        }
    }
    
    private static void loadBindsFromConfig() {
        binds.clear();
        bindKeys.clear();
        
        for (Map.Entry<String, String> entry : StarredHeltixClient.CONFIG.customBinds.binds.entrySet()) {
            String name = entry.getKey();
            String command = entry.getValue();
            CustomBind bind = new CustomBind(name, command);
            
            Integer keyCode = StarredHeltixClient.CONFIG.customBinds.keys.get(name);
            if (keyCode != null) {
                bind.keyCode = keyCode;
                bindKeys.put(name, keyCode);
            }
            
            binds.put(name, bind);
        }
    }
    
    private static void saveBindsToConfig() {
        StarredHeltixClient.CONFIG.customBinds.binds.clear();
        StarredHeltixClient.CONFIG.customBinds.keys.clear();
        
        for (Map.Entry<String, CustomBind> entry : binds.entrySet()) {
            String name = entry.getKey();
            CustomBind bind = entry.getValue();
            
            StarredHeltixClient.CONFIG.customBinds.binds.put(name, bind.command);
            if (bind.keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                StarredHeltixClient.CONFIG.customBinds.keys.put(name, bind.keyCode);
            }
        }
        
        StarredHeltixClient.CONFIG.save();
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        
        for (Map.Entry<String, Integer> entry : bindKeys.entrySet()) {
            int keyCode = entry.getValue();
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN) continue;
            
            boolean currentState = GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
            boolean previousState = keyStates.getOrDefault(keyCode, false);
            
            if (currentState && !previousState) {
                executeBind(entry.getKey());
            }
            
            keyStates.put(keyCode, currentState);
        }
    }

    public static boolean createBind(String name, String command) {
        if (binds.containsKey(name)) {
            sendMessage("§cБинд с названием '" + name + "' уже существует!");
            return false;
        }

        if (name.length() > 20) {
            sendMessage("§cНазвание бинда не может быть длиннее 20 символов!");
            return false;
        }

        if (command.length() > 100) {
            sendMessage("§cКоманда не может быть длиннее 100 символов!");
            return false;
        }

        CustomBind bind = new CustomBind(name, command);
        binds.put(name, bind);
        bindKeys.put(name, GLFW.GLFW_KEY_UNKNOWN);
        saveBindsToConfig();

        sendMessage("§aБинд '" + name + "' создан! Команда: '" + command + "'");
        sendMessage("§eИспользуйте /starredheltix binds setkey \"" + name + "\" <клавиша> для назначения клавиши");
        sendMessage("§eИспользуйте /starredheltix binds list для просмотра всех биндов");

        return true;
    }

    public static boolean deleteBind(String name) {
        CustomBind bind = binds.remove(name);
        if (bind == null) {
            sendMessage("§cБинд с названием '" + name + "' не найден!");
            return false;
        }

        bindKeys.remove(name);
        saveBindsToConfig();
        sendMessage("§aБинд '" + name + "' удален!");
        return true;
    }

    public static void listBinds() {
        if (binds.isEmpty()) {
            sendMessage("§eУ вас нет созданных биндов!");
            return;
        }

        sendMessage("§6=== Ваши бинды ===");
        for (Map.Entry<String, CustomBind> entry : binds.entrySet()) {
            CustomBind bind = entry.getValue();
            String keyName = getKeyName(bind.keyCode);
            sendMessage("§e" + bind.name + "§7: §f" + bind.command + "§7 [" + keyName + "]");
        }
        sendMessage("§6==================");
    }

    public static void executeBind(String name) {
        CustomBind bind = binds.get(name);
        if (bind != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String cmd = bind.command.startsWith("/") ? bind.command.substring(1) : bind.command;
                client.player.networkHandler.sendChatCommand(cmd);
            }
        }
    }

    public static Map<String, CustomBind> getAllBinds() {
        return new HashMap<>(binds);
    }



    public static boolean setBindKey(String name, String keyName) {
        CustomBind bind = binds.get(name);
        if (bind == null) {
            sendMessage("§cБинд с названием '" + name + "' не найден!");
            return false;
        }
        
        int keyCode = parseKeyName(keyName);
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            sendMessage("§cНеизвестная клавиша: " + keyName);
            return false;
        }
        
        bind.keyCode = keyCode;
        bindKeys.put(name, keyCode);
        saveBindsToConfig();
        
        sendMessage("§aКлавиша '" + keyName + "' назначена для бинда '" + name + "'");
        return true;
    }
    
    private static int parseKeyName(String keyName) {
        keyName = keyName.toUpperCase();
        switch (keyName) {
            case "F1": return GLFW.GLFW_KEY_F1;
            case "F2": return GLFW.GLFW_KEY_F2;
            case "F3": return GLFW.GLFW_KEY_F3;
            case "F4": return GLFW.GLFW_KEY_F4;
            case "F5": return GLFW.GLFW_KEY_F5;
            case "F6": return GLFW.GLFW_KEY_F6;
            case "F7": return GLFW.GLFW_KEY_F7;
            case "F8": return GLFW.GLFW_KEY_F8;
            case "F9": return GLFW.GLFW_KEY_F9;
            case "F10": return GLFW.GLFW_KEY_F10;
            case "F11": return GLFW.GLFW_KEY_F11;
            case "F12": return GLFW.GLFW_KEY_F12;
            case "G": return GLFW.GLFW_KEY_G;
            case "H": return GLFW.GLFW_KEY_H;
            case "J": return GLFW.GLFW_KEY_J;
            case "K": return GLFW.GLFW_KEY_K;
            case "L": return GLFW.GLFW_KEY_L;
            case "M": return GLFW.GLFW_KEY_M;
            case "N": return GLFW.GLFW_KEY_N;
            case "O": return GLFW.GLFW_KEY_O;
            case "P": return GLFW.GLFW_KEY_P;
            case "U": return GLFW.GLFW_KEY_U;
            case "Y": return GLFW.GLFW_KEY_Y;
            case "Z": return GLFW.GLFW_KEY_Z;
            default: return GLFW.GLFW_KEY_UNKNOWN;
        }
    }
    
    private static String getKeyName(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_F1: return "F1";
            case GLFW.GLFW_KEY_F2: return "F2";
            case GLFW.GLFW_KEY_F3: return "F3";
            case GLFW.GLFW_KEY_F4: return "F4";
            case GLFW.GLFW_KEY_F5: return "F5";
            case GLFW.GLFW_KEY_F6: return "F6";
            case GLFW.GLFW_KEY_F7: return "F7";
            case GLFW.GLFW_KEY_F8: return "F8";
            case GLFW.GLFW_KEY_F9: return "F9";
            case GLFW.GLFW_KEY_F10: return "F10";
            case GLFW.GLFW_KEY_F11: return "F11";
            case GLFW.GLFW_KEY_F12: return "F12";
            case GLFW.GLFW_KEY_G: return "G";
            case GLFW.GLFW_KEY_H: return "H";
            case GLFW.GLFW_KEY_J: return "J";
            case GLFW.GLFW_KEY_K: return "K";
            case GLFW.GLFW_KEY_L: return "L";
            case GLFW.GLFW_KEY_M: return "M";
            case GLFW.GLFW_KEY_N: return "N";
            case GLFW.GLFW_KEY_O: return "O";
            case GLFW.GLFW_KEY_P: return "P";
            case GLFW.GLFW_KEY_U: return "U";
            case GLFW.GLFW_KEY_Y: return "Y";
            case GLFW.GLFW_KEY_Z: return "Z";
            case GLFW.GLFW_KEY_UNKNOWN: return "Не назначена";
            default: return "Неизвестная";
        }
    }

    private static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            MutableText text = Text.literal(message);
            client.player.sendMessage(text, false);
        }
    }


}