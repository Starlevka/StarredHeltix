package set.starlev.starredheltix.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import set.starlev.starredheltix.util.user.ModUserManager;
import java.util.UUID;

public record ModIdentificationPacket(UUID playerUUID) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of("starredheltix", "mod_identification");
    public static final Id<ModIdentificationPacket> PACKET_TYPE = new Id<>(PACKET_ID);
    
    public static final PacketCodec<RegistryByteBuf, ModIdentificationPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            // Encoding - write player UUID
            buf.writeUuid(value.playerUUID());
        },
        buf -> new ModIdentificationPacket(buf.readUuid())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_TYPE;
    }
    
    public static void registerClientReceiver() {
        // Register client receiver
        ClientPlayNetworking.registerGlobalReceiver(PACKET_TYPE, (packet, context) -> {
            // When receiving a packet from the server, mark the player as a mod user
            ModUserManager.getInstance().addModUser(packet.playerUUID());
        });
    }
    
    public static void sendIdentificationPacket() {
        // Send identification packet to server
        if (ClientPlayNetworking.canSend(PACKET_TYPE)) {
            // Include the player's UUID in the packet
            UUID playerUUID = net.minecraft.client.MinecraftClient.getInstance().player.getUuid();
            ClientPlayNetworking.send(new ModIdentificationPacket(playerUUID));
        }
    }
}