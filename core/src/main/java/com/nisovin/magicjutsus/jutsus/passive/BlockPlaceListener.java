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
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable of comma separated list of blocks to accept
public class BlockPlaceListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<Material, List<PassiveJutsu>> types = new HashMap<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(jutsu);
			return;
		}
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Material.getMaterial(s.toUpperCase());
			if (m == null) continue;
			List<PassiveJutsu> list = types.computeIfAbsent(m, material -> new ArrayList<>());
			list.add(jutsu);
			materials.add(m);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
		if (!allTypes.isEmpty()) {
			for (PassiveJutsu jutsu : allTypes) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu, false)) continue;
				boolean casted = jutsu.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
		if (!types.isEmpty()) {
			List<PassiveJutsu> list = getJutsus(event.getBlock());
			if (list != null) {
				for (PassiveJutsu jutsu : list) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
				}
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
