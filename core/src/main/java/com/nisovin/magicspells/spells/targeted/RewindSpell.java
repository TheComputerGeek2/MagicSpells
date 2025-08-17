package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedHashMultimap;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RewindSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Multimap<UUID, Rewinder> entities;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> startDuration;
	private final ConfigData<Integer> rewindInterval;
	private final ConfigData<Integer> specialEffectInterval;
	private final ConfigData<Integer> delayedEffectInterval;

	private final ConfigData<Boolean> rewindMana;
	private final ConfigData<Boolean> rewindHealth;
	private final ConfigData<Boolean> allowForceRewind;

	private Subspell rewindSpell;
	private String rewindSpellName;

	public RewindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tickInterval = getConfigDataInt("tick-interval", 4);
		startDuration = getConfigDataInt("start-duration", 200);
		rewindInterval = getConfigDataInt("rewind-interval", 2);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 5);
		delayedEffectInterval = getConfigDataInt("delayed-effect-interval", 5);

		rewindMana = getConfigDataBoolean("rewind-mana", false);
		rewindHealth = getConfigDataBoolean("rewind-health", true);
		allowForceRewind = getConfigDataBoolean("allow-force-rewind", true);

		rewindSpellName = getConfigString("spell-on-rewind", "");

		entities = LinkedHashMultimap.create();
	}

	@Override
	public void initialize() {
		super.initialize();

		rewindSpell = initSubspell(rewindSpellName,
				"RewindSpell '" + internalName + "' has an invalid spell-on-rewind defined!",
				true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		new Rewinder(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		new Rewinder(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler
	public void onSpellCast(SpellCastEvent e) {
		if (e.getSpell() != this) return;

		Collection<Rewinder> rewinders = entities.get(e.getCaster().getUniqueId());
		if (rewinders.isEmpty()) return;

		Iterator<Rewinder> it = rewinders.iterator();
		while (it.hasNext()) {
			Rewinder rewinder = it.next();

			if (rewinder.allowForceRewind) {
				rewinder.rewind(false);
				it.remove();

				e.setCancelled(true);
			}
		}
	}

	private class Rewinder implements Runnable {

		private final SpellData data;

		private final int taskId;
		private int counter = 0;

		private final int startMana;
		private final double startHealth;

		private final List<Location> locations;

		private final boolean rewindMana;
		private final boolean rewindHealth;
		private final boolean allowForceRewind;

		private final int startDuration;
		private final int specialEffectInterval;

		private Rewinder(SpellData data) {
			this.locations = new ArrayList<>();
			this.data = data;

			this.startHealth = data.target().getHealth();
			if (MagicSpells.isManaSystemEnabled() && data.target() instanceof Player player) {
				ManaHandler handler = MagicSpells.getManaHandler();
				if (handler != null) this.startMana = handler.getMana(player);
				else startMana = -1;
			} else startMana = -1;

			rewindMana = RewindSpell.this.rewindMana.get(data);
			rewindHealth = RewindSpell.this.rewindHealth.get(data);
			allowForceRewind = RewindSpell.this.allowForceRewind.get(data);

			int tickInterval = RewindSpell.this.tickInterval.get(data);
			startDuration = RewindSpell.this.startDuration.get(data) / tickInterval;
			specialEffectInterval = RewindSpell.this.specialEffectInterval.get(data);

			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			if (data.hasCaster()) entities.put(data.caster().getUniqueId(), this);

			playSpellEffects(data);
		}

		@Override
		public void run() {
			if (!data.target().isValid()) {
				stop();
				return;
			}

			// Save locations
			locations.add(data.target().getLocation());
			// Loop through already saved locations and play effects with special position
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0)
				locations.forEach(loc -> playSpellEffects(EffectPosition.SPECIAL, loc, data));

			counter++;
			if (counter >= startDuration) rewind(true);
		}

		private void rewind(boolean remove) {
			MagicSpells.cancelTask(taskId);
			if (remove && data.hasCaster()) entities.remove(data.caster().getUniqueId(), this);
			if (rewindSpell != null) rewindSpell.subcast(data.noTarget());
			new ForceRewinder(data, locations, startHealth, startMana, rewindHealth, rewindMana);
		}

		private void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

	private class ForceRewinder implements Runnable {

		private final SpellData data;

		private final int taskId;
		private int counter;

		private final int startMana;
		private final double startHealth;

		private final boolean rewindMana;
		private final boolean rewindHealth;

		private Location tempLocation;
		private List<Location> locations;

		private final int delayedEffectInterval;

		private ForceRewinder(SpellData data, List<Location> locations, double startHealth, int startMana, boolean rewindHealth, boolean rewindMana) {
			this.data = data;
			this.locations = locations;
			this.startMana = startMana;
			this.startHealth = startHealth;
			this.rewindMana = rewindMana;
			this.rewindHealth = rewindHealth;

			this.counter = locations.size();

			delayedEffectInterval = RewindSpell.this.delayedEffectInterval.get(data);

			int rewindInterval = RewindSpell.this.rewindInterval.get(data);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, rewindInterval);
		}

		@Override
		public void run() {
			// Check if the entity is valid and alive
			if (!data.target().isValid()) {
				cancel();
				return;
			}

			if (locations != null && !locations.isEmpty()) tempLocation = locations.get(counter - 1);
			if (tempLocation != null) {
				data.target().teleportAsync(tempLocation, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
				locations.remove(tempLocation);
				if (delayedEffectInterval > 0 && counter % delayedEffectInterval == 0)
					locations.forEach(loc -> playSpellEffects(EffectPosition.DELAYED, loc, data));
			}

			counter--;
			if (counter <= 0) stop();
		}

		private void stop() {
			MagicSpells.cancelTask(taskId);
			if (rewindHealth) data.target().setHealth(startHealth);
			if (rewindMana && MagicSpells.isManaSystemEnabled() && startMana > -1 && data.target() instanceof Player player) {
				ManaHandler handler = MagicSpells.getManaHandler();
				if (handler != null) handler.setMana(player, startMana, ManaChangeReason.OTHER);
			}
		}

		private void cancel() {
			MagicSpells.cancelTask(taskId);
			locations.clear();
			locations = null;
		}

	}

}
