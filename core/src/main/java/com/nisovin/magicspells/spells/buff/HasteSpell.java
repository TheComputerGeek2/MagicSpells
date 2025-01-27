package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class HasteSpell extends BuffSpell {

	private final Map<UUID, HasteData> players;

	private final ConfigData<Integer> strength;
	private final ConfigData<Integer> boostDuration;
	private final ConfigData<Integer> accelerationDelay;
	private final ConfigData<Integer> accelerationAmount;
	private final ConfigData<Integer> accelerationIncrease;
	private final ConfigData<Integer> accelerationInterval;

	private final ConfigData<Boolean> icon;
	private final ConfigData<Boolean> hidden;
	private final ConfigData<Boolean> ambient;
	private final ConfigData<Boolean> powerAffectsStrength;

	public HasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigDataInt("effect-strength", 3);
		boostDuration = getConfigDataInt("boost-duration", 300);
		accelerationDelay = getConfigDataInt("acceleration-delay", 0);
		accelerationAmount = getConfigDataInt("acceleration-amount", 0);
		accelerationIncrease = getConfigDataInt("acceleration-increase", 0);
		accelerationInterval = getConfigDataInt("acceleration-interval", 0);

		icon = getConfigDataBoolean("icon", true);
		hidden = getConfigDataBoolean("hidden", false);
		ambient = getConfigDataBoolean("ambient", false);
		powerAffectsStrength = getConfigDataBoolean("power-affects-strength", true);

		players = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;
		players.put(target.getUniqueId(), new HasteData(data));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		turnOffBuff(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		HasteData data = players.remove(entity.getUniqueId());
		if (data == null) return;

		MagicSpells.cancelTask(data.task);
		entity.removePotionEffect(PotionEffectType.SPEED);
	}

	@Override
	protected void turnOff() {
		for (UUID id : players.keySet()) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) continue;
			player.removePotionEffect(PotionEffectType.SPEED);
			HasteData data = players.get(id);
			if (data != null) MagicSpells.cancelTask(data.task);
		}

		players.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		HasteData data = players.get(player.getUniqueId());

		if (event.isSprinting()) {
			event.setCancelled(true);
			addUseAndChargeCost(player);
			playSpellEffects(EffectPosition.CASTER, player, data.data);

			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, data.boostDuration, data.strength, data.ambient, !data.hidden, data.icon));

			if (data.acceleration) {
				data.task = MagicSpells.scheduleRepeatingTask(() -> {
					if (data.count >= data.accelerationAmount) {
						MagicSpells.cancelTask(data.task);
						return;
					}
					data.count++;
					player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, data.boostDuration, data.strength + (data.count * data.accelerationIncrease), data.ambient, !data.hidden, data.icon));
				}, data.accelerationDelay, data.accelerationInterval, player);
			}
		} else {
			player.removePotionEffect(PotionEffectType.SPEED);
			playSpellEffects(EffectPosition.DISABLED, player, data.data);
			MagicSpells.cancelTask(data.task);
			data.count = 0;
		}
	}

	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		player.removePotionEffect(PotionEffectType.SPEED);

		HasteData data = players.get(player.getUniqueId());
		if (data == null) return;

		MagicSpells.cancelTask(data.task);
	}

	private class HasteData {

		private final int accelerationIncrease;
		private final int accelerationInterval;
		private final int accelerationAmount;
		private final int accelerationDelay;
		private final int boostDuration;
		private final int strength;

		private final boolean acceleration;
		private final boolean ambient;
		private final boolean hidden;
		private final boolean icon;

		private final SpellData data;

		private int count;
		private ScheduledTask task;

		private HasteData(SpellData data) {
			this.data = data;

			int strength = HasteSpell.this.strength.get(data);
			if (powerAffectsStrength.get(data)) strength = Math.round(strength * data.power());
			this.strength = strength;

			accelerationIncrease = HasteSpell.this.accelerationIncrease.get(data);
			accelerationInterval = HasteSpell.this.accelerationInterval.get(data);
			accelerationAmount = HasteSpell.this.accelerationAmount.get(data);
			accelerationDelay = HasteSpell.this.accelerationDelay.get(data);
			boostDuration = HasteSpell.this.boostDuration.get(data);

			ambient = HasteSpell.this.ambient.get(data);
			hidden = HasteSpell.this.hidden.get(data);
			icon = HasteSpell.this.icon.get(data);

			acceleration = accelerationDelay >= 0 && accelerationAmount > 0 && accelerationIncrease > 0 && accelerationInterval > 0;
		}

	}

}
