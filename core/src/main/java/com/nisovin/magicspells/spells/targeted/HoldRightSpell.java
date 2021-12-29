package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class HoldRightSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private ConfigData<Integer> resetTime;

	private ConfigData<Float> maxDuration;
	private ConfigData<Float> maxDistance;

	private boolean targetEntity;
	private boolean targetLocation;

	private Subspell spellToCast;
	private String spellToCastName;

	private Map<UUID, CastData> casting;

	public HoldRightSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		resetTime = getConfigDataInt("reset-time", 250);

		maxDuration = getConfigDataFloat("max-duration", 0F);
		maxDistance = getConfigDataFloat("max-distance", 0F);

		targetEntity = getConfigBoolean("target-entity", true);
		targetLocation = getConfigBoolean("target-location", false);

		spellToCastName = getConfigString("spell", "");

		casting = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			spellToCast = null;
			MagicSpells.error("HoldRightSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			CastData data = casting.get(caster.getUniqueId());
			if (data != null && data.isValid(caster)) {
				data.cast(caster);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (targetEntity) {
				TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
				if (target != null) data = new CastData(caster, target.getTarget(), target.getPower(), args);
				else return noTarget(caster);
			} else if (targetLocation) {
				Block block = getTargetedBlock(caster, power);
				if (block != null && block.getType() != Material.AIR)
					data = new CastData(caster, block.getLocation().add(0.5, 0.5, 0.5), power, args);
				else return noTarget(caster);
			} else data = new CastData(caster, power, args);

			if (data != null) {
				data.cast(caster);
				casting.put(caster.getUniqueId(), data);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		if (!targetLocation) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}

		data = new CastData(caster, target, power, args);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);

		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!targetEntity) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}

		data = new CastData(caster, target, power, args);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);

		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private class CastData {

		private Location targetLocation = null;
		private LivingEntity targetEntity = null;

		private final float maxDistance;
		private final float maxDuration;
		private final int resetTime;
		private final float power;

		private long start = System.currentTimeMillis();
		private long lastCast = 0;

		private CastData(LivingEntity caster, LivingEntity target, float power, String[] args) {
			targetEntity = target;
			this.power = power;

			maxDistance = HoldRightSpell.this.maxDistance.get(caster, target, power, args);
			maxDuration = HoldRightSpell.this.maxDuration.get(caster, target, power, args);
			resetTime = HoldRightSpell.this.resetTime.get(caster, target, power, args);
		}

		private CastData(LivingEntity caster, Location target, float power, String[] args) {
			targetLocation = target;
			this.power = power;

			maxDistance = HoldRightSpell.this.maxDistance.get(caster, null, power, args);
			maxDuration = HoldRightSpell.this.maxDuration.get(caster, null, power, args);
			resetTime = HoldRightSpell.this.resetTime.get(caster, null, power, args);
		}

		private CastData(LivingEntity caster, float power, String[] args) {
			this.power = power;

			maxDistance = HoldRightSpell.this.maxDistance.get(caster, null, power, args);
			maxDuration = HoldRightSpell.this.maxDuration.get(caster, null, power, args);
			resetTime = HoldRightSpell.this.resetTime.get(caster, null, power, args);
		}

		private boolean isValid(LivingEntity livingEntity) {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * TimeUtil.MILLISECONDS_PER_SECOND)
				return false;
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
			if (targetEntity != null) spellToCast.castAtEntity(caster, targetEntity, power);
			else if (targetLocation != null) spellToCast.castAtLocation(caster, targetLocation, power);
			else spellToCast.cast(caster, power);
		}

	}

}
