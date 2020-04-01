package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class HoldRightJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedLocationJutsu {

	private int resetTime;

	private float maxDuration;
	private float maxDistance;

	private boolean targetEntity;
	private boolean targetLocation;

	private Ninjutsu jutsuToCast;
	private String jutsuToCastName;
	
	private Map<UUID, CastData> casting;
	
	public HoldRightJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		resetTime = getConfigInt("reset-time", 250);

		maxDuration = getConfigFloat("max-duration", 0F);
		maxDistance = getConfigFloat("max-distance", 0F);

		targetEntity = getConfigBoolean("target-entity", true);
		targetLocation = getConfigBoolean("target-location", false);

		jutsuToCastName = getConfigString("jutsu", "");

		casting = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuToCast = new Ninjutsu(jutsuToCastName);
		if (!jutsuToCast.process()) {
			jutsuToCast = null;
			MagicJutsus.error("HoldRightJutsu '" + internalName + "' has an invalid jutsu defined!");
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			CastData data = casting.get(livingEntity.getUniqueId());
			if (data != null && data.isValid(livingEntity)) {
				data.cast(livingEntity);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (targetEntity) {
				TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
				if (target != null) data = new CastData(target.getTarget(), target.getPower());
				else return noTarget(livingEntity);
			} else if (targetLocation) {
				Block block = getTargetedBlock(livingEntity, power);
				if (block != null && block.getType() != Material.AIR) data = new CastData(block.getLocation().add(0.5, 0.5, 0.5), power);
				else return noTarget(livingEntity);
			} else data = new CastData(power);

			if (data != null) {
				data.cast(livingEntity);
				casting.put(livingEntity.getUniqueId(), data);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!targetLocation) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!targetEntity) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	private class CastData {

		private Location targetLocation = null;
		private LivingEntity targetEntity = null;

		private float power;

		private long start = System.currentTimeMillis();
		private long lastCast = 0;
		
		private CastData(LivingEntity target, float power) {
			targetEntity = target;
			this.power = power;
		}

		private CastData(Location target, float power) {
			targetLocation = target;
			this.power = power;
		}

		private CastData(float power) {
			this.power = power;
		}

		private boolean isValid(LivingEntity livingEntity) {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * TimeUtil.MILLISECONDS_PER_SECOND) return false;
			if (maxDistance > 0) {
				Location l = targetLocation;
				if (targetEntity != null) l = targetEntity.getLocation();
				if (l == null) return false;
				if (!l.getWorld().equals(livingEntity.getWorld())) return false;
				if (l.distanceSquared(livingEntity.getLocation()) > maxDistance * maxDistance) return false;
			}
			return true;
		}

		private void cast(LivingEntity caster) {
			lastCast = System.currentTimeMillis();
			if (targetEntity != null) jutsuToCast.castAtEntity(caster, targetEntity, power);
			else if (targetLocation != null) jutsuToCast.castAtLocation(caster, targetLocation, power);
			else jutsuToCast.cast(caster, power);
		}
		
	}

}
