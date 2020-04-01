package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

import org.apache.commons.math3.util.FastMath;

public class AreaEffectJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private List<Ninjutsu> jutsus;
	private List<String> jutsuNames;

	private int maxTargets;

	private double cone;
	private double vRadius;
	private double hRadius;

	private boolean pointBlank;
	private boolean failIfNoTargets;
	private boolean jutsuSourceInCenter;
	
	public AreaEffectJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		jutsuNames = getConfigStringList("jutsus", null);

		maxTargets = getConfigInt("max-targets", 0);

		cone = getConfigDouble("cone", 0);
		vRadius = getConfigDouble("vertical-radius", 5);
		hRadius = getConfigDouble("horizontal-radius", 10);

		pointBlank = getConfigBoolean("point-blank", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		jutsuSourceInCenter = getConfigBoolean("jutsu-source-in-center", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		jutsus = new ArrayList<>();

		if (jutsuNames == null || jutsuNames.isEmpty()) {
			MagicJutsus.error("AreaEffectJutsu '" + internalName + "' has no jutsus defined!");
			return;
		}
		
		for (String jutsuName : jutsuNames) {
			Ninjutsu jutsu = new Ninjutsu(jutsuName);

			if (!jutsu.process()) {
				MagicJutsus.error("AreaEffectJutsu '" + internalName + "' attempted to use invalid jutsu '" + jutsuName + '\'');
				continue;
			}

			if (!jutsu.isTargetedLocationJutsu() && !jutsu.isTargetedEntityFromLocationJutsu() && !jutsu.isTargetedEntityJutsu()) {
				MagicJutsus.error("AreaEffectJutsu '" + internalName + "' attempted to use non-targeted jutsu '" + jutsuName + '\'');
				continue;
			}

			jutsus.add(jutsu);
		}

		jutsuNames.clear();
		jutsuNames = null;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = livingEntity.getLocation();
			else {
				try {
					Block block = getTargetedBlock(livingEntity, power);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation().add(0.5, 0, 0.5);
				} catch (IllegalStateException e) {
					loc = null;
				}
			}

			if (loc == null) return noTarget(livingEntity);

			JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, loc, power);
			EventUtil.call(event);
			if (event.isCancelled()) loc = null;
			else {
				loc = event.getTargetLocation();
				power = event.getPower();
			}

			if (loc == null) return noTarget(livingEntity);
			
			boolean done = doAoe(livingEntity, loc, power);
			
			if (!done && failIfNoTargets) return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return doAoe(null, target, power);
	}
	
	private boolean doAoe(LivingEntity livingEntity, Location location, float basePower) {
		int count = 0;

		Vector vLoc = livingEntity != null ? livingEntity.getLocation().toVector() : location.toVector();
		Vector facing = livingEntity != null ? livingEntity.getLocation().getDirection() : location.getDirection();

		BoundingBox box = new BoundingBox(location, hRadius, vRadius);
		List<Entity> entities = new ArrayList<>(location.getWorld().getEntitiesByClasses(LivingEntity.class));
		Collections.shuffle(entities);

		for (Entity e : entities) {
			if (e == null) continue;
			if (!box.contains(e)) continue;
			if (pointBlank && cone > 0) {
				Vector dir = e.getLocation().toVector().subtract(vLoc);
				if (FastMath.toDegrees(FastMath.abs(dir.angle(facing))) > cone) continue;
			}

			LivingEntity target = (LivingEntity) e;
			float power = basePower;

			if (target.isDead()) continue;
			if (livingEntity == null && !validTargetList.canTarget(target)) continue;
			if (livingEntity != null && !validTargetList.canTarget(livingEntity, target)) continue;

			JutsuTargetEvent event = new JutsuTargetEvent(this, livingEntity, target, power);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			target = event.getTarget();
			power = event.getPower();

			for (Ninjutsu jutsu : jutsus) {
				if (jutsuSourceInCenter && jutsu.isTargetedEntityFromLocationJutsu()) jutsu.castAtEntityFromLocation(livingEntity, location, target, power);
				else if (livingEntity != null && jutsu.isTargetedEntityFromLocationJutsu()) jutsu.castAtEntityFromLocation(livingEntity, livingEntity.getLocation(), target, power);
				else if (jutsu.isTargetedEntityJutsu()) jutsu.castAtEntity(livingEntity, target, power);
				else if (jutsu.isTargetedLocationJutsu()) jutsu.castAtLocation(livingEntity, target.getLocation(), power);
			}

			playJutsuEffects(EffectPosition.TARGET, target);
			if (jutsuSourceInCenter) playJutsuEffectsTrail(location, target.getLocation());
			else if (livingEntity != null) playJutsuEffectsTrail(livingEntity.getLocation(), target.getLocation());

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;
		}

		if (count > 0 || !failIfNoTargets) {
			playJutsuEffects(EffectPosition.SPECIAL, location);
			if (livingEntity != null) playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		
		return count > 0;
	}
	
}
