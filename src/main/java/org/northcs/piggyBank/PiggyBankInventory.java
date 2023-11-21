package org.northcs.piggyBank;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.northcs.CurrencyMod;

public class PiggyBankInventory extends SimpleInventory {
    public boolean playerLeft = false;

    /**
     * This should only be called when loading data in.
     * Otherwise, use init.
     */
    public PiggyBankInventory() {
        super(54);
    }

    /**
     * Creates a new PiggyBankInventory, initialized with the default items.
     * This should be called normally because a piggy bank will only load
     * if it exists on the player.
     * So, if it doesn't in PiggyBanks (and needs to be created), that means
     * that this is the player's first piggy bank, and they should get the default
     * items.
     */
    public static PiggyBankInventory init() {
        var piggyBank = new PiggyBankInventory();
        piggyBank.addDefaultItems();

        return piggyBank;
    }

    public void addDefaultItems() {
        this.addStack(new ItemStack(CurrencyMod.GOLD_COIN, 5));
        this.addStack(new ItemStack(CurrencyMod.IRON_COIN, 5));
        this.addStack(new ItemStack(CurrencyMod.COPPER_COIN, 25));
    }

    @Override
    public int getMaxCountPerStack() {
        return 128;
    }

    @Override
    public void readNbtList(NbtList nbtList) {
        int i;
        for (i = 0; i < this.size(); ++i) {
            this.setStack(i, ItemStack.EMPTY);
        }

        for (i = 0; i < nbtList.size(); ++i) {
            var nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            if (j >= 0 && j < this.size()) {
                this.setStack(j, ItemStack.fromNbt(nbtCompound));
            }
        }
    }

    @Override
    public NbtList toNbtList() {
        var nbtList = new NbtList();

        for (int i = 0; i < this.size(); ++i) {
            ItemStack itemStack = this.getStack(i);
            if (!itemStack.isEmpty()) {
                var nbtCompound = new NbtCompound();
                nbtCompound.putByte("Slot", (byte) i);
                itemStack.writeNbt(nbtCompound);
                nbtList.add(nbtCompound);
            }
        }

        return nbtList;
    }
}
