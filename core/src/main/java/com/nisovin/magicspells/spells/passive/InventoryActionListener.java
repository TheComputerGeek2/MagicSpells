package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class InventoryActionListener extends PassiveListener {

	private final EnumSet<InventoryAction> actions = EnumSet.noneOf(InventoryAction.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.replace(" ", "").split(",");
		for (String s : split) {
			try {
				InventoryAction action = InventoryAction.valueOf(s.toUpperCase());
				actions.add(action);
			} catch (Exception e) {
				// ignored
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!actions.isEmpty() && !actions.contains(InventoryAction.OPEN)) return;
		HumanEntity humanEntity = event.getPlayer();
		if (!(humanEntity instanceof Player)) return;
		Player player = (Player) humanEntity;

		if (!hasSpell(player)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

	@OverridePriority
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!actions.isEmpty() && !actions.contains(InventoryAction.CLOSE)) return;
		HumanEntity humanEntity = event.getPlayer();
		if (!(humanEntity instanceof Player)) return;
		Player player = (Player) humanEntity;
		if (!hasSpell(player)) return;

		passiveSpell.activate(player);
	}

	private enum InventoryAction {

		OPEN,

		CLOSE

	}

}
