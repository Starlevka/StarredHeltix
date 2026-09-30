package set.starlev.injections.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.scripting.SHScriptEngine;

/**
 * Перехват входящих и исходящих сетевых пакетов для JS-модулей.
 * @author Starlev
 */
@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onPacketReceive(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (!SHScriptEngine.INSTANCE.isLoaded() || packet == null) return;
        final boolean hasPkt = SHScriptEngine.INSTANCE.hasListeners("packetReceive");
        final boolean hasGame = SHScriptEngine.INSTANCE.hasListeners("pickup") || SHScriptEngine.INSTANCE.hasListeners("damage");
        // Нет слушателей — не трогаем netty-поток вообще (без аллокаций describePacket/getSimpleName).
        if (!hasPkt && !hasGame) return;
        // JS и разбор client.level идут на netty-потоке — маршалим в клиентский тред.
        final Packet<?> p = packet;
        Minecraft.getInstance().execute(() -> {
            if (hasPkt) SHScriptEngine.INSTANCE.fireTrigger("packetReceive", p.getClass().getSimpleName(), SHScriptEngine.INSTANCE.describePacket(p));
            if (hasGame) set.starlev.scripting.SHGameEvents.INSTANCE.handleGamePacket(p);
        });
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onPacketSend(Packet<?> packet, CallbackInfo ci) {
        if (!SHScriptEngine.INSTANCE.isLoaded() || packet == null) return;
        if (!SHScriptEngine.INSTANCE.hasListeners("packetSend")) return;
        final Packet<?> p = packet;
        Minecraft.getInstance().execute(() ->
            SHScriptEngine.INSTANCE.fireTrigger("packetSend", p.getClass().getSimpleName(), SHScriptEngine.INSTANCE.describePacket(p)));
    }
}
