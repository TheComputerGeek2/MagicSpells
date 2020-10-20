package com.nisovin.magicspells.listeners;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.managers.VariableManager;

import com.google.common.collect.Multimap;

public class VariableListener implements Listener {

	private final VariableManager variableManager;

	public VariableListener() {
		variableManager = MagicSpells.getVariableManager();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		variableManager.loadPlayerVariables(player.getName(), Util.getUniqueId(player));
		variableManager.loadBossBars(player);
		MagicSpells.scheduleDelayedTask(() -> variableManager.loadExpBar(player), 10);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (!variableManager.getDirtyPlayerVariables().contains(player.getName())) return;
		variableManager.savePlayerVariables(player.getName(), Util.getUniqueId(player));
	}

	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell cast
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCast(SpellCastEvent event) {
		if (event.getSpellCastState() != Spell.SpellCastState.NORMAL) return;
		Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsCast();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player player = (Player) caster;
		for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
			VariableMod mod = entry.getValue();
			if (mod == null) continue;

			String amount = variableManager.processVariableMods(entry.getKey(), mod, player, player, null);
			MagicSpells.debug(3, "Variable '" + entry.getKey() + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell cast '" + event.getSpell().getName() + "'");
		}
	}

	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell casted
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCasted(SpellCastedEvent event) {
		if (event.getSpellCastState() != Spell.SpellCastState.NORMAL || event.getPostCastAction() == Spell.PostCastAction.ALREADY_HANDLED) return;
		Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsCasted();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player player = (Player) caster;
		for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
			VariableMod mod = entry.getValue();
			if (mod == null) continue;

			String amount = variableManager.processVariableMods(entry.getKey(), mod, player, player, null);
			MagicSpells.debug(3, "Variable '" + entry.getKey() + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell casted '" + event.getSpell().getName() + "'");
		}
	}

	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell target
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsTarget(SpellTargetEvent event) {
		Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsTarget();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player target = event.getTarget() instanceof Player ? (Player) event.getTarget() : null;
		if (target == null) return;
		for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
			VariableMod mod = entry.getValue();
			if (mod == null) continue;

			String amount = variableManager.processVariableMods(entry.getKey(), mod, target, (Player) caster, target);
			MagicSpells.debug(3, "Variable '" + entry.getKey() + "' for player '" + target.getName() + "' modified by " + amount + " as a result of spell target from '" + event.getSpell().getName() + "'");
		}
	}

}
