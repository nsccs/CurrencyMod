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
import org.northcs.piggyBank.PiggyBanks;

import java.util.HashMap;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class SellCommand implements CommandRegistrationCallback {
    public static HashMap<UUID, ItemStack> confirmations = new HashMap<>();

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

            var perItemValue = getValue(handStack);
            var startingItemCount = handStack.getCount();

            // TODO: Implement selling limits.
            // Try to give away as many of the items as possible.
            for (var receiverID : ReceiverQueue.randomReceivers()) {
                // Don't give the items back to the seller.
                if (receiverID == player.getUuid()) continue;

                var receiver = context.getSource().getServer().getPlayerManager().getPlayer(receiverID);
                // This will work regardless of whether they're offline (receiver is null).
                // Update handstack to the new stack after attempting to give the items.
                handStack = PiggyBanks.addToPiggyBank(receiver, receiverID, handStack);

                if (handStack.isEmpty()) break;
            }

            var itemsSold = startingItemCount - handStack.getCount();

            if (itemsSold == 0) {
                context.getSource().sendFeedback(() -> Text.literal("No items were sold."), false);
            } else {
                var moneyMade = itemsSold * perItemValue;

                player.setStackInHand(Hand.MAIN_HAND, handStack);
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
}
