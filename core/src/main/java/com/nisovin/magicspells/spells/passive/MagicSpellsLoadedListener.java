package com.nisovin.magicspells.spells.passive;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class MagicSpellsLoadedListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onLoaded(MagicSpellsLoadedEvent e) {
		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity livingEntity : world.getLivingEntities()) {
				if (!canTrigger(livingEntity)) continue;
				if (!hasSpell(livingEntity)) continue;
				passiveSpell.activate(livingEntity);
			}
		}
	}

}
