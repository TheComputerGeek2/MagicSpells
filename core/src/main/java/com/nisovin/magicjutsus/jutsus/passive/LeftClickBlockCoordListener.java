package com.nisovin.magicjutsus.jutsus.passive;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.MagicLocation;
import com.nisovin.magicjutsus.util.OverridePriority;

// Trigger variable is a semicolon separated list of locations to accept
// The format of locations is world,x,y,z
// Where "world" is a string
// And x, y, and z are integers
public class LeftClickBlockCoordListener extends PassiveListener {

	Map<MagicLocation, PassiveJutsu> locs = new HashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		String[] split = var.split(";");
		for (String s : split) {
			try {
				String[] data = s.split(",");
				String world = data[0];
				int x = Integer.parseInt(data[1]);
				int y = Integer.parseInt(data[2]);
				int z = Integer.parseInt(data[3]);				
				locs.put(new MagicLocation(world, x, y, z), jutsu);
			} catch (NumberFormatException e) {
				MagicJutsus.error("Invalid coords on leftclickblockcoord trigger for jutsu '" + jutsu.getInternalName() + '\'');
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		Location location = event.getClickedBlock().getLocation();
		MagicLocation loc = new MagicLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		PassiveJutsu jutsu = locs.get(loc);
		if (jutsu != null) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) return;
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			if (jutsubook.hasJutsu(jutsu, false)) {
				boolean casted = jutsu.activate(event.getPlayer(), location.add(0.5, 0.5, 0.5));
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
	}

}
