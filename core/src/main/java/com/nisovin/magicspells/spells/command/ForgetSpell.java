package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Collections;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

// Advanced perm allows you to make others forget a spell
// Put * for the spell to forget all of them

public class ForgetSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private final ConfigData<Boolean> allowSelfForget;

	private String strUsage;
	private String strNoSpell;
	private String strNoTarget;
	private String strResetSelf;
	private String strDoesntKnow;
	private String strCastTarget;
	private String strResetTarget;
	private String strCastSelfTarget;

	public ForgetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		allowSelfForget = getConfigDataBoolean("allow-self-forget", true);

		strUsage = getConfigString("str-usage", "Usage: /cast forget <target> <spell>");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strResetSelf = getConfigString("str-reset-self", "You have forgotten all of your spells.");
		strDoesntKnow = getConfigString("str-doesnt-know", "That person does not know that spell.");
		strCastTarget = getConfigString("str-cast-target", "%a has made you forget the %s spell.");
		strResetTarget = getConfigString("str-reset-target", "You have reset %t's spellbook.");
		strCastSelfTarget = getConfigString("str-cast-self-target", "You have forgotten the %s spell.");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs() || data.args().length > 2) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spellbook casterSpellbook = MagicSpells.getSpellbook(caster);

		Player target;
		if (data.args().length == 1 && allowSelfForget.get(data)) target = caster;
		else if (data.args().length == 2 && casterSpellbook.hasAdvancedPerm("forget")) {
			List<Player> players = Bukkit.matchPlayer(data.args()[0]);
			if (players.size() != 1) {
				sendMessage(strNoTarget, caster, data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}
			target = players.get(0);
		} else {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}
		data = data.target(target);

		String spellName = data.args().length == 1 ? data.args()[0] : data.args()[1];
		boolean all = false;
		Spell spell = null;
		if (spellName.equals("*")) all = true;
		else spell = MagicSpells.getSpellByName(spellName);

		if (spell == null && !all) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!all && !casterSpellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
		if (!all && !targetSpellbook.hasSpell(spell)) {
			sendMessage(strDoesntKnow, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		// Remove spell(s)
		if (!all) {
			targetSpellbook.removeSpell(spell);
			targetSpellbook.save();
			if (!caster.equals(target)) {
				sendMessage(strCastTarget, target, data, "%s", spell.getName());
				sendMessage(strCastSelf, caster, data, "%s", spell.getName());
				playSpellEffects(data);
			} else {
				sendMessage(strCastSelfTarget, caster, data, "%s", spell.getName());
				playSpellEffects(data);
			}
			return new CastResult(PostCastAction.NO_MESSAGES, data);
		}

		targetSpellbook.removeAllSpells();
		targetSpellbook.save();

		if (!caster.equals(target)) {
			sendMessage(strResetTarget, caster, data);
			playSpellEffects(data);
		} else {
			sendMessage(strResetSelf, caster, data);
			playSpellEffects(data);
		}

		return new CastResult(PostCastAction.NO_MESSAGES, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			sender.sendMessage(strUsage);
			return false;
		}
		Player target = Bukkit.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(strNoTarget);
			return false;
		}
		Spell spell = null;
		boolean all = false;
		if (args[1].equals("*")) all = true;
		else spell = MagicSpells.getSpellByName(args[1]);

		if (spell == null && !all) {
			sender.sendMessage(strNoSpell);
			return false;
		}

		Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
		if (!all && !targetSpellbook.hasSpell(spell)) {
			sender.sendMessage(strDoesntKnow);
			return false;
		}

		SpellForgetEvent forgetEvent = new SpellForgetEvent(spell, target);
		if (!forgetEvent.callEvent()) return false;

		String consoleName = MagicSpells.getConsoleName();
		String targetDisplayName = Util.getStrictString(target.displayName());
		if (!all) {
			targetSpellbook.removeSpell(spell);
			targetSpellbook.save();

			String spellName = Util.getStrictString(spell.getName());
			sendMessage(strCastTarget, target, args, "%a", consoleName, "%s", spellName, "%t", targetDisplayName);
			sender.sendMessage(Util.getMiniMessage(formatMessage(strCastSelf, "%a", consoleName, "%s", spellName, "%t", targetDisplayName)));
		} else {
			targetSpellbook.removeAllSpells();
			targetSpellbook.save();

			sender.sendMessage(Util.getMiniMessage(formatMessage(strResetTarget, "%a", consoleName, "%t", targetDisplayName)));
		}
		return true;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (!(executor instanceof ConsoleCommandSender) && !executor.hasPermission(Perm.ADVANCED.getNode() + "forget")) {
			if (!(executor instanceof Player caster) || !allowSelfForget.get(new SpellData(caster)))
				return Collections.emptyList();

			return OwnedSpellParser.suggest(caster);
		}

		CommandInput original = input.copy();

		String playerName = input.readString();
		if (!input.isEmpty() || input.input().endsWith(" ")) {
			Player target = Bukkit.getPlayer(playerName);

			if (target != null) {
				String prefix = original.difference(input.skipWhitespace(), true);

				return OwnedSpellParser.suggest(target).stream()
					.map(s -> prefix + s)
					.toList();
			}
		}

		List<String> suggestions = TxtUtil.tabCompletePlayerName(executor);
		if (executor instanceof Player caster && allowSelfForget.get(new SpellData(caster))) {
			suggestions.add("*");
			suggestions.addAll(OwnedSpellParser.suggest(caster));
		}

		return suggestions;
	}

	public String getStrUsage() {
		return strUsage;
	}

	public void setStrUsage(String strUsage) {
		this.strUsage = strUsage;
	}

	public String getStrNoSpell() {
		return strNoSpell;
	}

	public void setStrNoSpell(String strNoSpell) {
		this.strNoSpell = strNoSpell;
	}

	public String getStrNoTarget() {
		return strNoTarget;
	}

	public void setStrNoTarget(String strNoTarget) {
		this.strNoTarget = strNoTarget;
	}

	public String getStrResetSelf() {
		return strResetSelf;
	}

	public void setStrResetSelf(String strResetSelf) {
		this.strResetSelf = strResetSelf;
	}

	public String getStrDoesntKnow() {
		return strDoesntKnow;
	}

	public void setStrDoesntKnow(String strDoesntKnow) {
		this.strDoesntKnow = strDoesntKnow;
	}

	public String getStrCastTarget() {
		return strCastTarget;
	}

	public void setStrCastTarget(String strCastTarget) {
		this.strCastTarget = strCastTarget;
	}

	public String getStrResetTarget() {
		return strResetTarget;
	}

	public void setStrResetTarget(String strResetTarget) {
		this.strResetTarget = strResetTarget;
	}

	public String getStrCastSelfTarget() {
		return strCastSelfTarget;
	}

	public void setStrCastSelfTarget(String strCastSelfTarget) {
		this.strCastSelfTarget = strCastSelfTarget;
	}

}
