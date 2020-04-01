package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Trigger variable of a comma separated list of items to accept
public class RightClickItemListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<MagicMaterial, List<PassiveJutsu>> types = new LinkedHashMap<>();
	
	Set<Material> materialsOffhand = new HashSet<>();
	Map<MagicMaterial, List<PassiveJutsu>> typesOffhand = new LinkedHashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null) {
			MagicJutsus.error(trigger.getName() + " cannot accept a null variable");
			return;
		}
		Set<Material> materialSetAddTo;
		Map<MagicMaterial, List<PassiveJutsu>> typesMapAddTo;
		if (isMainHand(trigger)) {
			materialSetAddTo = materials;
			typesMapAddTo = types;
		} else {
			materialSetAddTo = materialsOffhand;
			typesMapAddTo = typesOffhand;
		}
		
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			MagicMaterial mat = null;
			if (s.contains("|")) {
				String[] stuff = s.split("\\|");
				mat = MagicJutsus.getItemNameResolver().resolveItem(stuff[0]);
				if (mat != null) mat = new MagicItemWithNameMaterial(mat, stuff[1]);
			} else {
				mat = MagicJutsus.getItemNameResolver().resolveItem(s);
			}
			if (mat != null) {
				List<PassiveJutsu> list = typesMapAddTo.computeIfAbsent(mat, m -> new ArrayList<>());
				list.add(jutsu);
				materialSetAddTo.add(mat.getMaterial());
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;
		
		ItemStack item = event.getItem();
		if (item == null || item.getType() == Material.AIR) return;
		List<PassiveJutsu> list = getJutsus(item, event.getHand() == EquipmentSlot.HAND);
		if (list != null) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			for (PassiveJutsu jutsu : list) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu, false)) continue;
				boolean casted = jutsu.activate(event.getPlayer());
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
	}
	
	private List<PassiveJutsu> getJutsus(ItemStack item, boolean mainHand) {
		Set<Material> materialSet;
		Map<MagicMaterial, List<PassiveJutsu>> jutsuMap;
		if (mainHand) {
			materialSet = materials;
			jutsuMap = types;
		} else {
			materialSet = materialsOffhand;
			jutsuMap = typesOffhand;
		}
		
		if (materialSet.contains(item.getType())) {
			for (Entry<MagicMaterial, List<PassiveJutsu>> entry : jutsuMap.entrySet()) {
				if (entry.getKey().equals(item)) return entry.getValue();
			}
		}
		return null;
	}
	
	private boolean isMainHand(PassiveTrigger trigger) {
		return PassiveTrigger.RIGHT_CLICK.contains(trigger);
	}

}
