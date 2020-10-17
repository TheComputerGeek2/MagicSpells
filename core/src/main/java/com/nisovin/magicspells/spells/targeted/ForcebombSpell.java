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
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(livingEntity, power);
			if (block != null && !BlockUtils.isAir(block.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null || BlockUtils.isAir(block.getType())) return noTarget(livingEntity);
			knockback(livingEntity, block.getLocation().add(0.5, 0, 0.5), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		knockback(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		knockback(null, target, power);
		return true;
	}
	
	private void knockback(LivingEntity livingEntity, Location location, float basePower) {
		if (location == null) return;
		if (location.getWorld() == null) return;

		location = location.clone().add(0D, yOffset, 0D);
		Collection<LivingEntity> entities = location.getWorld().getLivingEntities();

		Vector e;
		Vector v;
		Vector t = location.toVector();
		for (LivingEntity entity : entities) {
			if (livingEntity == null && !validTargetList.canTarget(entity)) continue;
			if (livingEntity != null && !validTargetList.canTarget(livingEntity, entity)) continue;
			if (!entity.getLocation().getWorld().equals(location.getWorld())) continue;
			if (entity.getLocation().distanceSquared(location) > radiusSquared) continue;

			float power = basePower;
			if (callTargetEvents && livingEntity != null) {
				SpellTargetEvent event = new SpellTargetEvent(this, livingEntity, entity, power);
				EventUtil.call(event);
				if (event.isCancelled()) continue;
				power = event.getPower();
			}

			e = entity.getLocation().toVector();
			v = e.subtract(t).normalize().multiply(force * power);

			if (force != 0) v.setY(v.getY() * (yForce * power));
			else v.setY(yForce * power);
			if (v.getY() > maxYForce) v.setY(maxYForce);

			v = Util.makeFinite(v);

			if (addVelocityInstead) entity.setVelocity(entity.getVelocity().add(v));
			else entity.setVelocity(v);

			if (livingEntity != null) playSpellEffectsTrail(livingEntity.getLocation(), entity.getLocation());
			playSpellEffects(EffectPosition.TARGET, entity);
		}

		playSpellEffects(EffectPosition.SPECIAL, location);
		if (livingEntity != null) playSpellEffects(EffectPosition.CASTER, livingEntity);
	}

}
