package com.nisovin.magicspells.towny;

import java.io.File;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.compat.CompatBasics;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

public class MagicSpellsTowny extends JavaPlugin implements Listener {
	
	private Set<Spell> disallowedInTowns = new HashSet<>();
	//TODO add spell filter to control what is allowed
	private Towny towny;
	
	@Override
	public void onEnable() {
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) saveDefaultConfig();

		Configuration config = getConfig();
		if (config.contains("disallowed-in-towns")) {
			List<String> list = config.getStringList("disallowed-in-towns");
			for (String s : list) {
				Spell spell = MagicSpells.getSpellByName(s);
				if (spell != null) disallowedInTowns.add(spell);
				else getLogger().warning("Could not find spell: '" + s + "'.");
			}
		}
		
		Plugin townyPlugin = CompatBasics.getPlugin("Towny");
		if (townyPlugin != null) {
			towny = (Towny) townyPlugin;
			Bukkit.getPluginManager().registerEvents(this, this);
		} else {
			getLogger().severe("Failed to find Towny.");
			setEnabled(false);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		LivingEntity caster = event.getCaster();
		LivingEntity target = event.getTarget();

		Spell spell = event.getSpell();

		if (caster == null) return;

		boolean friendlySpell = false;
		if (spell instanceof TargetedSpell && spell.isBeneficial()) friendlySpell = true;

		if (!friendlySpell && CombatUtil.preventDamageCall(caster, target, EntityDamageEvent.DamageCause.MAGIC)) {
			event.setCancelled(true);
		} else if (friendlySpell && target instanceof Player && !CombatUtil.isAlly(caster.getName(), target.getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onSpellCast(SpellCastEvent event) {
		if (!disallowedInTowns.contains(event.getSpell())) return;

		LivingEntity caster = event.getCaster();

		try {
			TownyWorld world = TownyUniverse.getInstance().getWorld(caster.getWorld().getName());
			if (world == null || !world.isUsingTowny()) return;

			Coord coord = Coord.parseCoord(caster);
			if (world.getTownBlock(coord) != null) event.setCancelled(true);

		} catch (NotRegisteredException ignored) { }
	}
	
}
