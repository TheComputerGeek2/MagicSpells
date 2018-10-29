package com.nisovin.magicspells.util;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

public class EffectPackage {
	
	public ParticleEffect effect;
	public ParticleData data;

	/**
	 * Whether or not the particle effect should be rendered
	 */
	public boolean render;

	/**
	 * An empty effect package with render set to false.
	 */
	public EffectPackage() {
		this.render = false;
	}

	public EffectPackage(ParticleEffect effect, ParticleData data) {
		this.effect = effect;
		this.data = data;
		this.render = true;
	}

	public boolean canRender() {
		return this.render;
	}
	
}