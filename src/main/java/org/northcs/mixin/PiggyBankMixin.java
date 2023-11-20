package org.northcs.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.northcs.piggyBank.PiggyBankInventory;
import org.northcs.piggyBank.PiggyBanks;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PiggyBankMixin extends Entity {
    @Shadow
    @Final
    private static Logger LOGGER;

    public PiggyBankMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt", locals = LocalCapture.CAPTURE_FAILHARD)
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID uuid = this.getUuid();

        if (nbt.contains("PiggyItems", 9)) {
            if (!PiggyBanks.piggyBanks.containsKey(uuid)) {
                // The player doesn't have a piggy bank, so give them a starting balance.
                PiggyBankInventory piggyBankInventory = new PiggyBankInventory();
                piggyBankInventory.addDefaultItems();

                PiggyBanks.piggyBanks.put(uuid, piggyBankInventory);
            }

            PiggyBanks.piggyBanks.get(uuid).readNbtList(nbt.getList("PiggyItems", 10));
        }
    }

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt", locals = LocalCapture.CAPTURE_FAILHARD)
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID uuid = this.getUuid();

        if (!PiggyBanks.piggyBanks.containsKey(uuid)) {
            PiggyBanks.piggyBanks.put(uuid, new PiggyBankInventory());
        }

        PiggyBankInventory piggyBankInventory = PiggyBanks.piggyBanks.get(uuid);

        nbt.put("PiggyItems", piggyBankInventory.toNbtList());

        // If this is writing because the player left, clear the inventory entry.
        if (piggyBankInventory.playerLeft) {
            PiggyBanks.piggyBanks.remove(uuid);
        }
    }
}