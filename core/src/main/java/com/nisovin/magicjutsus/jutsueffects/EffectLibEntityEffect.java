package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.entity.Entity;

public class EffectLibEntityEffect extends EffectLibEffect {
	
	@Override
	protected Runnable playEffectEntity(final Entity e) {
		return manager.start(className, effectLibSection, e);
	}

}
