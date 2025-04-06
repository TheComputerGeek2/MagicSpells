package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("missarrow")
public class MissArrowListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();
	
	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		for (String s : var.split(MagicItemDataParser.DATA_REGEX)) {
			s = s.trim();

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + s + "' in missarrow trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

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
		if (!(event.getEntity() instanceof Arrow arrow)) return;

		LivingEntity caster = getAttacker(event);
		if (caster == null || !canTrigger(caster)) return;

		String name = caster.getName();
		UUID id = caster.getUniqueId();
		
		if (!event.getEntity().hasMetadata("mal-" + id + '-' + name)) return;
		if (event.getEntity().getMetadata("mal-" + id + '-' + name).isEmpty()) return;
		
		ArrowParticle arrowParticle = (ArrowParticle) event.getEntity().getMetadata("mal-" + id + '-' + name).get(0).value();
		if (arrowParticle.isHitEntity()) return;

		if (!items.isEmpty()) {
			ItemStack item = arrow.getWeapon();
			if (item == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
			if (!contains(itemData)) return;
		}

		passiveSpell.activate(caster, event.getEntity().getLocation());
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
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
		if (event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof LivingEntity p) || !(event.getEntity() instanceof Arrow)) return;
		ArrowParticle arrowParticle = new ArrowParticle(p);
		event.getEntity().setMetadata("mal-" + p.getUniqueId() + '-' + p.getName(), new FixedMetadataValue(MagicSpells.getInstance(), arrowParticle));
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
