package com.nisovin.magicspells.spells.passive;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable is a semicolon separated list of locations to accept
// Locations follow the format of world,x,y,z
// Where "world" is a string and x, y, and z are integers
public class RightClickBlockCoordListener extends PassiveListener {

	private MagicLocation magicLocation;

	@Override
	public void initialize(String var) {
		String[] split = var.split(";");
		for (String s : split) {
			try {
				String[] data = s.split(",");
				String world = data[0];
				int x = Integer.parseInt(data[1]);
				int y = Integer.parseInt(data[2]);
				int z = Integer.parseInt(data[3]);
				magicLocation = new MagicLocation(world, x, y, z);
			} catch (NumberFormatException e) {
				MagicSpells.error("Invalid coords on rightClickBlockCoord trigger for spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Location location = event.getClickedBlock().getLocation();
		MagicLocation loc = new MagicLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

		if (event.getHand() != EquipmentSlot.HAND) return;
		if (!magicLocation.equals(loc)) return;

		if (!isCancelStateOk(isCancelled(event))) return;
		if (!hasSpell(event.getPlayer())) return;
		boolean casted = passiveSpell.activate(event.getPlayer(), location.add(0.5, 0.5, 0.5));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean isCancelled(PlayerInteractEvent event) {
		return event.useInteractedBlock() == Event.Result.DENY;
	}

}
