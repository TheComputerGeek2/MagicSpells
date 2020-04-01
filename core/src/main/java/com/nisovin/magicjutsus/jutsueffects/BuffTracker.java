package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsueffects.JutsuEffect.JutsuEffectActiveChecker;

public class BuffTracker extends EffectTracker implements Runnable {

	BuffTracker(Entity entity, JutsuEffectActiveChecker checker, JutsuEffect effect) {
		super(entity, checker, effect);
	}

	@Override
	public void run() {
		if (!entity.isValid() || !checker.isActive(entity) || effect == null) {
			stop();
			return;
		}

		if (entity instanceof LivingEntity && !effect.getModifiers().check((LivingEntity) entity)) return;

		effect.playEffect(entity);
	}

	@Override
	public void stop() {
		super.stop();
	}

}
