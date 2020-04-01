package com.nisovin.magicjutsus.memory;

import com.nisovin.magicjutsus.Jutsu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;

public class MemoryJutsuListener implements Listener {

	private MagicJutsusMemory plugin;
	
	public MemoryJutsuListener(MagicJutsusMemory plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onJutsuLearn(JutsuLearnEvent event) {
		Jutsu jutsu = event.getJutsu();
		int req = this.plugin.getRequiredMemory(jutsu);
		if (req > 0) {
			Player learner = event.getLearner();
			int mem = this.plugin.getMemoryRemaining(learner);
			MagicJutsus.debug("Memory check: " + req + " required, " + mem + " remaining");
			if (mem < req) {
				event.setCancelled(true);
				MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.plugin.strOutOfMemory, "%jutsu", jutsu.getName()), learner, (String[])null);
			}
		}
	}

}
