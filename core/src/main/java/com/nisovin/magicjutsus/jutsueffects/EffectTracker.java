package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.entity.Entity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.jutsueffects.JutsuEffect.JutsuEffectActiveChecker;

public class EffectTracker implements Runnable {

	Entity entity;
	BuffJutsu buffJutsu;
	JutsuEffect effect;
	JutsuEffectActiveChecker checker;

	int effectTrackerTaskId;

	EffectTracker(Entity entity, JutsuEffectActiveChecker checker, JutsuEffect effect) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;

		effectTrackerTaskId = MagicJutsus.scheduleRepeatingTask(this, 0, effect.getEffectInterval());
	}

	public Entity getEntity() {
		return entity;
	}

	public BuffJutsu getBuffJutsu() {
		return buffJutsu;
	}

	public JutsuEffect getEffect() {
		return effect;
	}

	public JutsuEffectActiveChecker getChecker() {
		return checker;
	}

	public int getEffectTrackerTaskId() {
		return effectTrackerTaskId;
	}

	public void setBuffJutsu(BuffJutsu jutsu) {
		buffJutsu = jutsu;
	}

	@Override
	public void run() {

	}

	public void stop() {
		MagicJutsus.cancelTask(effectTrackerTaskId);
		entity = null;
	}

	public void unregister() {
		if (buffJutsu != null) buffJutsu.getEffectTrackers().remove(this);
	}

}
