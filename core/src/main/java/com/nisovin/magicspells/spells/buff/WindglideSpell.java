package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

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

	private final Map<UUID, SpellData> entities;

	private Subspell glideSpell;
	private Subspell collisionSpell;

	private final String glideSpellName;
	private final String collisionSpellName;

	private boolean cancelOnCollision;
	private boolean blockCollisionDmg;

	private int interval;

	private ConfigData<Double> height;
	private ConfigData<Double> velocity;

	private final GlideMonitor monitor;

	public WindglideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		glideSpellName = getConfigString("spell", "");
		collisionSpellName = getConfigString("collision-spell", "");

		blockCollisionDmg = getConfigBoolean("block-collision-dmg", true);
		cancelOnCollision = getConfigBoolean("cancel-on-collision", false);

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

		glideSpell = new Subspell(glideSpellName);
		if (!glideSpell.process() || !glideSpell.isTargetedLocationSpell()) {
			glideSpell = null;
			if (!glideSpellName.isEmpty())
				MagicSpells.error("WindglideSpell " + internalName + " has an invalid spell defined: " + glideSpellName);
		}

		collisionSpell = new Subspell(collisionSpellName);
		if (!collisionSpell.process() || !collisionSpell.isTargetedLocationSpell()) {
			collisionSpell = null;
			if (!collisionSpellName.isEmpty())
				MagicSpells.error("WindglideSpell " + internalName + " has an invalid collision-spell defined: " + collisionSpellName);
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		entity.setGliding(true);
		return true;
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
			if (entity == null) continue;
			if (!(entity instanceof LivingEntity)) continue;
			if (!entity.isValid()) continue;

			((LivingEntity) entity).setGliding(false);
			turnOffBuff((LivingEntity) entity);
		}

		entities.clear();
	}

	@EventHandler
	public void onEntityGlide(EntityToggleGlideEvent e) {
		Entity entity = e.getEntity();
		if (!(entity instanceof LivingEntity livingEntity)) return;
		if (!isActive(livingEntity)) return;
		if (livingEntity.isGliding()) e.setCancelled(true);
	}

	@EventHandler
	public void onEntityCollision(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;
		if (!(e.getEntity() instanceof LivingEntity livingEntity)) return;
		if (!isActive(livingEntity)) return;
		if (blockCollisionDmg) e.setCancelled(true);
		if (cancelOnCollision) turnOff(livingEntity);
		if (collisionSpell != null) collisionSpell.castAtLocation(livingEntity, livingEntity.getLocation(), 1F);
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

	public boolean shouldCancelOnCollision() {
		return cancelOnCollision;
	}

	public void setCancelOnCollision(boolean cancelOnCollision) {
		this.cancelOnCollision = cancelOnCollision;
	}

	public boolean shouldBlockCollisionDamage() {
		return blockCollisionDmg;
	}

	public void setBlockCollisionDmg(boolean blockCollisionDmg) {
		this.blockCollisionDmg = blockCollisionDmg;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	private class GlideMonitor implements Runnable {

		private final int taskId;

		private GlideMonitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			for (UUID id : entities.keySet()) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null || !entity.isValid()) continue;
				if (!(entity instanceof LivingEntity caster)) continue;

				SpellData data = entities.get(id);

				double velocity = WindglideSpell.this.velocity.get(caster, null, data.power(), data.args()) / 10;
				double height = WindglideSpell.this.height.get(caster, null, data.power(), data.args());

				Location eLoc = entity.getLocation();
				Vector v = eLoc.getDirection().normalize().multiply(velocity).add(new Vector(0, height, 0));
				entity.setVelocity(v);

				if (glideSpell != null) glideSpell.castAtLocation(caster, eLoc, 1F);
				playSpellEffects(EffectPosition.SPECIAL, eLoc);
				addUseAndChargeCost(caster);
			}
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

}
