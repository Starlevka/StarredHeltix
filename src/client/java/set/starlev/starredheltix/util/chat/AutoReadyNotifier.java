package set.starlev.starredheltix.util.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class AutoReadyNotifier {
    private static final Box READY_ZONE = new Box(-72, 122, -2, -70, 135, 2);
    
    private static boolean wasInZone = false;
    private static long lastMessageTime = 0;
    private static final long MESSAGE_COOLDOWN = 5000; // 5秒冷却时间

    public static void register() {
        // 注册客户端tick事件，用于检测玩家位置
        ClientTickEvents.END_CLIENT_TICK.register(AutoReadyNotifier::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        // 检查功能是否启用
        if (!StarredHeltixClient.CONFIG.general.chattingEnabled) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }

        // Получить позицию игрока
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        
        // Проверить находится ли игрок в зоне
        boolean isInZone = READY_ZONE.contains(playerPos);
        
        // 如果玩家进入了区域且之前不在区域中
        if (isInZone && !wasInZone) {
            // 检查冷却时间
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMessageTime > MESSAGE_COOLDOWN) {
                // Отправить сообщение о готовности
                String readyMessage = StarredHeltixClient.CONFIG.partyCommands.customReadyPhrase;
                
                player.networkHandler.sendChatCommand("pc " + readyMessage);
                lastMessageTime = currentTime;
            }
        }

        wasInZone = isInZone;
    }
}
