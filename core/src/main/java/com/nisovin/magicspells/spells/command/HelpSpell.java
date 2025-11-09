package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Collections;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

public class HelpSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private final ConfigData<Boolean> requireKnownSpell;

	private String strUsage;
	private String strNoSpell;
	private String strDescLine;
	private String strCostLine;

	public HelpSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requireKnownSpell = getConfigDataBoolean("require-known-spell", true);

		strUsage = getConfigString("str-usage", "Usage: /cast " + name + " <spell>");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strDescLine = getConfigString("str-desc-line", "%s - %d");
		strCostLine = getConfigString("str-cost-line", "Cost: %c");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spell spell = MagicSpells.getSpellByName(Util.arrayJoin(data.args(), ' '));
		Spellbook spellbook = MagicSpells.getSpellbook(caster);

		if (spell == null || requireKnownSpell.get(data) && !spellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		sendMessage(strDescLine, caster, data, "%s", spell.getName(), "%d", spell.getDescription());

		if (spell.getCostStr() != null && !spell.getCostStr().isEmpty())
			sendMessage(strCostLine, caster, data, "%c", spell.getCostStr());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (!(executor instanceof Player caster)) return Collections.emptyList();

		return requireKnownSpell.get(new SpellData(caster)) ? OwnedSpellParser.suggest(caster) : SpellParser.suggest();
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

	public String getStrDescLine() {
		return strDescLine;
	}

	public void setStrDescLine(String strDescLine) {
		this.strDescLine = strDescLine;
	}

	public String getStrCostLine() {
		return strCostLine;
	}

	public void setStrCostLine(String strCostLine) {
		this.strCostLine = strCostLine;
	}

}
