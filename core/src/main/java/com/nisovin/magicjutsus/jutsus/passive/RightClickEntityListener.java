package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.util.Util;
import org.bukkit.inventory.EquipmentSlot;

// Trigger variable option is optional
// If not defined, it will trigger regardless of entity type
// If specified, it should be a comma separated list of entity types to accept
public class RightClickEntityListener extends PassiveListener {

	Map<EntityType, List<PassiveJutsu>> types = new HashMap<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	Map<EntityType, List<PassiveJutsu>> typesOffhand = new HashMap<>();
	List<PassiveJutsu> allTypesOffhand = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		
		Map<EntityType, List<PassiveJutsu>> typeMapLocal;
		List<PassiveJutsu> allTypesLocal;
		
		if (isMainHand(trigger)) {
			typeMapLocal = types;
			allTypesLocal = allTypes;
		} else {
			typeMapLocal = typesOffhand;
			allTypesLocal = allTypesOffhand;
		}
		
		if (var == null || var.isEmpty()) {
			allTypesLocal.add(jutsu);
		} else {
			String[] split = var.replace(" ", "").toUpperCase().split(",");
			for (String s : split) {
				EntityType t = Util.getEntityType(s);
				if (t != null) {
					List<PassiveJutsu> list = typeMapLocal.computeIfAbsent(t, type -> new ArrayList<>());
					list.add(jutsu);
				}
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClickEntity(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof LivingEntity)) return;
		
		Map<EntityType, List<PassiveJutsu>> typeMapLocal;
		List<PassiveJutsu> allTypesLocal;
		
		if (event.getHand() == EquipmentSlot.HAND) {
			typeMapLocal = types;
			allTypesLocal = allTypes;
		} else {
			typeMapLocal = typesOffhand;
			allTypesLocal = allTypesOffhand;
		}
		
		if (!allTypesLocal.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			for (PassiveJutsu jutsu : allTypesLocal) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(event.getPlayer(), (LivingEntity)event.getRightClicked());
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
		if (typeMapLocal.containsKey(event.getRightClicked().getType())) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
			List<PassiveJutsu> list = typeMapLocal.get(event.getRightClicked().getType());
			for (PassiveJutsu jutsu : list) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(event.getPlayer(), (LivingEntity)event.getRightClicked());
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
	}
	
	public boolean isMainHand(PassiveTrigger trigger) {
		return PassiveTrigger.RIGHT_CLICK_ENTITY.contains(trigger);
	}

}
