package com.nisovin.magicspells.listeners;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.pdc.UUIDTagType;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.effecttypes.*;
import com.nisovin.magicspells.util.pdc.PersistentDataEntry;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;

public class MagicSpellListener implements Listener {

	public static final PersistentDataEntry<byte[], UUID> PDC_CASTER = new PersistentDataEntry<>(UUIDTagType.INSTANCE, "caster");
	public static final PersistentDataEntry<Byte, Boolean> PDC_TARGETABLE = new PersistentDataEntry<>(PersistentDataType.BOOLEAN, "targetable");
	public static final PersistentDataEntry<Byte, Boolean> PDC_TARGETABLE_BY_CASTER = new PersistentDataEntry<>(PersistentDataType.BOOLEAN, "targetable_by_caster");

	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		Spell spell = event.getSpell();
		SpellData data = event.getSpellData();
		if (!data.hasTarget()) return;

		if (!isTargetable(data.target(), data.caster())) {
			event.setCancelled(true);
			return;
		}

		if (Perm.NO_TARGET.has(data.target()))  {
			event.setCancelled(true);
			return;
		}

		NoMagicZoneManager zoneManager = MagicSpells.getNoMagicZoneManager();
		if (spell != null && zoneManager != null && zoneManager.willFizzle(data.target(), spell))
			event.setCancelled(true);
	}

	private boolean isTargetable(Entity target, Entity caster) {
		PersistentDataContainer pdc = target.getPersistentDataContainer();

		if (caster != null && caster.getUniqueId().equals(PDC_CASTER.get(pdc))) {
			Boolean targetableByCaster = PDC_TARGETABLE_BY_CASTER.get(pdc);
			if (targetableByCaster != null) return targetableByCaster;
		}

		Boolean targetable = PDC_TARGETABLE.get(pdc);
		if (targetable != null) return targetable;

		return !isMSEntity(target);
	}

	private boolean isMSEntity(Entity entity) {
		return entity.getScoreboardTags().contains(ArmorStandEffect.ENTITY_TAG)
			|| entity.getScoreboardTags().contains(EntityEffect.ENTITY_TAG);
	}

	@EventHandler
	public void onProjectileHit(ParticleProjectileHitEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null) return;
		if (isMSEntity(target)) event.setCancelled(true);
	}

	/**
	 * This is for backwards compatibility for removing entities in unloaded chunks.
	 * @see Entity#setPersistent(boolean)
	 */
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!isMSEntity(entity)) continue;
			entity.remove();
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!isMSEntity(entity)) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onEntityRemove(EntityRemoveEvent event) {
		Util.forEachPassenger(event.getEntity(), passenger -> {
			PersistentDataContainer pdc = passenger.getPersistentDataContainer();
			if (!EntityData.MS_PASSENGER.has(pdc)) return;

			if (passenger.isPersistent()) {
				EntityData.MS_PASSENGER.remove(pdc);
				return;
			}

			passenger.remove();
		});
	}

	@EventHandler
	public void onEntityDismount(EntityDismountEvent event) {
		EntityData.MS_PASSENGER.remove(event.getEntity().getPersistentDataContainer());
	}

	@EventHandler
	public void onFireworkDamage(EntityDamageByEntityEvent event) {
		if (!FireworksEffect.MS_FIREWORK.has(event.getDamager().getPersistentDataContainer())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onInvPickup(InventoryPickupItemEvent event) {
		if (!ItemSprayEffect.MS_ITEM_SPRAY.has(event.getItem().getPersistentDataContainer())) return;
		event.setCancelled(true);
	}

}
