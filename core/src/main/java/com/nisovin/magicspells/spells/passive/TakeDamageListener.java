package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of a comma separated list that can contain
// Damage causes to accept or damaging weapons to accept
public class TakeDamageListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();
	private final EnumSet<DamageCause> damageCauses = EnumSet.noneOf(DamageCause.class);
	
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();
			boolean isDamCause = false;
			for (DamageCause c : DamageCause.values()) {
				if (!s.equalsIgnoreCase(c.name())) continue;

				damageCauses.add(c);
				isDamCause = true;
				break;
			}
			if (isDamCause) continue;

			MagicItem magicItem = MagicItems.getMagicItemFromString(s);
			MagicItemData itemData = null;
			if (magicItem != null) itemData = magicItem.getMagicItemData();
			if (itemData == null) continue;

			items.add(itemData);
		}
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();
		LivingEntity attacker = getAttacker(event);

		if (!hasSpell(entity)) return;
		if (!canTrigger(entity)) return;

		if (items.isEmpty() && damageCauses.isEmpty()) {
			if (!isCancelStateOk(event.isCancelled())) return;
			boolean casted = passiveSpell.activate(entity, attacker);
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}

		if (damageCauses.isEmpty() || damageCauses.contains(event.getCause())) {
			if (!isCancelStateOk(event.isCancelled())) return;
			boolean casted = passiveSpell.activate(entity, attacker);
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}
		
		if (!(attacker instanceof Player)) return;

		ItemStack item = attacker.getEquipment().getItemInMainHand();
		if (item == null) return;
		if (BlockUtils.isAir(item)) return;

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return;
		if (!items.isEmpty() && !contains(itemData)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(entity, attacker);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}
	
	private LivingEntity getAttacker(EntityDamageEvent event) {
		if (!(event instanceof EntityDamageByEntityEvent)) return null;
		Entity e = ((EntityDamageByEntityEvent) event).getDamager();
		if (e instanceof LivingEntity) return (LivingEntity) e;
		if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Projectile) e).getShooter();
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
