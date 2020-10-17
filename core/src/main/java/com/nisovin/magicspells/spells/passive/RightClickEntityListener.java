package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable option is optional
// If not defined, it will trigger regardless of entity type
// If specified, it should be a comma separated list of entity types to accept
public class RightClickEntityListener extends PassiveListener {

	private final EnumSet<EntityType> entities = EnumSet.noneOf(EntityType.class);
	
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.replace(" ", "").toUpperCase().split(",");
		for (String s : split) {
			EntityType type = Util.getEntityType(s);
			if (type == null) continue;

			entities.add(type);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClickEntity(PlayerInteractAtEntityEvent event) {
		if (!(event.getRightClicked() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getRightClicked();
		if (event.getHand() != EquipmentSlot.HAND) return;
		if (!entities.isEmpty() && !entities.contains(entity.getType())) return;

		if (!hasSpell(event.getPlayer())) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(event.getPlayer(), entity);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

}
