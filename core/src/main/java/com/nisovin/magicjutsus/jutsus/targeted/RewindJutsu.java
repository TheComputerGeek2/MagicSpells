package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class RewindJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<LivingEntity, Rewinder> entities;

	private int tickInterval;
	private int startDuration;
	private int rewindInterval;
	private int specialEffectInterval;
	private int delayedEffectInterval;

	private boolean rewindMana;
	private boolean rewindHealth;
	private boolean allowForceRewind;

	private Ninjutsu rewindJutsu;
	private String rewindJutsuName;

	public RewindJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		tickInterval = getConfigInt("tick-interval", 4);
		startDuration = getConfigInt("start-duration", 200);
		rewindInterval = getConfigInt("rewind-interval", 2);
		specialEffectInterval = getConfigInt("special-effect-interval", 5);
		delayedEffectInterval = getConfigInt("delayed-effect-interval", 5);

		rewindMana = getConfigBoolean("rewind-mana", false);
		rewindHealth = getConfigBoolean("rewind-health", true);
		allowForceRewind = getConfigBoolean("allow-force-rewind", true);

		rewindJutsuName = getConfigString("jutsu-on-rewind", "");

		startDuration = (startDuration / tickInterval);

		entities = new ConcurrentHashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		rewindJutsu = new Ninjutsu(rewindJutsuName);
		if (!rewindJutsu.process()) {
			if (!rewindJutsuName.isEmpty()) MagicJutsus.error("RewindJutsu '" + internalName + "' has an invalid jutsu-on-rewind defined!");
			rewindJutsu = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			if (targetSelf) new Rewinder(livingEntity, livingEntity, power);
			else {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo == null) return noTarget(livingEntity);
				sendMessages(livingEntity, targetInfo.getTarget());
				new Rewinder(livingEntity, targetInfo.getTarget(), power);
			}
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity player, LivingEntity livingEntity, float v) {
		new Rewinder(player, livingEntity, v);
		sendMessages(player, livingEntity);
		playJutsuEffects(EffectPosition.CASTER, player);
		playJutsuEffects(EffectPosition.TARGET, livingEntity);
		playJutsuEffectsTrail(player.getLocation(), livingEntity.getLocation());
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity livingEntity, float v) {
		new Rewinder(null, livingEntity, v);
		playJutsuEffects(EffectPosition.TARGET, livingEntity);
		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onJutsuCast(JutsuCastEvent e) {
		if (!allowForceRewind) return;
		LivingEntity caster = e.getCaster();
		if (!entities.containsKey(caster)) return;
		if (!e.getJutsu().getInternalName().equals(internalName)) return;
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

		private Rewinder(LivingEntity caster, LivingEntity entity, float power) {
			this.locations = new ArrayList<>();
			this.caster = caster;
			this.power = power;
			this.entity = entity;
			if (entity instanceof Player) this.startMana = MagicJutsus.getManaHandler().getMana((Player) entity);
			this.startHealth = entity.getHealth();

			entities.put(entity, this);
			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);
		}

		@Override
		public void run() {
			// Save locations
			locations.add(entity.getLocation());
			// Loop through already saved locations and play effects with special position
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) locations.forEach(loc -> playJutsuEffects(EffectPosition.SPECIAL, loc));
			counter++;
			if (counter >= startDuration) rewind();
		}

		private void rewind() {
			MagicJutsus.cancelTask(taskId);
			entities.remove(entity);
			if (rewindJutsu != null) rewindJutsu.cast(caster, power);
			new ForceRewinder(entity, locations, startHealth, startMana);
		}

		private void stop() {
			MagicJutsus.cancelTask(taskId);
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
			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 0, rewindInterval);
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
				if (delayedEffectInterval > 0 && counter % delayedEffectInterval == 0) locations.forEach(loc -> playJutsuEffects(EffectPosition.DELAYED, loc));
			}

			counter--;
			if (counter <= 0) stop();
		}

		private void stop() {
			MagicJutsus.cancelTask(taskId);
			if (rewindHealth) entity.setHealth(startHealth);
			if (entity instanceof Player && rewindMana) MagicJutsus.getManaHandler().setMana((Player) entity, startMana, ManaChangeReason.OTHER);
		}

		private void cancel() {
			MagicJutsus.cancelTask(taskId);
			locations.clear();
			locations = null;
		}

	}

}
