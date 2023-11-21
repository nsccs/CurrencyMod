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
        var uuid = this.getUuid();

        // Only load if there is data (doing otherwise will break default items).
        if (nbt.contains("PiggyItems", 9)) {
            var piggyBank = new PiggyBankInventory();
            piggyBank.readNbtList(nbt.getList("PiggyItems", 10));

            // We're loading data in, so the old copy should be overridden.
            PiggyBanks.piggyBanks.put(uuid, piggyBank);
        }
    }

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt", locals = LocalCapture.CAPTURE_FAILHARD)
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        var uuid = this.getUuid();

        // Don't save data if they have no piggy bank (doing this will break default items).
        if (!PiggyBanks.piggyBanks.containsKey(uuid)) {
            return;
        }

        var piggyBankInventory = PiggyBanks.piggyBanks.get(uuid);
        nbt.put("PiggyItems", piggyBankInventory.toNbtList());

        // If this is writing because the player left, clear the inventory entry.
        if (piggyBankInventory.playerLeft) {
            PiggyBanks.piggyBanks.remove(uuid);
        }
    }
}