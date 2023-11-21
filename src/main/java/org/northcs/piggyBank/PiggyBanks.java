package org.northcs.piggyBank;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.northcs.CurrencyMod;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class PiggyBanks {
    public static HashMap<UUID, PiggyBankInventory> piggyBanks = new HashMap<>();

    /**
     * Adds an item stack to a player's piggy bank, even if they're offline.
     *
     * @param player   the player to add the item to. If the player is offline, this should be null.
     * @param playerID the UUID of the player to add the item to.
     * @param stack    the item stack to add to the player.
     * @return the part of the stack that wasn't added to the player's inventory.
     */
    public static ItemStack addToPiggyBank(PlayerEntity player, @NotNull UUID playerID, @NotNull ItemStack stack) {
        if (stack.isEmpty()) return stack;

        if (player == null) {
            return addToOfflinePiggyBank(playerID, stack);
        } else {
            return addToOnlinePlayer(player, stack);
        }
    }

    private static ItemStack addToOfflinePiggyBank(UUID playerID, ItemStack stack) {
        // TODO: Is there a proper way to find the world directory?
        var playerDataFile = FabricLoader.getInstance().getGameDir().resolve("world/playerdata/" + playerID.toString().toLowerCase() + ".dat").toFile();
        if (!playerDataFile.exists()) return stack;

        // TODO: Put an exclusive lock on the file before reading and writing to prevent race conditions.

        // Adapted from WorldSaveHandler.
        NbtCompound nbtCompound;
        try {
            nbtCompound = NbtIo.readCompressed(playerDataFile);
        } catch (Exception exception) {
            CurrencyMod.LOGGER.error(exception.toString());
            return stack;
        }

        if (nbtCompound == null) {
            return stack;
        }

        int nbtVersion = NbtHelper.getDataVersion(nbtCompound, -1);
        var playerData = DataFixTypes.PLAYER.update(Schemas.getFixer(), nbtCompound, nbtVersion);

        // This usage is correct because we add default items when there's no data.
        PiggyBankInventory playerPiggyBank = new PiggyBankInventory();

        if (playerData.contains("PiggyItems", 9)) {
            playerPiggyBank.readNbtList(playerData.getList("PiggyItems", 10));
        } else {
            playerPiggyBank.addDefaultItems();
        }

        var outputStack = playerPiggyBank.addStack(stack);
        playerData.put("PiggyItems", playerPiggyBank.toNbtList());

        try {
            var playerDataDir = FabricLoader.getInstance().getGameDir().resolve("world/playerdata/").toFile();

            var tempFile = File.createTempFile(playerID.toString().toLowerCase() + "-", ".dat", playerDataDir);
            NbtIo.writeCompressed(nbtCompound, tempFile);

            var dataFile = new File(playerDataDir, playerID.toString().toLowerCase() + ".dat");
            var oldDataFile = new File(playerDataDir, playerID.toString().toLowerCase() + ".dat_old");

            Util.backupAndReplace(dataFile, tempFile, oldDataFile);
        } catch (Exception exception) {
            CurrencyMod.LOGGER.error(exception.toString());
            return stack;
        }

        return outputStack;
    }

    private static ItemStack addToOnlinePlayer(PlayerEntity player, ItemStack stack) {
        var uuid = player.getUuid();
        if (!PiggyBanks.piggyBanks.containsKey(uuid)) {
            PiggyBanks.piggyBanks.put(uuid, PiggyBankInventory.init());
        }

        var piggyBankInventory = PiggyBanks.piggyBanks.get(uuid);

        return piggyBankInventory.addStack(stack);
    }

    /**
     * Puts the specified amount of money in a player's piggy bank.
     * If it doesn't fit, it will be dropped on the ground.
     *
     * @param player the player to give the money to.
     * @param amount the amount of money to give.
     */
    public static void giveMoney(PlayerEntity player, int amount) {
        // First, split the denominations.
        int curAmount = amount;

        ItemStack[] coins = new ItemStack[CurrencyMod.COINS.length];
        // This list is sorted by highest to lowest value.
        for (var i = 0; i < CurrencyMod.COINS.length; i++) {
            var denomination = CurrencyMod.COINS[i];

            var numberOfCoins = curAmount / denomination.amount();
            curAmount %= denomination.amount();

            coins[i] = new ItemStack(denomination, numberOfCoins);
        }

        // Try adding the coins to the player's piggy bank.
        for (var i = 0; i < coins.length; i++) {
            if (!coins[i].isEmpty()) coins[i] = addToOnlinePlayer(player, coins[i]);
        }

        // And if that doesn't work, stick them on the ground.
        for (ItemStack coin : coins) {
            if (!coin.isEmpty()) player.dropStack(coin);
        }
    }
}
