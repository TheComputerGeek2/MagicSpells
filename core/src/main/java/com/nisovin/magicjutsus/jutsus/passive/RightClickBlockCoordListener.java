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
import org.bukkit.inventory.EquipmentSlot;

// Trigger variable is a semicolon separated list of locations to accept
// Locations follow the format of world,x,y,z
// Where "world" is a string and x, y, and z are integers
public class RightClickBlockCoordListener extends PassiveListener {

	Map<MagicLocation, PassiveJutsu> locs = new HashMap<>();
	Map<MagicLocation, PassiveJutsu> offhandLocs = new HashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		Map<MagicLocation, PassiveJutsu> addTo;
		if (isMainHandListener(trigger)) {
			addTo = locs;
		} else {
			addTo = offhandLocs;
		}
		String[] split = var.split(";");
		for (String s : split) {
			try {
				String[] data = s.split(",");
				String world = data[0];
				int x = Integer.parseInt(data[1]);
				int y = Integer.parseInt(data[2]);
				int z = Integer.parseInt(data[3]);				
				addTo.put(new MagicLocation(world, x, y, z), jutsu);
			} catch (NumberFormatException e) {
				MagicJutsus.error("Invalid coords on rightclickblockcoord trigger for jutsu '" + jutsu.getInternalName() + '\'');
			}
		}
	}
	
	public boolean isMainHandListener(PassiveTrigger trigger) {
		return PassiveTrigger.RIGHT_CLICK_BLOCK_COORD.contains(trigger);
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Location location = event.getClickedBlock().getLocation();
		MagicLocation loc = new MagicLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		
		PassiveJutsu jutsu;
		if (event.getHand() == EquipmentSlot.HAND) {
			jutsu = locs.get(loc);
		} else {
			jutsu = offhandLocs.get(loc);
		}
		
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
