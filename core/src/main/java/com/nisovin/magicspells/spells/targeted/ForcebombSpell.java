package com.nisovin.magicspells.spells.targeted;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class ForcebombSpell extends TargetedSpell implements TargetedLocationSpell {

	private float force;
	private float yForce;
	private float yOffset;
	private float maxYForce;

	private double radiusSquared;

	private boolean callTargetEvents;
	private boolean addVelocityInstead;
	
	public ForcebombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		force = getConfigFloat("pushback-force", 30) / 10.0F;
		yForce = getConfigFloat("additional-vertical-force", 15) / 10.0F;
		yOffset = getConfigFloat("y-offset", 0F);
		maxYForce = getConfigFloat("max-vertical-force", 20) / 10.0F;

		radiusSquared = getConfigDouble("radius", 3);
		if (radiusSquared > MagicSpells.getGlobalRadius()) radiusSquared = MagicSpells.getGlobalRadius();
		radiusSquared *= radiusSquared;

		callTargetEvents = getConfigBoolean("call-target-events", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(caster, power);
			if (block != null && !BlockUtils.isAir(block.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null || BlockUtils.isAir(block.getType())) return noTarget(caster);
			knockback(caster, block.getLocation().add(0.5, 0, 0.5), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		knockback(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		knockback(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		knockback(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		knockback(null, target, power, null);
		return true;
	}

	private void knockback(LivingEntity caster, Location location, float basePower, String[] args) {
		if (location == null) return;
		if (location.getWorld() == null) return;

		location = location.clone().add(0D, yOffset, 0D);

		if (validTargetList.canTargetOnlyCaster()) {
			if (caster == null) return;
			bomb(caster, caster, location, basePower, args);

			playSpellEffects(EffectPosition.TARGET, caster);
			playSpellEffects(EffectPosition.CASTER, caster);
			playSpellEffects(EffectPosition.SPECIAL, location);
			return;
		}

		Collection<LivingEntity> entities = location.getWorld().getLivingEntities();
		for (LivingEntity entity : entities) {
			if (caster == null && !validTargetList.canTarget(entity)) continue;
			if (caster != null && !validTargetList.canTarget(caster, entity)) continue;

			bomb(caster, entity, location, basePower, args);
			if (caster != null) playSpellEffectsTrail(caster.getLocation(), entity.getLocation());
			playSpellEffects(EffectPosition.TARGET, entity);
		}

		playSpellEffects(EffectPosition.SPECIAL, location);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
	}

	private void bomb(LivingEntity caster, LivingEntity target, Location location, float basePower, String[] args) {
		if (!target.getLocation().getWorld().equals(location.getWorld())) return;
		if (target.getLocation().distanceSquared(location) > radiusSquared) return;

		float power = basePower;
		if (callTargetEvents && caster != null) {
			SpellTargetEvent event = new SpellTargetEvent(this, caster, target, power);
			EventUtil.call(event);
			if (event.isCancelled()) return;
			power = event.getPower();
		}

		Vector v = target.getLocation().toVector().subtract(location.toVector()).normalize().multiply(force * power);

		if (force != 0) v.setY(v.getY() * (yForce * power));
		else v.setY(yForce * power);
		if (v.getY() > maxYForce) v.setY(maxYForce);

		v = Util.makeFinite(v);

		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);
	}

}
