package org.northcs.selling;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class SellingEvents implements ServerPlayConnectionEvents.Join, ServerPlayConnectionEvents.Disconnect {

    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ReceiverQueue.push(handler.player.getUuid());
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        SellCommand.confirmations.remove(handler.player.getUuid());
    }
}
