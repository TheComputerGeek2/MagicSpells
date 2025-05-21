package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;

import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;

public class InvisibilitySpell extends BuffSpell {

	private static InvisibilityManager manager;

	private final ConfigData<Double> mobRadius;

	private final ConfigData<Boolean> preventPickups;

	public InvisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		mobRadius = getConfigDataDouble("mob-radius", 30);

		preventPickups = getConfigDataBoolean("prevent-pickups", true);

		if (manager == null) manager = new InvisibilityManager();
	}

	@Override
	public boolean castBuff(SpellData data) {
		manager.castBuff(this, data.target(), mobRadius.get(data), preventPickups.get(data));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return manager.isActive(this, entity);
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		manager.turnOffBuff(this, entity);
	}

	@Override
	protected void turnOff() {
		super.turnOff();

		if (manager != null) manager.stop();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return manager == null ? Collections.emptyList() : manager.getActiveEntities(this);
	}

	private static class InvisibilityManager implements Listener {

		private final Object2ObjectArrayMap<UUID, InvisibilityData> entities = new Object2ObjectArrayMap<>();
		private boolean stopped = false;

		public InvisibilityManager() {
			MagicSpells.registerEvents(this);
		}

		public void castBuff(InvisibilitySpell spell, LivingEntity caster, double radius, boolean preventPickups) {
			InvisibilityData data = entities.computeIfAbsent(caster.getUniqueId(), uuid -> {
				Util.forEachPlayerOnline(player -> player.hideEntity(MagicSpells.getInstance(), caster));
				return new InvisibilityData();
			});

			data.spells.put(spell.internalName, preventPickups);
			data.preventPickups |= preventPickups;

			radius = Math.min(radius, MagicSpells.getGlobalRadius());
			for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
				if (!(entity instanceof Mob mob)) continue;

				LivingEntity target = mob.getTarget();
				if (target == null || !target.equals(caster)) continue;

				mob.setTarget(null);
			}
		}

		public boolean isActive(InvisibilitySpell spell, LivingEntity entity) {
			InvisibilityData data = entities.get(entity.getUniqueId());
			return data != null && data.spells.containsKey(spell.internalName);
		}

		public Collection<UUID> getActiveEntities(InvisibilitySpell spell) {
			return entities.entrySet().stream()
				.filter(entry -> entry.getValue().spells.containsKey(spell.internalName))
				.map(Map.Entry::getKey)
				.toList();
		}

		public void turnOffBuff(InvisibilitySpell spell, LivingEntity entity) {
			UUID uuid = entity.getUniqueId();
			InvisibilityData data = entities.get(uuid);

			boolean preventPickups = data.spells.removeBoolean(spell.internalName);
			if (data.spells.isEmpty()) {
				entities.remove(uuid);
				Util.forEachPlayerOnline(player -> player.showEntity(MagicSpells.getInstance(), entity));
				return;
			}

			if (preventPickups && data.preventPickups) {
				data.preventPickups = false;

				var it = data.spells.values().iterator();
				while (it.hasNext()) {
					if (it.nextBoolean()) {
						data.preventPickups = true;
						break;
					}
				}
			}
		}

		public void stop() {
			// Only stop if all other InvisibilitySpell instances with active casts have been turned off.
			if (stopped || !entities.isEmpty()) return;

			stopped = true;
			InvisibilitySpell.manager = null;
			HandlerList.unregisterAll(this);
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void onEntityItemPickup(EntityPickupItemEvent event) {
			InvisibilityData data = entities.get(event.getEntity().getUniqueId());
			if (data == null || !data.preventPickups) return;

			event.setCancelled(true);
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void onEntityTarget(EntityTargetLivingEntityEvent event) {
			LivingEntity target = event.getTarget();
			if (target == null || !entities.containsKey(target.getUniqueId())) return;

			event.setCancelled(true);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onTrack(PlayerTrackEntityEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof LivingEntity caster) || !entities.containsKey(caster.getUniqueId())) return;

			event.setCancelled(true);
			event.getPlayer().hideEntity(MagicSpells.getInstance(), caster);
		}

	}

	private static class InvisibilityData {

		private final Object2BooleanArrayMap<String> spells = new Object2BooleanArrayMap<>();
		private boolean preventPickups;

	}

}
