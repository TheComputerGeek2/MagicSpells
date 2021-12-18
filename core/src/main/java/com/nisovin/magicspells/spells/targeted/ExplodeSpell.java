package com.nisovin.magicspells.spells.targeted;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class ExplodeSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private ConfigData<Integer> explosionSize;
	private ConfigData<Integer> backfireChance;

	private ConfigData<Float> damageMultiplier;

	private boolean addFire;
	private boolean simulateTnt;
	private boolean ignoreCanceled;
	private boolean preventBlockDamage;
	private boolean preventPlayerDamage;
	private boolean preventAnimalDamage;
	private boolean powerAffectsDamageMultiplier;

	private long currentTick = 0;
	private String[] currentArgs;
	private float currentPower = 0;
	private LivingEntity currentCaster;

	public ExplodeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		explosionSize = getConfigDataInt("explosion-size", 4);
		backfireChance = getConfigDataInt("backfire-chance", 0);

		damageMultiplier = getConfigDataFloat("damage-multiplier", 0);

		addFire = getConfigBoolean("add-fire", false);
		simulateTnt = getConfigBoolean("simulate-tnt", true);
		ignoreCanceled = getConfigBoolean("ignore-cancelled", false);
		preventBlockDamage = getConfigBoolean("prevent-block-damage", false);
		preventPlayerDamage = getConfigBoolean("prevent-player-damage", false);
		preventAnimalDamage = getConfigBoolean("prevent-animal-damage", false);
		powerAffectsDamageMultiplier = getConfigBoolean("power-affects-damage-multiplier", true);
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target;
			try {
				target = getTargetedBlock(caster, power);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				target = null;
			}

			if (target != null && !BlockUtils.isAir(target.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) target = null;
				else {
					target = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (target == null || BlockUtils.isAir(target.getType())) return noTarget(caster);
			boolean exploded = explode(caster, target.getLocation(), power, args);
			if (!exploded && !ignoreCanceled) return noTarget(caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean explode(LivingEntity livingEntity, Location target, float power, String[] args) {
		int explosionSize = this.explosionSize.get(livingEntity, null, power, args);
		if (simulateTnt) {
			boolean cancelled = MagicSpells.getVolatileCodeHandler().simulateTnt(target, livingEntity, explosionSize * power, addFire);
			if (cancelled) return false;
		}

		int backfireChance = this.backfireChance.get(livingEntity, null, power, args);
		if (backfireChance > 0) {
			Random rand = ThreadLocalRandom.current();
			if (rand.nextInt(10000) < backfireChance) target = livingEntity.getLocation();
		}

		currentTick = Bukkit.getWorlds().get(0).getFullTime();
		currentCaster = livingEntity;
		currentPower = power;
		currentArgs = args;

		boolean ret = target.getWorld().createExplosion(target, explosionSize * power, addFire, !preventBlockDamage, livingEntity);
		if (ret) playSpellEffects(livingEntity, target);

		return ret;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return explode(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return explode(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION)) return;
		if (currentTick != Bukkit.getWorlds().get(0).getFullTime()) return;

		float damageMultiplier = this.damageMultiplier.get(currentCaster, event.getEntity() instanceof LivingEntity le ? le : null, currentPower, currentArgs);
		if (!(damageMultiplier > 0 || preventPlayerDamage || preventAnimalDamage)) return;

		if (preventPlayerDamage && event.getEntity() instanceof Player) event.setCancelled(true);
		else if (preventAnimalDamage && event.getEntity() instanceof Animals) event.setCancelled(true);
		else if (damageMultiplier > 0) {
			if (powerAffectsDamageMultiplier) damageMultiplier *= currentPower;
			event.setDamage(damageMultiplier);
		}
	}
	
	@EventHandler
	public void onExplode(EntityExplodeEvent event) {
		if (event.isCancelled() || !preventBlockDamage) return;
		if (currentTick == Bukkit.getWorlds().get(0).getFullTime()) {
			event.blockList().clear();
			event.setYield(0);
		}
	}
	
}
