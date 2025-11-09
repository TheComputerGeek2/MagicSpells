package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
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
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

public class BindSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private Set<CastItem> bindableItems;

	private Set<Spell> allowedSpells;

	private final ConfigData<Boolean> allowBindToFist;
	private final ConfigData<Boolean> checkCanCastWithItem;

	private String strUsage;
	private String strNoSpell;
	private String strCantBindItem;
	private String strCantBindSpell;
	private String strSpellCantBind;

	public BindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> bindables = getConfigStringList("bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String s : bindables) {
				bindableItems.add(new CastItem(s));
			}
		}

		List<String> allowedSpellNames = getConfigStringList("allowed-spells", null);
		if (allowedSpellNames != null && !allowedSpellNames.isEmpty()) {
			allowedSpells = new HashSet<>();
			for (String name: allowedSpellNames) {
				Spell s = MagicSpells.getSpellByInternalName(name);
				if (s != null) allowedSpells.add(s);
				else MagicSpells.plugin.getLogger().warning("Invalid spell listed: " + name);
			}
		}

		allowBindToFist = getConfigDataBoolean("allow-bind-to-fist", false);
		checkCanCastWithItem = getConfigDataBoolean("check-can-cast-with-item", true);

		strUsage = getConfigString("str-usage", "You must specify a spell name and hold an item in your hand.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantBindItem = getConfigString("str-cant-bind-item", "That spell cannot be bound to that item.");
		strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
		strSpellCantBind = getConfigString("str-spell-cant-bind", "That spell cannot be bound like this.");
	}
	
	// DEBUG INFO: level 3, trying to bind spell internalName to cast item castItemString
	// DEBUG INFO: level 3, performing bind
	// DEBUG INFO: level 3, bind successful
	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spell spell = MagicSpells.getSpellByName(Util.arrayJoin(data.args(), ' '));
		Spellbook spellbook = MagicSpells.getSpellbook(caster);

		if (spell == null) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (spell.isHelperSpell()) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!spellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (checkCanCastWithItem.get(data) && !spell.canCastWithItem()) {
			sendMessage(strCantBindSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (allowedSpells != null && !allowedSpells.contains(spell)) {
			sendMessage(strSpellCantBind, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		CastItem castItem = new CastItem(caster.getEquipment().getItemInMainHand());
		MagicSpells.debug(3, "Trying to bind spell '" + spell.getInternalName() + "' to cast item " + castItem + "...");

		if (castItem.getType().isAir() && !allowBindToFist.get(data)) {
			sendMessage(strCantBindItem, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (bindableItems != null && !bindableItems.contains(castItem)) {
			sendMessage(strCantBindItem, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!spell.canBind(castItem)) {
			String msg = spell.getCantBindError();
			if (msg == null) msg = strCantBindItem;

			sendMessage(msg, caster, data);
			return new CastResult(PostCastAction.NO_MESSAGES, data);
		}

		MagicSpells.debug(3, "    Performing bind...");
		spellbook.addCastItem(spell, castItem);
		spellbook.save();
		MagicSpells.debug(3, "    Bind successful.");

		sendMessage(strCastSelf, caster, data, "%s", Util.getStrictString(spell.getName()));
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

		CastItem castItem = new CastItem(caster.getInventory().getItemInMainHand());
		if (bindableItems != null && !bindableItems.contains(castItem)) return Collections.emptyList();

		return OwnedSpellParser.suggest(caster, spell -> {
			if (spell.isHelperSpell() || !spell.canCastWithItem()) return false;
			if (allowedSpells != null && !allowedSpells.contains(spell)) return false;
			return spell.canBind(castItem);
		});
	}

	public Set<CastItem> getBindableItems() {
		return bindableItems;
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

	public String getStrCantBindItem() {
		return strCantBindItem;
	}

	public void setStrCantBindItem(String strCantBindItem) {
		this.strCantBindItem = strCantBindItem;
	}

	public String getStrCantBindSpell() {
		return strCantBindSpell;
	}

	public void setStrCantBindSpell(String strCantBindSpell) {
		this.strCantBindSpell = strCantBindSpell;
	}

	public String getStrSpellCantBind() {
		return strSpellCantBind;
	}

	public void setStrSpellCantBind(String strSpellCantBind) {
		this.strSpellCantBind = strSpellCantBind;
	}

}
