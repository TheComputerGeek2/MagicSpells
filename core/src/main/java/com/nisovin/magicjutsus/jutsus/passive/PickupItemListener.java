package com.nisovin.magicjutsus.jutsus.passive;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;

// Optional trigger variable that is a comma separated list of items to accept
public class PickupItemListener extends PassiveListener {

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
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player pl = (Player) event.getEntity();
		if (!allTypes.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(pl);
			for (PassiveJutsu jutsu : allTypes) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(pl);
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
		
		if (!types.isEmpty()) {
			List<PassiveJutsu> list = getJutsus(event.getItem().getItemStack());
			if (list != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(pl);
				for (PassiveJutsu jutsu : list) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu)) continue;
					boolean casted = jutsu.activate(pl);
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
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
