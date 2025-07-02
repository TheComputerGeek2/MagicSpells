package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.entity.Player;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellbookReloadEvent;

@SuppressWarnings("UnstableApiUsage")
public class ReloadCommands {

	private static final CloudKey<MultiplePlayerSelector> TARGET_PLAYERS_KEY = CloudKey.of("players", MultiplePlayerSelector.class);

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		Command.Builder<CommandSourceStack> base = manager.commandBuilder("ms", "magicspells");
		Command.Builder<CommandSourceStack> reload = base.literal("reload");

		manager.command(reload
			.permission(Perm.COMMAND_RELOAD)
			.commandDescription(Description.of("Reload MagicSpells."))
			.handler(ReloadCommands::reload)
		);

		manager.command(reload
			.required(
				TARGET_PLAYERS_KEY,
				MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false),
				Description.of("Selector for the player(s) to reload the spellbooks for.")
			)
			.commandDescription(Description.of("Reload the spellbooks' of the selected players."))
			.permission(Perm.COMMAND_RELOAD_SPELLBOOK)
			.handler(ReloadCommands::reloadSpellbook)
		);

		manager.command(base
			.literal("reloadeffectlib")
			.commandDescription(Description.of("Reload the shaded EffectLib instance inside MagicSpells."))
			.permission(Perm.COMMAND_RELOAD_EFFECTLIB)
			.handler(ReloadCommands::reloadEffectLib)
		);
	}

	private static void reload(CommandContext<CommandSourceStack> context) {
		MagicSpells plugin = MagicSpells.getInstance();
		plugin.unload();
		plugin.load();

		context.sender().getSender().sendMessage(Component.text("MagicSpells plugin reloaded.", MagicSpells.getTextStyle()));
	}

	private static void reloadSpellbook(CommandContext<CommandSourceStack> context) {
		MultiplePlayerSelector selector = context.get(TARGET_PLAYERS_KEY);
		Collection<Player> players = selector.values();

		players.forEach(player -> {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			spellbook.reload();

			new SpellbookReloadEvent(player, spellbook).callEvent();
		});

		if (players.size() == 1) {
			context.sender().getSender().sendMessage(Component.text(
				"The spellbook for player '" + players.iterator().next().getName() + "' has been reloaded.",
				MagicSpells.getTextStyle()
			));

			return;
		}

		context.sender().getSender().sendMessage(
			Component.text()
				.style(MagicSpells.getTextStyle())
				.append(Component.text("The spellbooks for the "))
				.append(
					Component.text()
						.content("[selected players]")
						.hoverEvent(HoverEvent.showText(
							Component.text(
								players.stream()
									.map(Player::getName)
									.sorted()
									.collect(Collectors.joining("\n"))
							)
						))
				)
				.append(Component.text(" have been reloaded."))
		);
	}

	private static void reloadEffectLib(CommandContext<CommandSourceStack> context) {
		MagicSpells.resetEffectlib();
		context.sender().getSender().sendMessage(Component.text("EffectLib reloaded.", MagicSpells.getTextStyle()));
	}

}
