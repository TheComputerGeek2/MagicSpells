package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.config.ConfigData;

// Advanced perm is for listing other player's spells

public class SublistSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private final List<String> spellsToHide;
	private final List<String> spellsToShow;

	private final ConfigData<Boolean> reloadGrantedSpells;
	private final ConfigData<Boolean> onlyShowCastableSpells;

	private String strPrefix;
	private String strNoSpells;

	public SublistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellsToHide = getConfigStringList("spells-to-hide", null);
		spellsToShow = getConfigStringList("spells-to-show", null);

		reloadGrantedSpells = getConfigDataBoolean("reload-granted-spells", true);
		onlyShowCastableSpells = getConfigDataBoolean("only-show-castable-spells", false);

		strPrefix = getConfigString("str-prefix", "Known spells:");
		strNoSpells = getConfigString("str-no-spells", "You do not know any spells.");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Spellbook spellbook = MagicSpells.getSpellbook(caster);
		ComponentLike extra = Component.space();
		if (data.hasArgs() && spellbook.hasAdvancedPerm("list")) {
			Player player = Bukkit.getPlayer(data.args()[0]);
			if (player != null) {
				spellbook = MagicSpells.getSpellbook(player);

				extra = Component.text()
					.append(Component.text(" ("))
					.append(player.displayName())
					.append(Component.text(") "));
			}
		}

		if (reloadGrantedSpells.get(data)) spellbook.addGrantedSpells();

		boolean onlyShowCastableSpells = this.onlyShowCastableSpells.get(data);
		Spellbook finalSpellbook = spellbook;

		List<Spell> spells = MagicSpells.getSpellsOrdered()
			.stream()
			.filter(spell -> shouldListSpell(spell, finalSpellbook, onlyShowCastableSpells))
			.sorted()
			.toList();

		if (spells.isEmpty()) {
			sendMessage(strNoSpells, caster, data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		TextComponent.Builder message = Component.text()
			.append(Util.getMiniMessage(strPrefix))
			.append(extra);

		boolean prev = false;
		for (Spell spell : spells) {
			if (shouldListSpell(spell, spellbook, onlyShowCastableSpells)) {
				if (prev) message.append(Component.text(", "));

				message.append(Util.getMiniMessage(spell.getName()));
				prev = true;
			}
		}

		caster.sendMessage(Util.getMessageText(message));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		Component message;

		Collection<Spell> spells = MagicSpells.spells();
		if (args != null && args.length > 0) {
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendPlainMessage("No such player.");
				return true;
			}

			spells = MagicSpells.getSpellbook(player).getSpells();
			message = Component.text(player.getName() + "'s spells: ");
		} else message = Component.text("All spells: ");

		boolean prev = false;
		for (Spell spell : spells) {
			if (prev) message = message.append(Component.text(", "));

			message = message.append(Util.getMiniMessage(spell.getName()));
			prev = true;
		}

		sender.sendMessage(message);

		return true;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (executor instanceof Player caster) {
			Spellbook spellbook = MagicSpells.getSpellbook(caster);
			if (!spellbook.hasAdvancedPerm("list")) return Collections.emptyList();
		} else if (!(executor instanceof ConsoleCommandSender)) return Collections.emptyList();

		return TxtUtil.tabCompletePlayerName(executor);
	}

	private boolean shouldListSpell(Spell spell, Spellbook spellbook, boolean onlyShowCastableSpells) {
		if (spell.isHelperSpell()) return false;
		if (!spellbook.hasSpell(spell, false)) return false;
		if (onlyShowCastableSpells && (!spellbook.canCast(spell) || spell instanceof PassiveSpell)) return false;
		if (spellsToHide != null && spellsToHide.contains(spell.getInternalName())) return false;
		return spellsToShow == null || spellsToShow.contains(spell.getInternalName());
	}

	public List<String> getSpellsToHide() {
		return spellsToHide;
	}

	public List<String> getSpellsToShow() {
		return spellsToShow;
	}

	public String getStrPrefix() {
		return strPrefix;
	}

	public void setStrPrefix(String strPrefix) {
		this.strPrefix = strPrefix;
	}

	public String getStrNoSpells() {
		return strNoSpells;
	}

	public void setStrNoSpells(String strNoSpells) {
		this.strNoSpells = strNoSpells;
	}

}
