package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

import org.apache.commons.math3.util.FastMath;

public class HasteSpell extends BuffSpell {

	private final Map<UUID, HasteData> entities;

	private ConfigData<Integer> strength;
	private ConfigData<Integer> boostDuration;
	private ConfigData<Integer> accelerationDelay;
	private ConfigData<Integer> accelerationAmount;
	private ConfigData<Integer> accelerationIncrease;
	private ConfigData<Integer> accelerationInterval;

	private boolean hidden;
	private boolean powerAffectsStrength;

	public HasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigDataInt("effect-strength", 3);
		boostDuration = getConfigDataInt("boost-duration", 300);
		accelerationDelay = getConfigDataInt("acceleration-delay", 0);
		accelerationAmount = getConfigDataInt("acceleration-amount", 0);
		accelerationIncrease = getConfigDataInt("acceleration-increase", 0);
		accelerationInterval = getConfigDataInt("acceleration-interval", 0);

		hidden = getConfigBoolean("hidden", false);
		powerAffectsStrength = getConfigBoolean("power-affects-strength", true);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new HasteData(entity, power, args));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		HasteData data = entities.get(entity.getUniqueId());
		if (data == null) return;
		MagicSpells.cancelTask(data.task);
		entities.remove(entity.getUniqueId());
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0));
		entity.removePotionEffect(PotionEffectType.SPEED);
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities.keySet()) {
			Entity e = Bukkit.getEntity(id);
			if (!(e instanceof LivingEntity livingEntity)) continue;
			livingEntity.removePotionEffect(PotionEffectType.SPEED);
			HasteData data = entities.get(id);
			if (data != null) MagicSpells.cancelTask(data.task);
		}

		entities.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		Player pl = event.getPlayer();
		if (!isActive(pl)) return;

		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		HasteData data = entities.get(pl.getUniqueId());
		int amplifier = data.strength;

		if (event.isSprinting()) {
			event.setCancelled(true);
			addUseAndChargeCost(pl);
			playSpellEffects(EffectPosition.CASTER, pl);

			int boostDuration = this.boostDuration.get(pl, null, data.power, data.args);
			pl.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, boostDuration, amplifier, false, !hidden));

			if (data.acceleration) {
				data.task = MagicSpells.scheduleRepeatingTask(() -> {
					if (data.count >= data.accelerationAmount) {
						MagicSpells.cancelTask(data.task);
						return;
					}
					data.count++;
					pl.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, boostDuration, amplifier + (data.count * data.accelerationIncrease), false, !hidden));
				}, data.accelerationDelay, data.accelerationInterval);
			}
		} else {
			pl.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0, false, !hidden));
			pl.removePotionEffect(PotionEffectType.SPEED);
			playSpellEffects(EffectPosition.DISABLED, pl);
			MagicSpells.cancelTask(data.task);
			data.count = 0;
		}
	}

	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player pl = event.getPlayer();
		if (!isActive(pl)) return;

		pl.removePotionEffect(PotionEffectType.SPEED);

		HasteData data = entities.get(pl.getUniqueId());
		if (data == null) return;

		MagicSpells.cancelTask(data.task);
	}

	public Map<UUID, HasteData> getEntities() {
		return entities;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	private class HasteData {

		private final String[] args;
		private final int strength;
		private final float power;

		private final int accelerationIncrease;
		private final int accelerationInterval;
		private final int accelerationAmount;
		private final int accelerationDelay;
		private final int boostDuration;

		private final boolean acceleration;

		private int count;
		private int task;

		private HasteData(LivingEntity entity, float power, String[] args) {
			this.power = power;
			this.args = args;

			int strength = HasteSpell.this.strength.get(entity, null, power, args);
			if (powerAffectsStrength) strength = FastMath.round(strength * power);
			this.strength = strength;

			accelerationIncrease = HasteSpell.this.accelerationIncrease.get(entity, null, power, args);
			accelerationInterval = HasteSpell.this.accelerationInterval.get(entity, null, power, args);
			accelerationAmount = HasteSpell.this.accelerationAmount.get(entity, null, power, args);
			accelerationDelay = HasteSpell.this.accelerationDelay.get(entity, null, power, args);
			boostDuration = HasteSpell.this.boostDuration.get(entity, null, power, args);

			acceleration = accelerationDelay >= 0 && accelerationAmount > 0 && accelerationIncrease > 0 && accelerationInterval > 0;
		}

	}

}
