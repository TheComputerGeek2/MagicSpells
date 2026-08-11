package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;
import java.util.function.Predicate;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class BeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> targetRelativeOffset;

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> hitRadius;
	private final ConfigData<Double> maxDistance;
	private final ConfigData<Double> verticalHitRadius;
	private final ConfigData<Double> verticalRotation;
	private final ConfigData<Double> horizontalRotation;

	private final ConfigData<Float> gravity;
	private final ConfigData<Float> interval;
	private final ConfigData<Float> rotation;
	private final ConfigData<Float> beamVertOffset;
	private final ConfigData<Float> beamHorizOffset;

	private final ConfigData<Float> beamVerticalSpread;
	private final ConfigData<Float> beamHorizontalSpread;

	private final ConfigData<Boolean> changePitch;
	private final ConfigData<Boolean> stopOnHitEntity;
	private final ConfigData<Boolean> stopOnHitGround;

	private Subspell hitSpell;
	private Subspell endSpell;
	private Subspell travelSpell;
	private Subspell groundSpell;
	private Subspell entityLocationSpell;

	private final String hitSpellName;
	private final String endSpellName;
	private final String travelSpellName;
	private final String groundSpellName;
	private final String entityLocationSpellName;

	private NoMagicZoneManager zoneManager;

	private static final double ANGLE_Y = AccurateMath.toRadians(-90);

	public BeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.5, 0));
		targetRelativeOffset = getConfigDataVector("target-relative-offset", new Vector(0, 0.5, 0));

		yOffset = getConfigDataDouble("y-offset", 0F);
		hitRadius = getConfigDataDouble("hit-radius", 2);
		maxDistance = getConfigDataDouble("max-distance", 30);
		verticalHitRadius = getConfigDataDouble("vertical-hit-radius", 2);

		verticalRotation = getConfigDataDouble("vertical-rotation", 0D);
		horizontalRotation = getConfigDataDouble("horizontal-rotation", 0D);

		gravity = getConfigDataFloat("gravity", 0F);
		interval = getConfigDataFloat("interval", 1F);
		rotation = getConfigDataFloat("rotation", 0F);
		beamVertOffset = getConfigDataFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigDataFloat("beam-horiz-offset", 0F);

		ConfigData<Float> beamSpread = getConfigDataFloat("beam-spread", 0F);
		beamVerticalSpread = getConfigDataFloat("beam-vertical-spread", beamSpread);
		beamHorizontalSpread = getConfigDataFloat("beam-horizontal-spread", beamSpread);

		changePitch = getConfigDataBoolean("change-pitch", true);
		stopOnHitEntity = getConfigDataBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		travelSpellName = getConfigString("spell-on-travel", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "BeamSpell '" + internalName + "' has an invalid '%s' defined!";
		hitSpell = initSubspell(hitSpellName,
				error.formatted("spell"),
				true);
		endSpell = initSubspell(endSpellName,
				error.formatted("spell-on-end"),
				true);
		travelSpell = initSubspell(travelSpellName,
				error.formatted("spell-on-travel"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		entityLocationSpell = initSubspell(entityLocationSpellName,
				error.formatted("spell-on-entity-location"),
				true);

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public CastResult cast(SpellData data) {
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		return castAtEntityFromLocation(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location loc = data.location();
		if (!changePitch.get(data)) {
			loc.setPitch(0);
			data = data.location(loc);
		}

		float beamVertOffset = this.beamVertOffset.get(data);
		if (beamVertOffset != 0) {
			loc.setPitch(loc.getPitch() - beamVertOffset);
			data = data.location(loc);
		}

		float beamHorizOffset = this.beamHorizOffset.get(data);
		if (beamHorizOffset != 0) {
			loc.setYaw(loc.getYaw() + beamHorizOffset);
			data = data.location(loc);
		}

		Vector direction;
		if (!data.hasTarget()) direction = loc.getDirection();
		else {
			direction = data.target().getLocation().subtract(loc).toVector();
			direction = direction.isZero() ? loc.getDirection() : direction.normalize();
		}

		//apply relative offset
		Vector relativeOffset = this.relativeOffset.get(data);

		double yOffset = this.yOffset.get(data);
		if (yOffset == 0) yOffset = relativeOffset.getY();

		Util.applyRelativeOffset(loc, relativeOffset.setY(0));
		loc.add(0, yOffset, 0);

		float interval = this.interval.get(data);
		if (interval < 0.01) interval = 0.01f;

		if (data.hasTarget()) {
			Vector targetRelativeOffset = this.targetRelativeOffset.get(data);
			double targetYOffset = targetRelativeOffset.getY();
			Location targetLoc = data.target().getLocation();

			Util.applyRelativeOffset(targetLoc, targetRelativeOffset.setY(0));
			targetLoc.add(0, targetYOffset, 0);

			direction = targetLoc.subtract(loc).toVector();
			direction = direction.isZero() ? loc.getDirection() : direction.normalize();
		}

		loc.setDirection(direction);
		float yaw = loc.getYaw(), pitch = loc.getPitch();

		double verticalRotation = this.verticalRotation.get(data);
		if (verticalRotation != 0) {
			Vector angleZ = Util.rotateVector(new Vector(0, 0, 1), yaw, pitch);
			direction.rotateAroundAxis(angleZ, AccurateMath.toRadians(verticalRotation));
		}

		double horizontalRotation = this.horizontalRotation.get(data);
		if (horizontalRotation != 0) {
			Vector angleY = Util.rotateVector(new Vector(0, -1, 0), yaw, pitch);
			direction.rotateAroundAxis(angleY, AccurateMath.toRadians(horizontalRotation));
		}

		Vector step = direction.clone().multiply(interval);

		float beamVerticalSpread = this.beamVerticalSpread.get(data);
		float beamHorizontalSpread = this.beamHorizontalSpread.get(data);
		if (beamVerticalSpread > 0 || beamHorizontalSpread > 0) {
			float rx = -1 + random.nextFloat() * 2;
			float ry = -1 + random.nextFloat() * 2;
			float rz = -1 + random.nextFloat() * 2;

			step.add(new Vector(rx * beamHorizontalSpread, ry * beamVerticalSpread, rz * beamHorizontalSpread));
		}

		double verticalHitRadius = this.verticalHitRadius.get(data);
		double maxDistance = this.maxDistance.get(data);
		double hitRadius = this.hitRadius.get(data);

		float rotation = this.rotation.get(data);
		float gravity = -this.gravity.get(data);

		boolean stopOnHitEntity = this.stopOnHitEntity.get(data);
		boolean stopOnHitGround = this.stopOnHitGround.get(data);

		Predicate<Location> transparent = isTransparent(data);
		Set<Entity> immune = new HashSet<>();
		float d = 0;

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		SpellData locData = data.noTargeting();

		mainLoop:
		while (d < maxDistance) {
			d += interval;
			loc.add(step);

			if (rotation != 0 || gravity != 0) {
				if (rotation != 0) Util.rotateVector(step, rotation);
				if (gravity != 0) step.setY(step.getY() + gravity);

				loc.setDirection(step);
			}

			loc = Util.makeFinite(loc);
			locData = locData.location(loc);

			if (zoneManager.willFizzle(loc, this)) break;

			//check block collision
			if (!transparent.test(loc)) {
				playSpellEffects(EffectPosition.DISABLED, loc, locData);
				if (groundSpell != null) groundSpell.subcast(locData);
				if (stopOnHitGround) break;
			}

			playSpellEffects(EffectPosition.SPECIAL, loc, locData);

			if (travelSpell != null) travelSpell.subcast(locData);

			//check entities in the beam range
			for (LivingEntity e : loc.getNearbyLivingEntities(hitRadius, verticalHitRadius)) {
				if (!e.isValid() || immune.contains(e)) continue;
				if (!validTargetList.canTarget(locData.caster(), e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(this, locData, e);
				if (!event.callEvent()) continue;

				SpellData subData = event.getSpellData();
				LivingEntity entity = event.getTarget();

				if (entityLocationSpell != null) entityLocationSpell.subcast(subData.noTarget());
				if (hitSpell != null) {
					SpellPreImpactEvent preImpact = new SpellPreImpactEvent(hitSpell.getSpell(), BeamSpell.this, subData);
					if (preImpact.callEvent()) {
						subData = preImpact.getSpellData();

						if (preImpact.getRedirected()) {
							step.multiply(-1);
							locData = subData.caster(entity);
							continue mainLoop;
						} else hitSpell.subcast(subData.noLocation());
					}
				}

				playSpellEffects(EffectPosition.TARGET, entity, subData);
				playSpellEffectsTrail(subData.caster().getLocation(), entity.getLocation(), subData);
				immune.add(entity);
				if (stopOnHitEntity) break mainLoop;
			}
		}

		//end of the beam
		if (!zoneManager.willFizzle(loc, this) && d >= maxDistance) {
			playSpellEffects(EffectPosition.DELAYED, loc, locData.location(loc));
			if (endSpell != null) endSpell.subcast(locData);
		}

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public Subspell getHitSpell() {
		return hitSpell;
	}

	public void setHitSpell(Subspell hitSpell) {
		this.hitSpell = hitSpell;
	}

	public Subspell getEndSpell() {
		return endSpell;
	}

	public void setEndSpell(Subspell endSpell) {
		this.endSpell = endSpell;
	}

	public Subspell getTravelSpell() {
		return travelSpell;
	}

	public void setTravelSpell(Subspell travelSpell) {
		this.travelSpell = travelSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

}
