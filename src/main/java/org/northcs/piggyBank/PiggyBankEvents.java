package org.northcs.piggyBank;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.util.UUID;

public class PiggyBankEvents implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        UUID uuid = handler.player.getUuid();

        if(!PiggyBanks.piggyBanks.containsKey(uuid)) {
            PiggyBanks.piggyBanks.put(uuid, new PiggyBankInventory());
        }

        PiggyBanks.piggyBanks.get(uuid).playerLeft = true;
    }
}
