package org.northcs.items;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public abstract class CoinBase extends Item {
    public CoinBase(Settings settings) {
        super(settings);
    }

    /**
     * How much is the coin worth?
     */
    public abstract int amount();

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        var amount = ((CoinBase) itemStack.getItem()).amount() * itemStack.getCount();
        tooltip.add(Text.literal("$" + amount).formatted(Formatting.AQUA));
    }
}
