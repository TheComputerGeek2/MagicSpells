package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

public class TeachSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private ConfigData<Boolean> requireKnownSpell;

	private String strUsage;
	private String strNoSpell;
	private String strNoTarget;
	private String strCantTeach;
	private String strCantLearn;
	private String strCastTarget;
	private String strAlreadyKnown;

	public TeachSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireKnownSpell = getConfigDataBoolean("require-known-spell", true);

		strUsage = getConfigString("str-usage", "Usage: /cast teach <target> <spell>");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strCantTeach = getConfigString("str-cant-teach", "You can't teach that spell.");
		strCantLearn = getConfigString("str-cant-learn", "That person cannot learn that spell.");
		strCastTarget = getConfigString("str-cast-target", "%a has taught you the %s spell.");
		strAlreadyKnown = getConfigString("str-already-known", "That person already knows that spell.");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player player)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (data.args() == null || data.args().length != 2) {
			sendMessage(strUsage, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		List<Player> players = Bukkit.matchPlayer(data.args()[0]);
		if (players.size() != 1) {
			sendMessage(strNoTarget, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Player target = players.get(0);
		data = data.target(target);

		Spell spell = MagicSpells.getSpellByName(data.args()[1]);
		if (spell == null) {
			sendMessage(strNoSpell, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (!spellbook.hasSpell(spell) && requireKnownSpell.get(data)) {
			sendMessage(strNoSpell, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!spellbook.canTeach(spell)) {
			sendMessage(strCantTeach, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
		if (!targetSpellbook.canLearn(spell)) {
			sendMessage(strCantLearn, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (targetSpellbook.hasSpell(spell)) {
			sendMessage(strAlreadyKnown, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		boolean cancelled = callEvent(spell, target, player);
		if (cancelled) {
			sendMessage(strCantLearn, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		targetSpellbook.addSpell(spell);
		targetSpellbook.save();

		sendMessage(spell.getStrOnTeach() == null ? strCastTarget : spell.getStrOnTeach(), target, data, "%s", spell.getName());
		sendMessage(strCastSelf, player, data, "%s", spell.getName());

		playSpellEffects(data);
		return new CastResult(PostCastAction.NO_MESSAGES, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			sender.sendMessage(strUsage);
			return true;
		}
		List<Player> players = Bukkit.matchPlayer(args[0]);
		if (players.size() != 1) {
			sender.sendMessage(strNoTarget);
			return true;
		}
		Spell spell = MagicSpells.getSpellByName(args[1]);
		if (spell == null) {
			sender.sendMessage(strNoSpell);
			return true;
		}
		Spellbook targetSpellbook = MagicSpells.getSpellbook(players.get(0));
		if (!targetSpellbook.canLearn(spell)) {
			sender.sendMessage(strCantLearn);
			return true;
		}
		if (targetSpellbook.hasSpell(spell)) {
			sender.sendMessage(strAlreadyKnown);
			return true;
		}
		boolean cancelled = callEvent(spell, players.get(0), sender);
		if (cancelled) {
			sender.sendMessage(strCantLearn);
			return true;
		}

		targetSpellbook.addSpell(spell);
		targetSpellbook.save();

		String consoleName = MagicSpells.getConsoleName();
		String displayName = Util.getStringFromComponent(players.get(0).displayName());
		sendMessage(spell.getStrOnTeach() == null ? strCastTarget : spell.getStrOnTeach(), players.get(0), args, "%a", consoleName, "%s", spell.getName(), "%t", displayName);
		sender.sendMessage(formatMessage(strCastSelf, "%a", consoleName, "%s", spell.getName(), "%t", displayName));
		return true;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();
		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());

		CommandInput original = input.copy();

		String playerName = input.readString();
		if (playerName.isEmpty() || Bukkit.getPlayer(playerName) == null) {
			return TxtUtil.tabCompletePlayerName(executor,
				!(executor instanceof Player caster) || !requireKnownSpell.get(new SpellData(caster)));
		}

		if (executor instanceof Player caster && requireKnownSpell.get(new SpellData(caster))) {
			if (!input.isEmpty() || input.input().endsWith(" ")) {
				String diff = original.difference(input.skipWhitespace(), true);
				Spellbook spellbook = MagicSpells.getSpellbook(caster);

				return OwnedSpellParser.suggest(caster, spellbook::canTeach).stream()
					.map(s -> diff + s)
					.toList();
			}

			return TxtUtil.tabCompletePlayerName(executor, false);
		}

		if (!input.isEmpty() || input.input().endsWith(" ")) {
			String diff = original.difference(input.skipWhitespace(), true);

			return SpellParser.suggest().stream()
				.map(s -> diff + s)
				.toList();
		}

		return TxtUtil.tabCompletePlayerName(executor, true);
	}

	private boolean callEvent(Spell spell, Player learner, Object teacher) {
		SpellLearnEvent event = new SpellLearnEvent(spell, learner, LearnSource.TEACH, teacher);
		EventUtil.call(event);
		return event.isCancelled();
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

	public String getStrCantTeach() {
		return strCantTeach;
	}

	public void setStrCantTeach(String strCantTeach) {
		this.strCantTeach = strCantTeach;
	}

	public String getStrCantLearn() {
		return strCantLearn;
	}

	public void setStrCantLearn(String strCantLearn) {
		this.strCantLearn = strCantLearn;
	}

	public String getStrCastTarget() {
		return strCastTarget;
	}

	public void setStrCastTarget(String strCastTarget) {
		this.strCastTarget = strCastTarget;
	}

	public String getStrAlreadyKnown() {
		return strAlreadyKnown;
	}

	public void setStrAlreadyKnown(String strAlreadyKnown) {
		this.strAlreadyKnown = strAlreadyKnown;
	}

}
