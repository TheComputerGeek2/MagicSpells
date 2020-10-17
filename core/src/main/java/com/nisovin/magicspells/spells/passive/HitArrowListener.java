package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class HitArrowListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();
			MagicItem magicItem = MagicItems.getMagicItemFromString(s);
			MagicItemData itemData = null;
			if (magicItem != null) itemData = magicItem.getMagicItemData();
			if (itemData == null) continue;

			items.add(itemData);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		LivingEntity attacker = getAttacker(event);
		if (attacker == null || !(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity attacked = (LivingEntity) event.getEntity();
		if (!hasSpell(attacker)) return;
		if (!canTrigger(attacker)) return;

		if (items.isEmpty()) {
			if (!isCancelStateOk(event.isCancelled())) return;
			boolean casted = passiveSpell.activate(attacker, attacked);
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}

		ItemStack item = attacker.getEquipment().getItemInMainHand();
		if (item == null) return;
		if (BlockUtils.isAir(item.getType())) return;
		MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
		if (!contains(data)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(attacker, attacked);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}
	
	private LivingEntity getAttacker(EntityDamageByEntityEvent event) {
		Entity e = event.getDamager();
		if (!(e instanceof Arrow)) return null;
		if (((Arrow) e).getShooter() != null && ((Arrow) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Arrow) e).getShooter();
		}
		return null;
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.equals(itemData)) return true;
		}
		return false;
	}

}
