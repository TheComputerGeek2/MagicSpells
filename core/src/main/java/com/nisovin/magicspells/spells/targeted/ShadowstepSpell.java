package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Float> yaw;
	private final ConfigData<Float> pitch;

	private final ConfigData<Double> distance;

	private final ConfigData<Boolean> snapToGround;

	private final ConfigData<Integer> requireGroundDistance;

	private final ConfigData<Vector> relativeOffset;

	private final String strNoLandingSpot;

	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		distance = getConfigDataDouble("distance", -1);

		snapToGround = getConfigDataBoolean("snap-to-ground", false);

		requireGroundDistance = getConfigDataInt("require-ground-distance", 0);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(-1, 0, 0));

		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location targetLoc = data.target().getLocation();
		targetLoc.setPitch(0);

		Vector relativeOffset = this.relativeOffset.get(data);

		double distance = this.distance.get(data);
		if (distance != -1) relativeOffset = relativeOffset.setX(distance);

		Util.applyRelativeOffset(targetLoc, relativeOffset);

		targetLoc.setPitch(pitch.get(data));
		targetLoc.setYaw(targetLoc.getYaw() + yaw.get(data));

		targetLoc = BlockUtils.adjustToSafeLocation(data.caster(), targetLoc, requireGroundDistance.get(data), snapToGround.get(data));
		if (targetLoc == null) return noTarget(strNoLandingSpot, data);

		playSpellEffects(data.caster(), targetLoc, data);
		data.caster().teleportAsync(targetLoc);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
