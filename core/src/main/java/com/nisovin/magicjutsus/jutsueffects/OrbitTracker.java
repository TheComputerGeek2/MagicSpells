package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsueffects.JutsuEffect.JutsuEffectActiveChecker;

import de.slikey.effectlib.util.VectorUtils;

public class OrbitTracker extends EffectTracker implements Runnable {

	private Vector currentPosition;

	private int repeatingHorizTaskId;
	private int repeatingVertTaskId;

	private float orbRadius;
	private float orbHeight;

	private float xAxis;
	private float yAxis;
	private float zAxis;

	OrbitTracker(Entity entity, JutsuEffectActiveChecker checker, JutsuEffect effect) {
		super(entity, checker, effect);
		currentPosition = entity.getLocation().getDirection().setY(0);
		Util.rotateVector(currentPosition, effect.getHorizOffset());
		orbRadius = effect.getOrbitRadius();
		orbHeight = effect.getOrbitYOffset();
		if (effect.getHorizExpandDelay() > 0 && effect.getHorizExpandRadius() != 0) {
			repeatingHorizTaskId = MagicJutsus.scheduleRepeatingTask(() -> orbRadius += effect.getHorizExpandRadius(), effect.getHorizExpandDelay(), effect.getHorizExpandDelay());
		}
		if (effect.getVertExpandDelay() > 0 && effect.getVertExpandRadius() != 0) {
			repeatingVertTaskId = MagicJutsus.scheduleRepeatingTask(() -> orbHeight += effect.getVertExpandRadius(), effect.getVertExpandDelay(), effect.getVertExpandDelay());
		}
	}

	@Override
	public void run() {
		if (!entity.isValid() || !checker.isActive(entity) || effect == null) {
			stop();
			return;
		}

		xAxis += effect.getOrbitXAxis();
		yAxis += effect.getOrbitYAxis();
		zAxis += effect.getOrbitZAxis();

		Location loc = getLocation();

		if (entity instanceof LivingEntity && !effect.getModifiers().check((LivingEntity) entity)) return;

		effect.playEffect(loc);
	}

	private Location getLocation() {
		Vector perp;
		if (effect.isCounterClockwise()) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
		else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
		currentPosition.add(perp.multiply(effect.getDistancePerTick())).normalize();
		Vector pos = VectorUtils.rotateVector(currentPosition.clone(), xAxis, yAxis, zAxis);
		return entity.getLocation().clone().add(0, orbHeight, 0).add(pos.multiply(orbRadius));
	}

	@Override
	public void stop() {
		super.stop();
		MagicJutsus.cancelTask(repeatingHorizTaskId);
		MagicJutsus.cancelTask(repeatingVertTaskId);
		currentPosition = null;
	}

}
