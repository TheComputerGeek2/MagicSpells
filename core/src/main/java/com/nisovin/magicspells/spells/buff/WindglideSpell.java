package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class WindglideSpell extends BuffSpell {

	private final Map<UUID, GlideData> entities;

	private Subspell glideSpell;
	private Subspell collisionSpell;

	private final String glideSpellName;
	private final String collisionSpellName;

	private final ConfigData<Boolean> constantHeight;
	private final ConfigData<Boolean> constantVelocity;
	private final ConfigData<Boolean> cancelOnCollision;
	private final ConfigData<Boolean> blockCollisionDmg;

	private int interval;

	private final ConfigData<Double> height;
	private final ConfigData<Double> velocity;

	private final GlideMonitor monitor;

	public WindglideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		glideSpellName = getConfigString("spell", "");
		collisionSpellName = getConfigString("collision-spell", "");

		constantHeight = getConfigDataBoolean("constant-height", true);
		constantVelocity = getConfigDataBoolean("constant-velocity", true);
		blockCollisionDmg = getConfigDataBoolean("block-collision-dmg", true);
		cancelOnCollision = getConfigDataBoolean("cancel-on-collision", false);

		height = getConfigDataDouble("height", 0F);
		velocity = getConfigDataDouble("velocity", 20F);

		interval = getConfigInt("interval", 4);
		if (interval <= 0) interval = 4;

		entities = new HashMap<>();

		monitor = new GlideMonitor();
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "WindglideSpell " + internalName + " has an invalid '%s' defined!";
		glideSpell = initSubspell(glideSpellName,
				error.formatted("spell"),
				true);
		collisionSpell = initSubspell(collisionSpellName,
				error.formatted("collision-spell"),
				true);
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantHeight = this.constantHeight.get(data);
		boolean constantVelocity = this.constantVelocity.get(data);

		double height = constantHeight ? this.height.get(data) : 0;
		double velocity = constantVelocity ? this.velocity.get(data) / 10 : 0;

		entities.put(data.target().getUniqueId(), new GlideData(
			data,
			velocity,
			height,
			constantVelocity,
			constantHeight,
			blockCollisionDmg.get(data),
			cancelOnCollision.get(data)
		));

		data.target().setGliding(true);

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		entity.setGliding(false);
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos : EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}

		for (UUID id : entities.keySet()) {
			Entity entity = Bukkit.getEntity(id);
			if (!(entity instanceof LivingEntity livingEntity)) continue;
			if (!entity.isValid()) continue;

			livingEntity.setGliding(false);
			turnOff(livingEntity);
		}

		entities.clear();
		monitor.stop();
	}

	@EventHandler
	public void onEntityGlide(EntityToggleGlideEvent e) {
		if (e.getEntity() instanceof LivingEntity entity && isActive(entity) && !e.isGliding())
			e.setCancelled(true);
	}

	@EventHandler
	public void onEntityCollision(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;
		if (!(e.getEntity() instanceof LivingEntity entity)) return;

		GlideData data = entities.get(entity.getUniqueId());
		if (data == null) return;

		if (data.blockCollisionDmg) e.setCancelled(true);
		if (data.cancelOnCollision) turnOff(entity);

		if (collisionSpell != null) {
			SpellData castData = data.spellData.builder().caster(entity).target(null).location(entity.getLocation()).build();
			collisionSpell.subcast(castData);
		}
	}

	public Subspell getGlideSpell() {
		return glideSpell;
	}

	public void setGlideSpell(Subspell glideSpell) {
		this.glideSpell = glideSpell;
	}

	public Subspell getCollisionSpell() {
		return collisionSpell;
	}

	public void setCollisionSpell(Subspell collisionSpell) {
		this.collisionSpell = collisionSpell;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	private class GlideMonitor implements Runnable {

		private final ScheduledTask task;

		private GlideMonitor() {
			task = MagicSpells.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			for (UUID id : entities.keySet()) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null || !entity.isValid()) continue;
				if (!(entity instanceof LivingEntity caster)) continue;

				GlideData data = entities.get(id);

				Location location = entity.getLocation();
				SpellData subData = data.spellData.location(location);

				double height = data.constantHeight ? data.height : WindglideSpell.this.height.get(subData);
				double velocity = data.constantVelocity ? data.velocity : WindglideSpell.this.velocity.get(subData) / 10;

				Vector v = location.getDirection().multiply(velocity);
				v.setY(v.getY() + height);
				entity.setVelocity(v);

				if (glideSpell != null) {
					SpellData castData = subData.builder().caster(caster).target(null).recipient(null).build();
					glideSpell.subcast(castData);
				}

				playSpellEffects(EffectPosition.SPECIAL, location, subData);

				addUseAndChargeCost(caster);
			}
		}

		public void stop() {
			MagicSpells.cancelTask(task);
		}

	}

	public record GlideData(SpellData spellData, double velocity, double height, boolean constantVelocity,
							boolean constantHeight, boolean blockCollisionDmg, boolean cancelOnCollision) {
	}

}
