package com.nisovin.magicspells.spells.command;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

// Advanced perm allows you to make others forget a spell
// Put * for the spell to forget all of them

public class ForgetSpell extends CommandSpell {

	private boolean allowSelfForget;

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
		
		allowSelfForget = getConfigBoolean("allow-self-forget", true);

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
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0 || args.length > 2) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Spellbook casterSpellbook = MagicSpells.getSpellbook(player);
			
			Player target;
			if (args.length == 1 && allowSelfForget) target = player;
			else if (args.length == 2 && casterSpellbook.hasAdvancedPerm("forget")) {
				List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
				if (players.size() != 1) {
					sendMessage(strNoTarget, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				target = players.get(0);
			} else {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			String spellName = args.length == 1 ? args[0] : args[1];
			boolean all = false;
			Spell spell = null;
			if (spellName.equals("*")) all = true;
			else spell = MagicSpells.getSpellByInGameName(spellName);

			if (spell == null && !all) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (!all && !casterSpellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
			if (targetSpellbook == null || (!all && !targetSpellbook.hasSpell(spell))) {
				sendMessage(strDoesntKnow, player, args);
				return PostCastAction.ALREADY_HANDLED;
			} 
			
			// Remove spell(s)
			if (!all) {
				targetSpellbook.removeSpell(spell);
				targetSpellbook.save();
				if (!player.equals(target)) {
					sendMessage(formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()), target, args);
					sendMessage(formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()), player, args);
					playSpellEffects(player, target);
				} else {
					sendMessage(formatMessage(strCastSelfTarget, "%s", spell.getName()), player, args);
					playSpellEffects(EffectPosition.CASTER, player);
				}
				return PostCastAction.NO_MESSAGES;
			}

			targetSpellbook.removeAllSpells();
			targetSpellbook.save();

			if (!player.equals(target)) {
				sendMessage(formatMessage(strResetTarget, "%t", target.getDisplayName()), player, args);
				playSpellEffects(player, target);
			} else {
				sendMessage(strResetSelf, player, args);
				playSpellEffects(EffectPosition.CASTER, player);
			}
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			sender.sendMessage(strUsage);
			return false;
		}
		Player target = PlayerNameUtils.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(strNoTarget);
			return false;
		}
		Spell spell = null;
		boolean all = false;
		if (args[1].equals("*")) all = true;
		else spell = MagicSpells.getSpellByInGameName(args[1]);

		if (spell == null && !all) {
			sender.sendMessage(strNoSpell);
			return false;
		}

		Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
		if (targetSpellbook == null || (!all && !targetSpellbook.hasSpell(spell))) {
			sender.sendMessage(strDoesntKnow);
			return false;
		}

		SpellForgetEvent forgetEvent = new SpellForgetEvent(spell, target);
		EventUtil.call(forgetEvent);
		if (forgetEvent.isCancelled()) return false;
		if (!all) {
			targetSpellbook.removeSpell(spell);
			targetSpellbook.save();
			sendMessage(formatMessage(strCastTarget, "%a", getConsoleName(), "%s", spell.getName(), "%t", target.getDisplayName()), target, args);
			sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", spell.getName(), "%t", target.getDisplayName()));
		} else {
			targetSpellbook.removeAllSpells();
			targetSpellbook.save();
			sender.sendMessage(formatMessage(strResetTarget, "%t", target.getDisplayName()));
		}
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) {
			// Matching player name or spell name
			List<String> options = new ArrayList<>();
			List<String> players = tabCompletePlayerName(sender, args[0]);
			List<String> spells = tabCompleteSpellName(sender, args[0]);
			if (players != null) options.addAll(players);
			if (spells != null) options.addAll(spells);
			if (!options.isEmpty()) return options;
		}

		if (args.length == 2) return tabCompleteSpellName(sender, args[1]);
		return null;
	}

}
