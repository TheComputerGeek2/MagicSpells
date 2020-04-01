package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable that may contain a comma separated list of items to accept
public class DropItemListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<MagicMaterial, List<PassiveJutsu>> types = new HashMap<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(jutsu);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				s = s.trim();
				MagicMaterial mat;
				if (s.contains("|")) {
					String[] stuff = s.split("\\|");
					mat = MagicJutsus.getItemNameResolver().resolveItem(stuff[0]);
					if (mat != null) mat = new MagicItemWithNameMaterial(mat, stuff[1]);
				} else {
					mat = MagicJutsus.getItemNameResolver().resolveItem(s);
				}
				if (mat != null) {
					List<PassiveJutsu> list = types.computeIfAbsent(mat, material -> new ArrayList<>());
					list.add(jutsu);
					materials.add(mat.getMaterial());
				}
			}	
		}		
	}
	
	@OverridePriority
	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (!allTypes.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			for (PassiveJutsu jutsu : allTypes) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(event.getPlayer());
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
		
		if (!types.isEmpty()) {
			List<PassiveJutsu> list = getJutsus(event.getItemDrop().getItemStack());
			if (list != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : list) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu)) continue;
					boolean casted = jutsu.activate(event.getPlayer());
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
				}
			}
		}
	}
	
	private List<PassiveJutsu> getJutsus(ItemStack item) {
		if (!materials.contains(item.getType())) return null;
		for (Entry<MagicMaterial, List<PassiveJutsu>> entry : types.entrySet()) {
			if (entry.getKey().equals(item)) return entry.getValue();
		}
		return null;
	}

}
