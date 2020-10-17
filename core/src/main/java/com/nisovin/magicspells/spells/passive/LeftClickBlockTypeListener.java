package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable accepts a comma separated list of blocks to accept
public class LeftClickBlockTypeListener extends PassiveListener {

	private final EnumSet<Material> materials = EnumSet.noneOf(Material.class);

	@Override
	public void initialize(String var) {
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Util.getMaterial(s);
			if (m == null) {
				MagicSpells.error("Invalid type on leftClickBlockType trigger '" + var + "' on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			materials.add(m);
		}
	}

	@OverridePriority
	@EventHandler
	public void onLeftClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (!materials.isEmpty() && !materials.contains(event.getClickedBlock().getType())) return;

		if (!hasSpell(event.getPlayer())) return;
		if (!isCancelStateOk(isCancelled(event))) return;
		boolean casted = passiveSpell.activate(event.getPlayer(), event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

	private boolean isCancelled(PlayerInteractEvent event) {
		return event.useInteractedBlock() == Event.Result.DENY;
	}

}
