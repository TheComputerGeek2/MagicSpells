package com.nisovin.magicspells.spells.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class BindSpell extends CommandSpell {
	
	private HashSet<CastItem> bindableItems;
	private boolean allowBindToFist;
	private String strUsage;
	private String strNoSpell;
	private String strCantBindSpell;
	private String strCantBindItem;
	private String strSpellCantBind;
	private Set<Spell> allowedSpells = null;

	public BindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> bindables = getConfigStringList("bindable-items", null);
		if (bindables != null) {
			this.bindableItems = new HashSet<>();
			for (String s : bindables) {
				this.bindableItems.add(new CastItem(s));
			}
		}
		this.allowBindToFist = getConfigBoolean("allow-bind-to-fist", false);
		this.strUsage = getConfigString("str-usage", "You must specify a spell name and hold an item in your hand.");
		this.strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		this.strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
		this.strCantBindItem = getConfigString("str-cant-bind-item", "That spell cannot be bound to that item.");
		this.strSpellCantBind = getConfigString("str-spell-cant-bind", "That spell cannot be bound like this.");
		List<String> allowedSpellNames = getConfigStringList("allowed-spells", null);
		if (allowedSpellNames != null && !allowedSpellNames.isEmpty()) {
			this.allowedSpells = new HashSet<>();
			for (String name: allowedSpellNames) {
				Spell s = MagicSpells.getSpellByInternalName(name);
				if (s != null) {
					this.allowedSpells.add(s);
				} else {
					MagicSpells.plugin.getLogger().warning("Invalid spell listed: " + name);
				}
			}
		}
	}
	
	// DEBUG INFO: level 3, trying to bind spell internalname to cast item castitemstring
	// DEBUG INFO: level 3, performing bind
	// DEBUG INFO: level 3, bind successful
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length == 0) {
				sendMessage(this.strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			} else { // TODO is the 'else' really needed explicitly?
				Spell spell = MagicSpells.getSpellByInGameName(Util.arrayJoin(args, ' '));
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				if (spell == null || spellbook == null) {
					// Fail - no such spell, or no spellbook
					sendMessage(this.strNoSpell, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spellbook.hasSpell(spell)) {
					// Fail - doesn't know spell
					sendMessage(this.strNoSpell, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spell.canCastWithItem()) {
					// Fail - spell can't be bound
					sendMessage(this.strCantBindSpell, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} else if (this.allowedSpells != null && !this.allowedSpells.contains(spell)) {
					// Fail - spell isn't allowed to be bound by this bind spell
					sendMessage(this.strSpellCantBind, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} else { // TODO is the 'else' really needed explicitly?
					CastItem castItem = new CastItem(HandHandler.getItemInMainHand(player));
					MagicSpells.debug(3, "Trying to bind spell '" + spell.getInternalName() + "' to cast item " + castItem.toString() + "...");
					if (castItem.getItemType() == Material.AIR && !this.allowBindToFist) {
						sendMessage(this.strCantBindItem, player, args);
						return PostCastAction.ALREADY_HANDLED;
					} else if (this.bindableItems != null && !this.bindableItems.contains(castItem)) {
						sendMessage(this.strCantBindItem, player, args);
						return PostCastAction.ALREADY_HANDLED;
					} else if (!spell.canBind(castItem)) {
						String msg = spell.getCantBindError();
						if (msg == null) msg = this.strCantBindItem;
						sendMessage(msg, player, args);
						return PostCastAction.ALREADY_HANDLED;
					} else {
						MagicSpells.debug(3, "    Performing bind...");
						spellbook.addCastItem(spell, castItem);
						spellbook.save();
						MagicSpells.debug(3, "    Bind successful.");
						sendMessage(formatMessage(this.strCastSelf, "%s", spell.getName()), player, args);
						playSpellEffects(EffectPosition.CASTER, player);
						return PostCastAction.NO_MESSAGES;
					}
				}
			}
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player) {
			// Only one arg
			if (partial.contains(" ")) return null;
			
			// Tab complete spellname from spellbook
			return tabCompleteSpellName(sender, partial);
		}
		return null;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

}
