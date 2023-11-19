package org.northcs;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.northcs.blocks.PiggyBank;
import org.northcs.items.CopperCoin;
import org.northcs.items.GoldCoin;
import org.northcs.items.IronCoin;
import org.northcs.piggyBank.PiggyBankEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("CurrencyMod");
	public static final GoldCoin GOLD_COIN = new GoldCoin(new FabricItemSettings());
	public static final IronCoin IRON_COIN = new IronCoin(new FabricItemSettings());
	public static final CopperCoin COPPER_COIN = new CopperCoin(new FabricItemSettings());

	public static final Block PIGGY_BANK = new PiggyBank(FabricBlockSettings.create().strength(3.0f).nonOpaque().requiresTool());

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier("currencymod", "gold_coin"), GOLD_COIN);
		Registry.register(Registries.ITEM, new Identifier("currencymod", "iron_coin"), IRON_COIN);
		Registry.register(Registries.ITEM, new Identifier("currencymod", "copper_coin"), COPPER_COIN);

		Registry.register(Registries.BLOCK, new Identifier("currencymod", "piggy_bank"), PIGGY_BANK);
		Registry.register(Registries.ITEM, new Identifier("currencymod", "piggy_bank"), new BlockItem(PIGGY_BANK, new FabricItemSettings()));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
			content.addAfter(Items.NETHERITE_INGOT, GOLD_COIN);
		}
		);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
					content.addAfter(GOLD_COIN, IRON_COIN);
				}
		);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
					content.addAfter(IRON_COIN, COPPER_COIN);
				}
		);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
					content.addAfter(Items.ENDER_CHEST, PIGGY_BANK);
				}
		);

		ServerPlayConnectionEvents.DISCONNECT.register(new PiggyBankEvents());
	}
}