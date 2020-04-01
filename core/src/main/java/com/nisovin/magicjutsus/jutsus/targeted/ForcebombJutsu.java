package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class ForcebombJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private float force;
	private float yForce;
	private float yOffset;
	private float maxYForce;

	private double radiusSquared;

	private boolean callTargetEvents;
	private boolean addVelocityInstead;
	
	public ForcebombJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		force = getConfigFloat("pushback-force", 30) / 10.0F;
		yForce = getConfigFloat("additional-vertical-force", 15) / 10.0F;
		yOffset = getConfigFloat("y-offset", 0F);
		maxYForce = getConfigFloat("max-vertical-force", 20) / 10.0F;

		radiusSquared = getConfigDouble("radius", 3);
		radiusSquared *= radiusSquared;

		callTargetEvents = getConfigBoolean("call-target-events", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Block block = getTargetedBlock(livingEntity, power);
			if (block != null && !BlockUtils.isAir(block.getType())) {
				JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, block.getLocation(), power);
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
		Collection<Entity> entities = location.getWorld().getEntitiesByClasses(LivingEntity.class);

		Vector e;
		Vector v;
		Vector t = location.toVector();
		for (Entity entity : entities) {
			if (livingEntity == null && !validTargetList.canTarget(entity)) continue;
			if (livingEntity != null && !validTargetList.canTarget(livingEntity, entity)) continue;
			if (!entity.getLocation().getWorld().equals(location.getWorld())) continue;
			if (entity.getLocation().distanceSquared(location) > radiusSquared) continue;

			float power = basePower;
			if (callTargetEvents && livingEntity != null) {
				JutsuTargetEvent event = new JutsuTargetEvent(this, livingEntity, (LivingEntity) entity, power);
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

			if (livingEntity != null) playJutsuEffectsTrail(livingEntity.getLocation(), entity.getLocation());
			playJutsuEffects(EffectPosition.TARGET, entity);
		}

		playJutsuEffects(EffectPosition.SPECIAL, location);
		if (livingEntity != null) playJutsuEffects(EffectPosition.CASTER, livingEntity);
	}

}
