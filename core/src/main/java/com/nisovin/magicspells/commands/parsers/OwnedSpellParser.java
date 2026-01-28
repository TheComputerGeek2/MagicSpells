package com.nisovin.magicspells.commands.parsers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.function.Predicate;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ArgumentParseResult;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.EntityEquipment;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.commands.exceptions.GenericCommandException;

public class OwnedSpellParser extends SpellParser<CommandSourceStack> {

	public static ParserDescriptor<CommandSourceStack, Spell> ownedSpellParser() {
		return ParserDescriptor.of(new OwnedSpellParser(), Spell.class);
	}

	@Override
	public @NotNull ArgumentParseResult<@NotNull Spell> parse(@NotNull CommandContext<@NotNull CommandSourceStack> context, @NotNull CommandInput input) {
		ArgumentParseResult<Spell> result = super.parse(context, input);
		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getExecutor();

		Optional<Spell> parsedValue = result.parsedValue();
		if (parsedValue.isEmpty()) return result;

		Spell spell = parsedValue.get();

		if (sender instanceof Player player) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spell.isHelperSpell() && !Perm.COMMAND_CAST_SELF_HELPER.has(player) || !spellbook.hasSpell(spell))
				return ArgumentParseResult.failure(new GenericCommandException(MagicSpells.getUnknownSpellMessage()));
		}

		if (!spell.canCastByCommand())
			return ArgumentParseResult.failure(new GenericCommandException("You cannot cast this spell using commands."));

		if (spell.isRequiringCastItemOnCommand()) {
			if (!(sender instanceof LivingEntity entity))
				return ArgumentParseResult.failure(new GenericCommandException(spell.getStrWrongCastItem()));

			EntityEquipment equipment = entity.getEquipment();
			if (equipment == null)
				return ArgumentParseResult.failure(new GenericCommandException(spell.getStrWrongCastItem()));

			ItemStack item = equipment.getItemInMainHand();
			if (!spell.isValidItemForCastCommand(item))
				return ArgumentParseResult.failure(new GenericCommandException(spell.getStrWrongCastItem()));
		}

		return result;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();
		if (!(stack.getExecutor() instanceof Player player)) return super.stringSuggestions(context, input);

		return OwnedSpellParser.suggest(player);
	}

	public static @NotNull List<@NotNull String> suggest(@NotNull Player player) {
		return suggest(player, null);
	}

	public static @NotNull List<@NotNull String> suggest(@NotNull Player player, @Nullable Predicate<Spell> predicate) {
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		boolean canCastHelperSpells = Perm.COMMAND_CAST_SELF_HELPER.has(player);

		Stream<Spell> stream = MagicSpells.spells().stream()
			.filter(Spell::canCastByCommand)
			.filter(spell -> canCastHelperSpells || !spell.isHelperSpell())
			.filter(spellbook::hasSpell);

		if (predicate != null) stream = stream.filter(predicate);

		return stream
			.map(spell -> Util.getPlainString(Util.getMiniMessage(spell.getName())))
			.map(SpellParser::escapeIfRequired)
			.toList();
	}

}
