package com.nisovin.magicjutsus.towny;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nisovin.magicjutsus.util.compat.CompatBasics;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.CombatUtil;

public class MagicJutsusTowny extends JavaPlugin implements Listener {
	
	private Set<Jutsu> disallowedInTowns = new HashSet<>();
	//TODO add jutsu filter to control what is allowed
	private Towny towny;
	
	@Override
	public void onEnable() {
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		Configuration config = getConfig();
		if (config.contains("disallowed-in-towns")) {
			List<String> list = config.getStringList("disallowed-in-towns");
			for (String s : list) {
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s);
				if (jutsu == null) {
					jutsu = MagicJutsus.getJutsuByInGameName(s);
				}
				if (jutsu != null) {
					disallowedInTowns.add(jutsu);
				} else {
					getLogger().warning("Could not find jutsu " + s);
				}
			}
		}
		
		Plugin townyPlugin = CompatBasics.getPlugin("Towny");
		if (townyPlugin != null) {
			towny = (Towny)townyPlugin;
			EventUtil.register(this, this);
		} else {
			getLogger().severe("Failed to find Towny");
			this.setEnabled(false);
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onJutsuTarget(JutsuTargetEvent event) {
		if (event.getCaster() == null) return;
		boolean friendlyJutsu = false;
		if (event.getJutsu() instanceof TargetedJutsu && event.getJutsu().isBeneficial()) friendlyJutsu = true;
		if (!friendlyJutsu && CombatUtil.preventDamageCall(towny, event.getCaster(), event.getTarget())) {
			event.setCancelled(true);
		} else if (friendlyJutsu && event.getTarget() instanceof Player && !CombatUtil.isAlly(event.getCaster().getName(), event.getTarget().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onJutsuCast(JutsuCastEvent event) {
		if (disallowedInTowns.contains(event.getJutsu())) {
			try {
				TownyWorld world = TownyUniverse.getDataSource().getWorld(event.getCaster().getWorld().getName());
				if (world != null && world.isUsingTowny()) {
					Coord coord = Coord.parseCoord(event.getCaster());
					if (world.getTownBlock(coord) != null) {
						event.setCancelled(true);
					}
				}
			} catch (NotRegisteredException e) {
			}
		}
	}
	
}
