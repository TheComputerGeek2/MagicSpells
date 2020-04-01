package com.nisovin.magicjutsus.jutsus.passive;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Trigger variable accepts a comma separated list of blocks to accept
public class LeftClickBlockTypeListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<Material, List<PassiveJutsu>> types = new HashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Material.getMaterial(s.toUpperCase());
			if (m == null) {
				MagicJutsus.error("Invalid type on leftclickblocktype trigger '" + var + "' on passive jutsu '" + jutsu.getInternalName() + '\'');
				continue;
			}
			List<PassiveJutsu> list = types.computeIfAbsent(m, material -> new ArrayList<>());
			list.add(jutsu);
			materials.add(m);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		List<PassiveJutsu> list = getJutsus(event.getClickedBlock());
		if (list != null) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			for (PassiveJutsu jutsu : list) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu, false)) continue;
				boolean casted = jutsu.activate(event.getPlayer(), event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
	}

	private List<PassiveJutsu> getJutsus(Block block) {
		if (!materials.contains(block.getType())) return null;
		for (Entry<Material, List<PassiveJutsu>> entry : types.entrySet()) {
			if (entry.getKey().equals(block.getType())) return entry.getValue();
		}
		return null;
	}

}
