package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class MissArrowListener extends PassiveListener {

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
	public void onHitEntity(EntityDamageByEntityEvent event) {
		LivingEntity attacker = getAttacker(event);
		if (attacker == null) return;
		String name = attacker.getName();
		UUID id = attacker.getUniqueId();
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata("mal-" + id + '-' + name)
				&& !event.getEntity().getMetadata("mal-" + id + '-' + name).isEmpty()) {
			((ArrowParticle) event.getDamager().getMetadata("mal-" + id + '-' + name).get(0).value()).setHitEntity(true);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDamage(ProjectileHitEvent event) {
		LivingEntity attacker = getAttacker(event);
		if (attacker == null) return;
		if (!(event.getEntity() instanceof Arrow)) return;

		String name = attacker.getName();
		UUID id = attacker.getUniqueId();
		
		if (!event.getEntity().hasMetadata("mal-" + id + '-' + name)) return;
		if (event.getEntity().getMetadata("mal-" + id + '-' + name).isEmpty()) return;
		
		ArrowParticle arrowParticle = (ArrowParticle) event.getEntity().getMetadata("mal-" + id + '-' + name).get(0).value();

		if (arrowParticle.isHitEntity()) return;
		if (!hasSpell(attacker)) return;
		if (!canTrigger(attacker)) return;
		
		if (items.isEmpty()) {
			passiveSpell.activate(attacker, event.getEntity().getLocation());
			return;
		}

		ItemStack item = attacker.getEquipment().getItemInMainHand();
		
		if (item == null) return;
		if (item.getType().isAir()) return;

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (!contains(itemData)) return;

		passiveSpell.activate(attacker, event.getEntity().getLocation());
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.equals(itemData)) return true;
		}
		return false;
	}
	
	private LivingEntity getAttacker(ProjectileHitEvent event) {
		Projectile e = event.getEntity();
		if (!(e instanceof Arrow)) return null;
		if (e.getShooter() != null && e.getShooter() instanceof LivingEntity) {
			return (LivingEntity) e.getShooter();
		}
		return null;
	}

	private LivingEntity getAttacker(EntityDamageByEntityEvent event) {
		Entity e = event.getDamager();
		if (!(e instanceof Arrow)) return null;
		if (((Arrow) e).getShooter() != null && ((Arrow) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Arrow) e).getShooter();
		}
		return null;
	}
	
	@EventHandler
	public void shoot(ProjectileLaunchEvent event) {
		if (event.getEntity() != null && event.getEntity().getShooter() != null
				&& event.getEntity().getShooter() instanceof LivingEntity && event.getEntity() instanceof Arrow) {
			LivingEntity p = (LivingEntity) event.getEntity().getShooter();
			ArrowParticle arrowParticle = new ArrowParticle(p);
			event.getEntity().setMetadata("mal-" + p.getUniqueId() + '-' + p.getName(), new FixedMetadataValue(MagicSpells.getInstance(), arrowParticle));
		}
	}
	
	private static class ArrowParticle {

		private LivingEntity origCaster;
		private boolean hitEntity;
		
		private ArrowParticle(LivingEntity origCaster) {
			this.origCaster = origCaster;
		}
		
		public LivingEntity getOrigCaster() {
			return origCaster;
		}
		
		public boolean isHitEntity() {
			return hitEntity;
		}
		
		public void setHitEntity(boolean hitEntity) {
			this.hitEntity = hitEntity;
		}

	}

}
