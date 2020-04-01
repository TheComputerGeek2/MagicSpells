package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;

import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class ForcepushJutsu extends InstantJutsu {

	private float force;
	private float radius;
	private float yForce;
	private float maxYForce;

	private boolean addVelocityInstead;
	
	public ForcepushJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		force = getConfigFloat("pushback-force", 30) / 10F;
		radius = getConfigFloat("radius", 3F);
		yForce = getConfigFloat("additional-vertical-force", 15) / 10F;
		maxYForce = getConfigFloat("max-vertical-force", 20) / 10F;

		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			knockback(livingEntity, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void knockback(LivingEntity livingEntity, float basePower) {
		List<Entity> entities = livingEntity.getNearbyEntities(radius, radius, radius);
		Vector e;
		Vector v;
		Vector p = livingEntity.getLocation().toVector();
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!validTargetList.canTarget(livingEntity, entity)) continue;

			LivingEntity target = (LivingEntity) entity;
			float power = basePower;
			JutsuTargetEvent event = new JutsuTargetEvent(this, livingEntity, target, power);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			target = event.getTarget();
			power = event.getPower();
			
			e = target.getLocation().toVector();
			v = e.subtract(p).normalize().multiply(force * power);

			if (force != 0) v.setY(v.getY() + (yForce * power));
			else v.setY(yForce * power);
			if (v.getY() > (maxYForce)) v.setY(maxYForce);

			v = Util.makeFinite(v);

			if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
			else target.setVelocity(v);

			playJutsuEffects(EffectPosition.TARGET, target);
			playJutsuEffectsTrail(livingEntity.getLocation(), target.getLocation());
		}
		playJutsuEffects(EffectPosition.CASTER, livingEntity);
	}

}
