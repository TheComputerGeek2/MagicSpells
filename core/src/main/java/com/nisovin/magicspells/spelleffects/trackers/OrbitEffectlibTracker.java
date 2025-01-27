package com.nisovin.magicspells.spelleffects.trackers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.util.VectorUtils;
import de.slikey.effectlib.effect.ModifiedEffect;

import java.util.concurrent.TimeUnit;

public class OrbitEffectlibTracker extends AsyncEffectTracker implements Runnable {

	private Vector currentPosition;

	private ScheduledTask repeatingHorizTask;
	private ScheduledTask repeatingVertTask;

	private float orbRadius;
	private float orbHeight;

	private float xAxis;
	private float yAxis;
	private float zAxis;

	private final float orbitXAxis;
	private final float orbitYAxis;
	private final float orbitZAxis;
	private final float distancePerTick;

	private final boolean counterClockwise;

	private final Effect effectlibEffect;

	private Location loc;

	private Effect modifiedEffect;

	public OrbitEffectlibTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		super(entity, checker, effect, data);

		currentPosition = entity.getLocation().getDirection().setY(0);
		Util.rotateVector(currentPosition, effect.getHorizOffset().get(data));

		orbRadius = effect.getOrbitRadius().get(data);
		orbHeight = effect.getOrbitYOffset().get(data);

		orbitXAxis = effect.getOrbitXAxis().get(data);
		orbitYAxis = effect.getOrbitYAxis().get(data);
		orbitZAxis = effect.getOrbitZAxis().get(data);
		distancePerTick = 6.28f * effect.getEffectInterval().get(data) / effect.getSecondsPerRevolution().get(data) / 20f;

		counterClockwise = effect.isCounterClockwise().get(data);

		float horizRadius = effect.getHorizExpandRadius().get(data);
		int horizDelay = effect.getHorizExpandDelay().get(data);
		if (horizDelay > 0 && horizRadius != 0)
			repeatingHorizTask = Bukkit.getAsyncScheduler().runAtFixedRate(MagicSpells.getInstance(),
				t -> orbRadius += horizRadius, horizDelay * 50L, horizDelay * 50L, TimeUnit.MILLISECONDS);

		float vertRadius = effect.getVertExpandRadius().get(data);
		int vertDelay = effect.getVertExpandDelay().get(data);
		if (vertDelay > 0 && vertRadius != 0)
			repeatingVertTask = Bukkit.getAsyncScheduler().runAtFixedRate(MagicSpells.getInstance(),
				t -> orbHeight += vertRadius, vertDelay * 50L, vertDelay * 50L, TimeUnit.MILLISECONDS);

		effectlibEffect = effect.playEffectLib(entity.getLocation(), data);
		if (effectlibEffect != null) effectlibEffect.infinite();
	}

	@Override
	public void run() {
		if (!canRun()) {
			stop();
			return;
		}

		xAxis += orbitXAxis;
		yAxis += orbitYAxis;
		zAxis += orbitZAxis;

		loc = effect.applyOffsets(getLocation(), data);

		effectlibEffect.setLocation(loc);
		if (effectlibEffect instanceof ModifiedEffect) {
			modifiedEffect = ((ModifiedEffect) effectlibEffect).getInnerEffect();
			if (modifiedEffect != null) modifiedEffect.setLocation(loc);
		}
	}

	private Location getLocation() {
		Vector perp;
		if (counterClockwise) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
		else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
		currentPosition.add(perp.multiply(distancePerTick)).normalize();
		Vector pos = VectorUtils.rotateVector(currentPosition.clone(), xAxis, yAxis, zAxis);
		return entity.getLocation().add(0, orbHeight, 0).add(pos.multiply(orbRadius)).setDirection(perp);
	}

	@Override
	public void stop() {
		super.stop();
		if (effectlibEffect != null) effectlibEffect.cancel();
		if (repeatingHorizTask != null) repeatingHorizTask.cancel();
		if (repeatingVertTask != null) repeatingVertTask.cancel();
		currentPosition = null;
	}

	public Effect getEffectlibEffect() {
		return effectlibEffect;
	}

	public boolean canRun() {
		if (entity == null) return false;
		if (!entity.isValid()) return false;
		if (!checker.isActive(entity)) return false;
		if (effect == null) return false;
		if (effectlibEffect == null) return false;
		return true;
	}

}
