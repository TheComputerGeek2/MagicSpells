package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;

public class EffectLibEntityEffect extends EffectLibEffect {
	
	@Override
	protected Runnable playEffectEntity(final Entity e) {
		updateManager();
		return manager.start(className, effectLibSection, e);
	}

}
