package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RewindSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<LivingEntity, Rewinder> entities;

	private int tickInterval;
	private int startDuration;
	private int rewindInterval;
	private int specialEffectInterval;
	private int delayedEffectInterval;

	private boolean rewindMana;
	private boolean rewindHealth;
	private boolean allowForceRewind;

	private Subspell rewindSpell;
	private String rewindSpellName;

	public RewindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tickInterval = getConfigInt("tick-interval", 4);
		startDuration = getConfigInt("start-duration", 200);
		rewindInterval = getConfigInt("rewind-interval", 2);
		specialEffectInterval = getConfigInt("special-effect-interval", 5);
		delayedEffectInterval = getConfigInt("delayed-effect-interval", 5);

		rewindMana = getConfigBoolean("rewind-mana", false);
		rewindHealth = getConfigBoolean("rewind-health", true);
		allowForceRewind = getConfigBoolean("allow-force-rewind", true);

		rewindSpellName = getConfigString("spell-on-rewind", "");

		startDuration = (startDuration / tickInterval);

		entities = new ConcurrentHashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		rewindSpell = new Subspell(rewindSpellName);
		if (!rewindSpell.process()) {
			if (!rewindSpellName.isEmpty()) MagicSpells.error("RewindSpell '" + internalName + "' has an invalid spell-on-rewind defined!");
			rewindSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (targetSelf) new Rewinder(caster, caster, power, args);
			else {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
				if (targetInfo == null) return noTarget(caster);
				sendMessages(caster, targetInfo.getTarget(), args);
				new Rewinder(caster, targetInfo.getTarget(), targetInfo.getPower(), args);
			}
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float v, String[] args) {
		new Rewinder(caster, target, v, args);
		sendMessages(caster, target, args);
		playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.TARGET, target);
		playSpellEffectsTrail(caster.getLocation(), target.getLocation());
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float v, String[] args) {
		new Rewinder(null, target, v, args);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellCast(SpellCastEvent e) {
		if (!allowForceRewind) return;
		LivingEntity caster = e.getCaster();
		if (!entities.containsKey(caster)) return;
		if (!e.getSpell().getInternalName().equals(internalName)) return;
		entities.get(caster).rewind();
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		Player pl = e.getPlayer();
		if (!entities.containsKey(pl)) return;
		entities.get(pl).stop();
	}

	private class Rewinder implements Runnable {

		private int taskId;
		private int counter = 0;

		private int startMana;
		private double startHealth;

		private LivingEntity caster;
		private float power;
		private LivingEntity entity;
		private List<Location> locations;

		private Rewinder(LivingEntity caster, LivingEntity entity, float power, String[] args) {
			this.locations = new ArrayList<>();
			this.caster = caster;
			this.power = power;
			this.entity = entity;

			this.startHealth = entity.getHealth();
			if (MagicSpells.isManaSystemEnabled() && entity instanceof Player player) {
				ManaHandler handler = MagicSpells.getManaHandler();
				if (handler != null) this.startMana = handler.getMana(player);
			}

			entities.put(entity, this);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
		}

		@Override
		public void run() {
			// Save locations
			locations.add(entity.getLocation());
			// Loop through already saved locations and play effects with special position
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) locations.forEach(loc -> playSpellEffects(EffectPosition.SPECIAL, loc));
			counter++;
			if (counter >= startDuration) rewind();
		}

		private void rewind() {
			MagicSpells.cancelTask(taskId);
			entities.remove(entity);
			if (rewindSpell != null) rewindSpell.cast(caster, power);
			new ForceRewinder(entity, locations, startHealth, startMana);
		}

		private void stop() {
			MagicSpells.cancelTask(taskId);
			entities.remove(entity);
		}

	}

	private class ForceRewinder implements Runnable {

		private int taskId;
		private int counter;

		private int startMana;
		private double startHealth;
		private LivingEntity entity;

		private Location tempLocation;
		private List<Location> locations;

		private ForceRewinder(LivingEntity entity, List<Location> locations, double startHealth, int startMana) {
			this.locations = locations;
			this.entity = entity;
			this.startMana = startMana;
			this.startHealth = startHealth;
			this.counter = locations.size();
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, rewindInterval);
		}

		@Override
		public void run() {
			// Check if the entity is valid and alive
			if (entity == null || !entity.isValid() || entity.isDead()) {
				cancel();
				return;
			}

			if (locations != null && locations.size() > 0) tempLocation = locations.get(counter - 1);
			if (tempLocation != null) {
				entity.teleport(tempLocation);
				locations.remove(tempLocation);
				if (delayedEffectInterval > 0 && counter % delayedEffectInterval == 0) locations.forEach(loc -> playSpellEffects(EffectPosition.DELAYED, loc));
			}

			counter--;
			if (counter <= 0) stop();
		}

		private void stop() {
			MagicSpells.cancelTask(taskId);
			if (rewindHealth) entity.setHealth(startHealth);
			if (rewindMana && MagicSpells.isManaSystemEnabled() && entity instanceof Player player) {
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
