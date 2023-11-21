package org.northcs.piggyBank;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class PiggyBankEvents implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        var uuid = handler.player.getUuid();

        // No need to do anything in this case, as this event is
        // to make sure that piggyBanks gets cleaned up.
        if (!PiggyBanks.piggyBanks.containsKey(uuid)) {
            return;
        }

        PiggyBanks.piggyBanks.get(uuid).playerLeft = true;
    }
}
