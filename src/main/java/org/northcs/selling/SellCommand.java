package org.northcs.selling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.northcs.CurrencyMod;
import org.northcs.piggyBank.PiggyBanks;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class SellCommand implements CommandRegistrationCallback {
    public static HashMap<UUID, ItemStack> confirmations = new HashMap<>();

    public static HashMap<UUID, Pair<LocalDate, Integer>> itemsSold = new HashMap<>();

    public static final int MAX_ITEMS_PER_DAY = 32;

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var confirmCommand = literal("confirm").requires(ServerCommandSource::isExecutedByPlayer).executes(context -> {
            var player = context.getSource().getPlayer();
            var handStack = player.getStackInHand(Hand.MAIN_HAND);

            // Make sure they've initiated the sell first.
            if (confirmations.get(player.getUuid()) != handStack) {
                throw new CommandException(Text.literal("Run \"/sell\" before this command!"));
            }
            confirmations.remove(player.getUuid());

            pruneItemsSold();
            var dailyItemsSold = itemsSold.containsKey(player.getUuid()) ? itemsSold.get(player.getUuid()).getSecond() : 0;

            var perItemValue = getValue(handStack);
            var startingItemCount = handStack.getCount();
            var curItemCount = startingItemCount;

            // Try to give away as many of the items as possible.
            for (var receiverID : ReceiverQueue.randomReceivers()) {
                // Don't give the items back to the seller.
                if (receiverID.equals(player.getUuid())) continue;

                // Figure out how much to give away.
                // Each retriever can receive 5 items (arbitrary).
                // It also needs to be within the item limit.
                // This max is just a sanity check to make sure the allowed amount is not negative.
                var maxItemsAllowedToSell = Math.max(0, MAX_ITEMS_PER_DAY - dailyItemsSold);
                var itemsToSell = Math.min(maxItemsAllowedToSell, Math.min(5, curItemCount));

                // If we can't sell any more items, then there's no reason to keep going.
                if (itemsToSell <= 0) break;

                var receiver = context.getSource().getServer().getPlayerManager().getPlayer(receiverID);
                // This will work regardless of whether they're offline (receiver is null).
                // Update handstack to the new stack after attempting to give the items.
                var unsoldItems = PiggyBanks.addToPiggyBank(receiver, receiverID, new ItemStack(handStack.getItem(), itemsToSell));

                // These mins and maxes are just sanity checks, because we can't sell less than 0
                // items or more than the amount we sold.
                var soldItems = Math.min(itemsToSell, Math.max(0, itemsToSell - unsoldItems.getCount()));

                curItemCount -= soldItems;
                dailyItemsSold += soldItems;
            }

            itemsSold.put(player.getUuid(), new Pair<>(LocalDate.now(), dailyItemsSold));

            var itemsSold = startingItemCount - curItemCount;

            if (itemsSold == 0) {
                context.getSource().sendFeedback(() -> Text.literal("No items were sold."), false);
            } else {
                var moneyMade = itemsSold * perItemValue;

                player.setStackInHand(Hand.MAIN_HAND, new ItemStack(handStack.getItem(), curItemCount));
                PiggyBanks.giveMoney(player, moneyMade);

                context.getSource().sendFeedback(() -> Text.literal("Successfully sold " + itemsSold + " items!"), false);
            }

            return 1;
        });

        var mainCommand = literal("sell").requires(ServerCommandSource::isExecutedByPlayer).executes(context -> {
            var player = context.getSource().getPlayer();
            var handStack = player.getStackInHand(Hand.MAIN_HAND);

            // Ensure that it's a food item.
            // TODO: Make this work with non-food items.
            if (!handStack.isFood()) {
                throw new CommandException(Text.literal("You can only sell food!"));
            }

            // Don't let people sell worthless items.
            var stackValue = getValue(handStack);
            if (stackValue == 0) {
                throw new CommandException(Text.literal("This item cannot be sold!"));
            }

            pruneItemsSold();
            var dailyItemsSold = itemsSold.containsKey(player.getUuid()) ? itemsSold.get(player.getUuid()).getSecond() : 0;
            if (dailyItemsSold >= MAX_ITEMS_PER_DAY) {
                throw new CommandException(Text.literal("You can only sell " + MAX_ITEMS_PER_DAY + " per day!"));
            }

            confirmations.put(player.getUuid(), handStack);

            context.getSource().sendFeedback(() -> Text.literal("You are trying to sell " +
                    handStack.getCount() +
                    "x " +
                    handStack.getName().getString() +
                    " for $" +
                    stackValue +
                    " each. Run /sell confirm to confirm."
            ), false);

            return 1;
        }).then(confirmCommand);

        dispatcher.register(mainCommand);
    }

    private int getValue(ItemStack stack) {
        var amount = 1.0;
        // Scale hunger down by 2 (arbitrary).
        amount += stack.getItem().getFoodComponent().getHunger() / 2.0;
        for (var effect : stack.getItem().getFoodComponent().getStatusEffects()) {
            var effectAmount = getEffectValue(effect);

            // Apply a scaling factor of 1.5 (arbitrary).
            amount += effectAmount * 1.5;
        }

        // This is arbitrary.
        var maxValue = 50;

        return Math.max(0, Math.min(maxValue, (int) amount));
    }

    private static double getEffectValue(Pair<StatusEffectInstance, Float> effect) {
        var statusEffect = effect.getFirst();
        var effectCategory = statusEffect.getEffectType().getCategory();

        var effectAmount = switch (effectCategory) {
            case BENEFICIAL -> 1.0;
            case NEUTRAL -> 0.0;
            case HARMFUL -> -1.0;
        };

        // The effect amount will be multiplied by the number of minutes it applies for,
        // up to a limit of 5 (arbitrary).
        // If the effect is negative, it will be punished more severely by having a lower bound of 1.
        effectAmount *= Math.max(effectCategory == StatusEffectCategory.HARMFUL ? 1.0 : 0.0, Math.min(5.0, statusEffect.getDuration() / 60.0));

        // It will also be multiplied by the effect level, up to 5 (arbitrary).
        effectAmount *= Math.max(1.0, Math.min(5.0, statusEffect.getAmplifier()));

        // Finally, it will be multiplied by the chance that it applies.
        effectAmount *= effect.getSecond();
        return effectAmount;
    }

    private static void pruneItemsSold() {
        var curDate = LocalDate.now();
        itemsSold.entrySet().removeIf(entry -> {
            CurrencyMod.LOGGER.info(Boolean.toString(!entry.getValue().getFirst().equals(curDate)));
            return !entry.getValue().getFirst().equals(curDate);
        });
    }
}
