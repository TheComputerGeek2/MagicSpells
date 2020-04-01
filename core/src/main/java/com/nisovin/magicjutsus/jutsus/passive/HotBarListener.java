package com.nisovin.magicjutsus.jutsus.passive;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.LinkedHashMap;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerItemHeldEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;

// Trigger variable is the item to trigger on
public class HotBarListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<MagicMaterial, List<PassiveJutsu>> select = new LinkedHashMap<>();
	Map<MagicMaterial, List<PassiveJutsu>> deselect = new LinkedHashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		MagicMaterial mat = null;
		if (var != null) {
			if (var.contains("|")) {
				String[] stuff = var.split("\\|");
				mat = MagicJutsus.getItemNameResolver().resolveItem(stuff[0]);
				if (mat != null) mat = new MagicItemWithNameMaterial(mat, stuff[1]);
			} else {
				mat = MagicJutsus.getItemNameResolver().resolveItem(var);
			}
		}
		if (mat != null) {
			materials.add(mat.getMaterial());
			List<PassiveJutsu> list = null;
			if (PassiveTrigger.HOT_BAR_SELECT.contains(trigger)) {
				list = select.computeIfAbsent(mat, material -> new ArrayList<>());
			} else if (PassiveTrigger.HOT_BAR_DESELECT.contains(trigger)) {
				list = deselect.computeIfAbsent(mat, material -> new ArrayList<>());
			}
			if (list != null) list.add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onPlayerScroll(PlayerItemHeldEvent event) {
		if (!deselect.isEmpty()) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
			if (item != null && item.getType() != Material.AIR) {
				List<PassiveJutsu> list = getJutsus(item, deselect);
				if (list != null) {
					Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
					for (PassiveJutsu jutsu : list) {
						if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
						if (!jutsubook.hasJutsu(jutsu, false)) continue;
						boolean casted = jutsu.activate(event.getPlayer());
						if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
						event.setCancelled(true);
					}
				}
			}
		}
		if (!select.isEmpty()) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
			if (item != null && item.getType() != Material.AIR) {
				List<PassiveJutsu> list = getJutsus(item, select);
				if (list != null) {
					Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
					for (PassiveJutsu jutsu : list) {
						if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
						if (!jutsubook.hasJutsu(jutsu, false)) continue;
						boolean casted = jutsu.activate(event.getPlayer());
						if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	private List<PassiveJutsu> getJutsus(ItemStack item, Map<MagicMaterial, List<PassiveJutsu>> map) {
		if (!materials.contains(item.getType())) return null;
		for (Entry<MagicMaterial, List<PassiveJutsu>> entry : map.entrySet()) {
			if (entry.getKey().equals(item)) return entry.getValue();
		}
		return null;
	}

}
