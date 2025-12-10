package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class MagicItemCommand {

	private static final CloudKey<MultiplePlayerSelector> TARGET_PLAYERS_KEY = CloudKey.of("player", MultiplePlayerSelector.class);
	private static final CloudKey<String> MAGIC_ITEM_KEY = CloudKey.of("magic item", String.class);
	private static final CloudKey<Integer> AMOUNT_KEY = CloudKey.of("amount", Integer.class);

	private static final CommandFlag<Void> DROP_LEFTOVER_FLAG = CommandFlag.builder("drop-leftover")
		.withDescription(Description.of("If used, leftover items will be dropped on the ground."))
		.withAliases("d")
		.build();

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("magicitem")
			.required(
				TARGET_PLAYERS_KEY,
				MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false),
				Description.of("Selector for the player(s) to give the magic item to.")
			)
			.required(
				MAGIC_ITEM_KEY,
				StringParser.stringParser(),
				Description.of("The magic item to give."),
				SuggestionProvider.suggestingStrings(MagicItems.getMagicItemKeys())
			)
			.optional(
				AMOUNT_KEY,
				IntegerParser.integerParser(1),
				Description.of("The amount of the magic item to give.")
			)
			.flag(DROP_LEFTOVER_FLAG)
			.commandDescription(Description.of("Give a magic item to players."))
			.permission(Perm.COMMAND_MAGIC_ITEM)
			.handler(MagicItemCommand::magicItem)
		);
	}

	private static void magicItem(CommandContext<CommandSourceStack> context) {
		String internalName = context.get(MAGIC_ITEM_KEY);

		MagicItem magicItem = MagicItems.getMagicItemByInternalName(internalName);
		if (magicItem == null)
			throw new InvalidCommandArgumentException("Invalid magic item '" + internalName + "'");

		Collection<Player> players = context.get(TARGET_PLAYERS_KEY).values();
		boolean dropLeftOver = context.flags().isPresent(DROP_LEFTOVER_FLAG);
		Integer amount = context.getOrDefault(AMOUNT_KEY, null);

		Collection<ItemStack> items = divideItems(magicItem, amount);
		players.forEach(p -> p.give(items, dropLeftOver));
	}

	private static Collection<ItemStack> divideItems(MagicItem magicItem, Integer amount) {
		ItemStack item = magicItem.getItemStack().clone();
		if (amount == null) return List.of(item);

		int maxStackSize = item.getMaxStackSize();
		if (amount < item.getMaxStackSize()) {
			item.setAmount(amount);
			return List.of(item);
		}

		ItemStack maxSizeStack = item.clone();
		maxSizeStack.setAmount(maxStackSize);

		int fullStackCount = amount / maxStackSize;
		int leftover = amount % maxStackSize;

		Collection<ItemStack> fullStacks = Collections.nCopies(fullStackCount, maxSizeStack);
		if (leftover == 0) return fullStacks;

		List<ItemStack> items = new ArrayList<>(fullStackCount + 1);
		items.addAll(fullStacks);

		item.setAmount(leftover);
		items.add(item);

		return items;
	}

}
