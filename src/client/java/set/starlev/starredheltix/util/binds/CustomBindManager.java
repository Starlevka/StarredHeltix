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
        
        // Check if ESC is pressed to reset the key binding
        if (keyName.equalsIgnoreCase("ESCAPE") || keyName.equalsIgnoreCase("ESC")) {
            bind.keyCode = GLFW.GLFW_KEY_UNKNOWN;
            bindKeys.put(name, GLFW.GLFW_KEY_UNKNOWN);
            saveBindsToConfig();
            sendMessage("§aКлавиша для бинда '" + name + "' сброшена");
            return true;
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
            // Function keys
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
            
            // Letter keys
            case "A": return GLFW.GLFW_KEY_A;
            case "B": return GLFW.GLFW_KEY_B;
            case "C": return GLFW.GLFW_KEY_C;
            case "D": return GLFW.GLFW_KEY_D;
            case "E": return GLFW.GLFW_KEY_E;
            case "F": return GLFW.GLFW_KEY_F;
            case "G": return GLFW.GLFW_KEY_G;
            case "H": return GLFW.GLFW_KEY_H;
            case "I": return GLFW.GLFW_KEY_I;
            case "J": return GLFW.GLFW_KEY_J;
            case "K": return GLFW.GLFW_KEY_K;
            case "L": return GLFW.GLFW_KEY_L;
            case "M": return GLFW.GLFW_KEY_M;
            case "N": return GLFW.GLFW_KEY_N;
            case "O": return GLFW.GLFW_KEY_O;
            case "P": return GLFW.GLFW_KEY_P;
            case "Q": return GLFW.GLFW_KEY_Q;
            case "R": return GLFW.GLFW_KEY_R;
            case "S": return GLFW.GLFW_KEY_S;
            case "T": return GLFW.GLFW_KEY_T;
            case "U": return GLFW.GLFW_KEY_U;
            case "V": return GLFW.GLFW_KEY_V;
            case "W": return GLFW.GLFW_KEY_W;
            case "X": return GLFW.GLFW_KEY_X;
            case "Y": return GLFW.GLFW_KEY_Y;
            case "Z": return GLFW.GLFW_KEY_Z;
            
            // Number keys
            case "0": return GLFW.GLFW_KEY_0;
            case "1": return GLFW.GLFW_KEY_1;
            case "2": return GLFW.GLFW_KEY_2;
            case "3": return GLFW.GLFW_KEY_3;
            case "4": return GLFW.GLFW_KEY_4;
            case "5": return GLFW.GLFW_KEY_5;
            case "6": return GLFW.GLFW_KEY_6;
            case "7": return GLFW.GLFW_KEY_7;
            case "8": return GLFW.GLFW_KEY_8;
            case "9": return GLFW.GLFW_KEY_9;
            
            // Numpad keys
            case "KP_0": return GLFW.GLFW_KEY_KP_0;
            case "KP_1": return GLFW.GLFW_KEY_KP_1;
            case "KP_2": return GLFW.GLFW_KEY_KP_2;
            case "KP_3": return GLFW.GLFW_KEY_KP_3;
            case "KP_4": return GLFW.GLFW_KEY_KP_4;
            case "KP_5": return GLFW.GLFW_KEY_KP_5;
            case "KP_6": return GLFW.GLFW_KEY_KP_6;
            case "KP_7": return GLFW.GLFW_KEY_KP_7;
            case "KP_8": return GLFW.GLFW_KEY_KP_8;
            case "KP_9": return GLFW.GLFW_KEY_KP_9;
            case "KP_ADD": return GLFW.GLFW_KEY_KP_ADD;
            case "KP_SUBTRACT": return GLFW.GLFW_KEY_KP_SUBTRACT;
            case "KP_MULTIPLY": return GLFW.GLFW_KEY_KP_MULTIPLY;
            case "KP_DIVIDE": return GLFW.GLFW_KEY_KP_DIVIDE;
            case "KP_DECIMAL": return GLFW.GLFW_KEY_KP_DECIMAL;
            case "KP_ENTER": return GLFW.GLFW_KEY_KP_ENTER;
            
            // Special keys
            case "ESCAPE": return GLFW.GLFW_KEY_ESCAPE;
            case "TAB": return GLFW.GLFW_KEY_TAB;
            case "BACKSPACE": return GLFW.GLFW_KEY_BACKSPACE;
            case "ENTER": return GLFW.GLFW_KEY_ENTER;
            case "SPACE": return GLFW.GLFW_KEY_SPACE;
            case "DELETE": return GLFW.GLFW_KEY_DELETE;
            case "INSERT": return GLFW.GLFW_KEY_INSERT;
            case "HOME": return GLFW.GLFW_KEY_HOME;
            case "END": return GLFW.GLFW_KEY_END;
            case "PAGE_UP": return GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN": return GLFW.GLFW_KEY_PAGE_DOWN;
            
            // Arrow keys
            case "UP": return GLFW.GLFW_KEY_UP;
            case "DOWN": return GLFW.GLFW_KEY_DOWN;
            case "LEFT": return GLFW.GLFW_KEY_LEFT;
            case "RIGHT": return GLFW.GLFW_KEY_RIGHT;
            
            // Modifier keys
            case "LEFT_SHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_SHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LEFT_CONTROL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHT_CONTROL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LEFT_ALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_ALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            case "LEFT_SUPER": return GLFW.GLFW_KEY_LEFT_SUPER;
            case "RIGHT_SUPER": return GLFW.GLFW_KEY_RIGHT_SUPER;
            
            // Punctuation keys
            case "SEMICOLON": return GLFW.GLFW_KEY_SEMICOLON;
            case "COMMA": return GLFW.GLFW_KEY_COMMA;
            case "PERIOD": return GLFW.GLFW_KEY_PERIOD;
            case "SLASH": return GLFW.GLFW_KEY_SLASH;
            case "BACKSLASH": return GLFW.GLFW_KEY_BACKSLASH;
            case "APOSTROPHE": return GLFW.GLFW_KEY_APOSTROPHE;
            case "EQUAL": return GLFW.GLFW_KEY_EQUAL;
            case "MINUS": return GLFW.GLFW_KEY_MINUS;
            case "LEFT_BRACKET": return GLFW.GLFW_KEY_LEFT_BRACKET;
            case "RIGHT_BRACKET": return GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "GRAVE_ACCENT": return GLFW.GLFW_KEY_GRAVE_ACCENT;
            
            // Media keys
            case "PRINT_SCREEN": return GLFW.GLFW_KEY_PRINT_SCREEN;
            case "SCROLL_LOCK": return GLFW.GLFW_KEY_SCROLL_LOCK;
            case "PAUSE": return GLFW.GLFW_KEY_PAUSE;
            case "CAPS_LOCK": return GLFW.GLFW_KEY_CAPS_LOCK;
            case "NUM_LOCK": return GLFW.GLFW_KEY_NUM_LOCK;
            
            default: return GLFW.GLFW_KEY_UNKNOWN;
        }
    }
    
    private static String getKeyName(int keyCode) {
        switch (keyCode) {
            // Function keys
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
            
            // Letter keys
            case GLFW.GLFW_KEY_A: return "A";
            case GLFW.GLFW_KEY_B: return "B";
            case GLFW.GLFW_KEY_C: return "C";
            case GLFW.GLFW_KEY_D: return "D";
            case GLFW.GLFW_KEY_E: return "E";
            case GLFW.GLFW_KEY_F: return "F";
            case GLFW.GLFW_KEY_G: return "G";
            case GLFW.GLFW_KEY_H: return "H";
            case GLFW.GLFW_KEY_I: return "I";
            case GLFW.GLFW_KEY_J: return "J";
            case GLFW.GLFW_KEY_K: return "K";
            case GLFW.GLFW_KEY_L: return "L";
            case GLFW.GLFW_KEY_M: return "M";
            case GLFW.GLFW_KEY_N: return "N";
            case GLFW.GLFW_KEY_O: return "O";
            case GLFW.GLFW_KEY_P: return "P";
            case GLFW.GLFW_KEY_Q: return "Q";
            case GLFW.GLFW_KEY_R: return "R";
            case GLFW.GLFW_KEY_S: return "S";
            case GLFW.GLFW_KEY_T: return "T";
            case GLFW.GLFW_KEY_U: return "U";
            case GLFW.GLFW_KEY_V: return "V";
            case GLFW.GLFW_KEY_W: return "W";
            case GLFW.GLFW_KEY_X: return "X";
            case GLFW.GLFW_KEY_Y: return "Y";
            case GLFW.GLFW_KEY_Z: return "Z";
            
            // Number keys
            case GLFW.GLFW_KEY_0: return "0";
            case GLFW.GLFW_KEY_1: return "1";
            case GLFW.GLFW_KEY_2: return "2";
            case GLFW.GLFW_KEY_3: return "3";
            case GLFW.GLFW_KEY_4: return "4";
            case GLFW.GLFW_KEY_5: return "5";
            case GLFW.GLFW_KEY_6: return "6";
            case GLFW.GLFW_KEY_7: return "7";
            case GLFW.GLFW_KEY_8: return "8";
            case GLFW.GLFW_KEY_9: return "9";
            
            // Numpad keys
            case GLFW.GLFW_KEY_KP_0: return "KP_0";
            case GLFW.GLFW_KEY_KP_1: return "KP_1";
            case GLFW.GLFW_KEY_KP_2: return "KP_2";
            case GLFW.GLFW_KEY_KP_3: return "KP_3";
            case GLFW.GLFW_KEY_KP_4: return "KP_4";
            case GLFW.GLFW_KEY_KP_5: return "KP_5";
            case GLFW.GLFW_KEY_KP_6: return "KP_6";
            case GLFW.GLFW_KEY_KP_7: return "KP_7";
            case GLFW.GLFW_KEY_KP_8: return "KP_8";
            case GLFW.GLFW_KEY_KP_9: return "KP_9";
            case GLFW.GLFW_KEY_KP_ADD: return "KP_ADD";
            case GLFW.GLFW_KEY_KP_SUBTRACT: return "KP_SUBTRACT";
            case GLFW.GLFW_KEY_KP_MULTIPLY: return "KP_MULTIPLY";
            case GLFW.GLFW_KEY_KP_DIVIDE: return "KP_DIVIDE";
            case GLFW.GLFW_KEY_KP_DECIMAL: return "KP_DECIMAL";
            case GLFW.GLFW_KEY_KP_ENTER: return "KP_ENTER";
            
            // Special keys
            case GLFW.GLFW_KEY_ESCAPE: return "ESCAPE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE_DOWN";
            
            // Arrow keys
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            
            // Modifier keys
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LEFT_SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RIGHT_SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LEFT_CONTROL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RIGHT_CONTROL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LEFT_ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RIGHT_ALT";
            case GLFW.GLFW_KEY_LEFT_SUPER: return "LEFT_SUPER";
            case GLFW.GLFW_KEY_RIGHT_SUPER: return "RIGHT_SUPER";
            
            // Punctuation keys
            case GLFW.GLFW_KEY_SEMICOLON: return "SEMICOLON";
            case GLFW.GLFW_KEY_COMMA: return "COMMA";
            case GLFW.GLFW_KEY_PERIOD: return "PERIOD";
            case GLFW.GLFW_KEY_SLASH: return "SLASH";
            case GLFW.GLFW_KEY_BACKSLASH: return "BACKSLASH";
            case GLFW.GLFW_KEY_APOSTROPHE: return "APOSTROPHE";
            case GLFW.GLFW_KEY_EQUAL: return "EQUAL";
            case GLFW.GLFW_KEY_MINUS: return "MINUS";
            case GLFW.GLFW_KEY_LEFT_BRACKET: return "LEFT_BRACKET";
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return "RIGHT_BRACKET";
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "GRAVE_ACCENT";
            
            // Media keys
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "PRINT_SCREEN";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "SCROLL_LOCK";
            case GLFW.GLFW_KEY_PAUSE: return "PAUSE";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS_LOCK";
            case GLFW.GLFW_KEY_NUM_LOCK: return "NUM_LOCK";
            
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