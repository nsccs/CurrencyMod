package org.northcs.piggyBank;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.northcs.items.CopperCoin;
import org.northcs.items.GoldCoin;
import org.northcs.items.IronCoin;

import java.util.Arrays;

public class PiggyBankGUI extends ScreenHandler {
    private static final int NUM_COLUMNS = 9;
    private final Inventory inventory;
    private final int rows;

    public PiggyBankGUI(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
    }

    private PiggyBankGUI(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, Inventory inventory, int rows) {
        // All of this code is from GenericScreenHandler.

        super(type, syncId);
        checkSize(inventory, rows * 9);
        this.inventory = inventory;
        this.rows = rows;
        inventory.onOpen(playerInventory.player);
        var i = (this.rows - 4) * 18;

        int j;
        int k;
        for (j = 0; j < this.rows; ++j) {
            for (k = 0; k < 9; ++k) {
                this.addSlot(new PiggyBankSlot(inventory, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        for (j = 0; j < 3; ++j) {
            for (k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + i));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 161 + i));
        }
    }

    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public ItemStack quickMove(PlayerEntity player, int slot) {
        // All of this code is from GenericScreenHandler.

        var itemStack = ItemStack.EMPTY;
        var slot2 = this.slots.get(slot);
        if (slot2 != null && slot2.hasStack()) {
            var itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();
            if (slot < this.rows * 9) {
                if (!this.insertItem(itemStack2, this.rows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack2, 0, this.rows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }

        return itemStack;
    }

    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public int getRows() {
        return this.rows;
    }

    public static class PiggyBankSlot extends Slot {
        public Class[] allowedItems = new Class[]{GoldCoin.class, IronCoin.class, CopperCoin.class};

        public PiggyBankSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!stack.isEmpty() && !stack.itemMatches(item -> Arrays.stream(allowedItems).anyMatch(itemClass -> itemClass == item.value().getClass()))) {
                return false;
            }

            return true;
        }
    }
}
