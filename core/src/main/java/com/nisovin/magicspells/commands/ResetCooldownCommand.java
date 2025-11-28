package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Collection;
import java.util.Collections;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.bukkit.data.Selector;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.bukkit.data.MultipleEntitySelector;
import org.incendo.cloud.bukkit.parser.selector.MultipleEntitySelectorParser;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class ResetCooldownCommand {

	private static final CloudKey<MultipleEntitySelector> TARGET_ENTITIES_KEY = CloudKey.of("entities", MultipleEntitySelector.class);
	private static final CloudKey<Spell> SPELL_KEY = CloudKey.of("spell", Spell.class);

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		Command.Builder<CommandSourceStack> base = manager.commandBuilder("ms", "magicspells")
			.literal("resetcd")
			.permission(Perm.COMMAND_RESET_COOLDOWN)
			.commandDescription(Description.of("Reset the cooldowns for the selected entities."))
			.handler(ResetCooldownCommand::resetCooldown);

		Command.Builder<CommandSourceStack> filtered = base.meta(HelpCommand.FILTER_FROM_HELP, true);

		var selectorComponent = CommandComponent.<CommandSourceStack, MultipleEntitySelector>builder()
			.key(TARGET_ENTITIES_KEY)
			.parser(MultipleEntitySelectorParser.multipleEntitySelectorParser(false))
			.description(Description.of("Selector for the entities to reset the cooldowns for. Use * for all entities, including unloaded entities and offline players."))
			.build();

		var spellComponent = CommandComponent.<CommandSourceStack, Spell>builder()
			.key(SPELL_KEY)
			.parser(SpellParser.spellParser())
			.description(Description.of("Spell to reset the cooldown for. Use * for all spells."))
			.build();

		manager.command(filtered
			.literal("*")
			.literal("*")
		);

		manager.command(filtered
			.argument(selectorComponent)
			.literal("*")
		);

		manager.command(filtered
			.literal("*")
			.argument(spellComponent)
		);

		manager.command(base
			.argument(selectorComponent)
			.argument(spellComponent)
		);
	}

	private static void resetCooldown(CommandContext<CommandSourceStack> context) {
		Collection<LivingEntity> entities = context.optional(TARGET_ENTITIES_KEY)
			.map(Selector::values)
			.map(entityCollection -> entityCollection.stream()
				.filter(entity -> entity instanceof LivingEntity)
				.map(entity -> (LivingEntity) entity)
				.toList()
			)
			.orElse(null);

		if (entities != null && entities.isEmpty())
			throw new InvalidCommandArgumentException("No targets were living entities");

		List<Spell> spells = context.optional(SPELL_KEY)
			.map(Collections::singletonList)
			.orElse(MagicSpells.getSpellsOrdered());

		for (Spell spell : spells) {
			if (entities == null) {
				spell.getCooldowns().clear();
				continue;
			}

			for (LivingEntity entity : entities)
				spell.setCooldown(entity, 0, false);
		}

		ComponentLike cooldown;
		if (spells.size() == 1) {
			cooldown = Component.text()
				.append(Component.text("Reset cooldown on spell '"))
				.append(Util.getMiniMessage(spells.getFirst().getName()))
				.append(Component.text("'"));
		} else {
			cooldown = Component.text("Reset cooldowns on all spells");
		}

		ComponentLike target;
		if (entities == null) {
			target = Component.text(" for all entities.");
		} else if (entities.size() == 1) {
			LivingEntity entity = entities.iterator().next();

			target = Component.text()
				.append(Component.text(entity instanceof Player ? " for player '" : " for entity '"))
				.append(entities.iterator().next().name().hoverEvent(entity))
				.append(Component.text("'."));
		} else {
			target = Component.text(" for the selected entities.");
		}

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text()
				.append(cooldown)
				.append(target)
		));
	}

}
