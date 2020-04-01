package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class BeamJutsu extends InstantJutsu implements TargetedLocationJutsu, TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private double hitRadius;
	private double maxDistance;
	private double verticalHitRadius;

	private float gravity;
	private float interval;
	private float rotation;
	private float beamVertOffset;
	private float beamHorizOffset;

	private boolean changePitch;
	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;

	private Ninjutsu hitJutsu;
	private Ninjutsu endJutsu;
	private Ninjutsu travelJutsu;
	private Ninjutsu groundJutsu;

	private String hitJutsuName;
	private String endJutsuName;
	private String travelJutsuName;
	private String groundJutsuName;

	public BeamJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		hitRadius = getConfigDouble("hit-radius", 2);
		maxDistance = getConfigDouble("max-distance", 30);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);

		float yOffset = getConfigFloat("y-offset", 0F);
		gravity = getConfigFloat("gravity", 0F);
		interval = getConfigFloat("interval", 1F);
		rotation = getConfigFloat("rotation", 0F);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0F);

		changePitch = getConfigBoolean("change-pitch", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		hitJutsuName = getConfigString("jutsu", "");
		endJutsuName = getConfigString("jutsu-on-end", "");
		travelJutsuName = getConfigString("jutsu-on-travel", "");
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");

		gravity *= -1;
		if (interval < 0.01) interval = 0.01F;
		if (yOffset != 0) relativeOffset.setY(yOffset);
	}

	@Override
	public void initialize() {
		super.initialize();

		hitJutsu = new Ninjutsu(hitJutsuName);
		if (!hitJutsu.process()) {
			if (!hitJutsuName.isEmpty()) MagicJutsus.error("BeamJutsu '" + internalName + "' has an invalid jutsu defined!");
			hitJutsu = null;
		}

		endJutsu = new Ninjutsu(endJutsuName);
		if (!endJutsu.process() || !endJutsu.isTargetedLocationJutsu()) {
			if (!endJutsuName.isEmpty()) MagicJutsus.error("BeamJutsu '" + internalName + "' has an invalid jutsu-on-end defined!");
			endJutsu = null;
		}

		travelJutsu = new Ninjutsu(travelJutsuName);
		if (!travelJutsu.process() || !travelJutsu.isTargetedLocationJutsu()) {
			if (!travelJutsuName.isEmpty()) MagicJutsus.error("BeamJutsu '" + internalName + "' has an invalid jutsu-on-travel defined!");
			travelJutsu = null;
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("BeamJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
			groundJutsu = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			new Beam(livingEntity, livingEntity.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity livingEntity, LivingEntity target, float power) {
		new Beam(livingEntity, livingEntity.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location location, float v) {
		new Beam(livingEntity, location, v);
		return true;
	}

	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		new Beam(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return false;
	}

	private class Beam {

		private Set<Entity> immune;

		private LivingEntity caster;
		private LivingEntity target;

		private Location startLoc;
		private Location currentLoc;

		private float power;

		private Beam(LivingEntity caster, Location from, float power) {
			this.caster = caster;
			this.power = power;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);
			immune = new HashSet<>();

			shootBeam();
		}

		private Beam(LivingEntity caster, Location from, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);
			immune = new HashSet<>();

			shootBeam();
		}

		private void shootBeam() {
			playJutsuEffects(EffectPosition.CASTER, caster);

			if (beamVertOffset != 0) startLoc.setPitch(startLoc.getPitch() - beamVertOffset);
			if (beamHorizOffset != 0) startLoc.setYaw(startLoc.getYaw() + beamHorizOffset);

			Vector startDir;
			if (target == null) startDir = startLoc.getDirection().normalize();
			else startDir = target.getLocation().toVector().subtract(startLoc.clone().toVector()).normalize();

			//apply relative offset
			Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
			startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLoc.add(startLoc.getDirection().clone().multiply(relativeOffset.getX()));
			startLoc.setY(startLoc.getY() + relativeOffset.getY());

			currentLoc = startLoc.clone();

			//apply target relative offset
			Location targetLoc = null;
			if (target != null) {
				targetLoc = target.getLocation().clone();
				startDir = targetLoc.clone().getDirection().normalize();
				horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
				targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
				targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
				targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());
			}

			Vector dir;
			if (target == null) dir = startLoc.getDirection().multiply(interval);
			else dir = targetLoc.toVector().subtract(startLoc.clone().toVector()).normalize().multiply(interval);

			BoundingBox box = new BoundingBox(currentLoc, hitRadius, verticalHitRadius);

			float d = 0;
			mainLoop:
			while (d < maxDistance) {

				d += interval;
				currentLoc.add(dir);

				if (rotation != 0) Util.rotateVector(dir, rotation);
				if (gravity != 0) dir.add(new Vector(0, gravity,0));
				if (rotation != 0 || gravity != 0) currentLoc.setDirection(dir);

				//check block collision
				if (!isTransparent(currentLoc.getBlock())) {
					playJutsuEffects(EffectPosition.DISABLED, currentLoc);
					if (groundJutsu != null) groundJutsu.castAtLocation(caster, currentLoc, power);
					if (stopOnHitGround) break;
				}

				playJutsuEffects(EffectPosition.SPECIAL, currentLoc);

				if (travelJutsu != null) travelJutsu.castAtLocation(caster, currentLoc, power);

				box.setCenter(currentLoc);

				//check entities in the beam range
				for (LivingEntity e : startLoc.getWorld().getLivingEntities()) {
					if (e.equals(caster)) continue;
					if (e.isDead()) continue;
					if (immune.contains(e)) continue;
					if (!box.contains(e)) continue;
					if (validTargetList != null && !validTargetList.canTarget(e)) continue;

					JutsuTargetEvent event = new JutsuTargetEvent(BeamJutsu.this, caster, e, power);
					EventUtil.call(event);
					if (event.isCancelled()) continue;
					LivingEntity entity = event.getTarget();

					if (hitJutsu != null) {
						if (hitJutsu.isTargetedEntityJutsu()) hitJutsu.castAtEntity(caster, entity, event.getPower());
						else if (hitJutsu.isTargetedLocationJutsu()) hitJutsu.castAtLocation(caster, entity.getLocation(), event.getPower());
					}

					playJutsuEffects(EffectPosition.TARGET, entity);
					playJutsuEffectsTrail(caster.getLocation(), entity.getLocation());
					immune.add(e);

					if (stopOnHitEntity) break mainLoop;
				}
			}

			//end of the beam
			if (d >= maxDistance) {
				playJutsuEffects(EffectPosition.DELAYED, currentLoc);
				if (endJutsu != null) endJutsu.castAtLocation(caster, currentLoc, power);
			}
		}

	}

}
