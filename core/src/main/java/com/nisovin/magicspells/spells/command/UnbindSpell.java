package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.ArrayList;
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
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.commands.parsers.SpellParser;

public class UnbindSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private Set<Spell> allowedSpells;

	private String strUsage;
	private String strNoSpell;
	private String strNotBound;
	private String strUnbindAll;
	private String strCantUnbind;

	public UnbindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<String> allowedSpellsNames = getConfigStringList("allowed-spells", null);
		if (allowedSpellsNames != null && !allowedSpellsNames.isEmpty()) {
			allowedSpells = new HashSet<>();
			for (String n: allowedSpellsNames) {
				Spell s = MagicSpells.getSpellByInternalName(n);
				if (s != null) allowedSpells.add(s);
				else MagicSpells.plugin.getLogger().warning("Invalid spell defined: " + n);
			}
		}

		strUsage = getConfigString("str-usage", "You must specify a spell name.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strNotBound = getConfigString("str-not-bound", "That spell is not bound to that item.");
		strUnbindAll = getConfigString("str-unbind-all", "All spells from your item were cleared.");
		strCantUnbind = getConfigString("str-cant-unbind", "You cannot unbind this spell.");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		
		if (!data.hasArgs()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		CastItem item = new CastItem(caster.getEquipment().getItemInMainHand());
		Spellbook spellbook = MagicSpells.getSpellbook(caster);

		if (data.args()[0] != null && data.args()[0].equalsIgnoreCase("*")) {
			List<Spell> spells = new ArrayList<>();

			for (CastItem i : spellbook.getItemSpells().keySet()) {
				if (!i.equals(item)) continue;
				spells.addAll(spellbook.getItemSpells().get(i));
			}

			for (Spell s : spells) {
				spellbook.removeCastItem(s, item);
			}

			spellbook.save();
			sendMessage(strUnbindAll, caster, data);
			playSpellEffects(EffectPosition.CASTER, caster, data);

			return new CastResult(PostCastAction.NO_MESSAGES, data);
		}

		Spell spell = MagicSpells.getSpellByName(Util.arrayJoin(data.args(), ' '));
		if (spell == null) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!spellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (allowedSpells != null && !allowedSpells.contains(spell)) {
			sendMessage(strCantUnbind, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		boolean removed = spellbook.removeCastItem(spell, item);
		if (!removed) {
			sendMessage(strNotBound, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		spellbook.save();
		sendMessage(strCastSelf, caster, data, "%s", spell.getName());
		playSpellEffects(EffectPosition.CASTER, caster, data);

		return new CastResult(PostCastAction.NO_MESSAGES, data);
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

		Spellbook spellbook = MagicSpells.getSpellbook(caster);
		CastItem castItem = new CastItem(caster.getInventory().getItemInMainHand());

		List<Spell> spells = spellbook.getItemSpells().get(castItem);
		if (spells == null || spells.isEmpty()) return Collections.emptyList();

		List<String> suggestions = new ArrayList<>();
		for (Spell spell : spells) {
			if (allowedSpells != null && !allowedSpells.contains(spell)) continue;
			suggestions.add(SpellParser.escapeIfRequired(Util.getPlainString(spell.getName())));
		}
		if (!suggestions.isEmpty()) suggestions.add("*");

		return suggestions;
	}

	public Set<Spell> getAllowedSpells() {
		return allowedSpells;
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

	public String getStrNotBound() {
		return strNotBound;
	}

	public void setStrNotBound(String strNotBound) {
		this.strNotBound = strNotBound;
	}

	public String getStrUnbindAll() {
		return strUnbindAll;
	}

	public void setStrUnbindAll(String strUnbindAll) {
		this.strUnbindAll = strUnbindAll;
	}

	public String getStrCantUnbind() {
		return strCantUnbind;
	}

	public void setStrCantUnbind(String strCantUnbind) {
		this.strCantUnbind = strCantUnbind;
	}

}
