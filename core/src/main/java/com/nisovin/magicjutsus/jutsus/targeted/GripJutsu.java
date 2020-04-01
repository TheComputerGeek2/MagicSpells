package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class GripJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private float yOffset;
	private float locationOffset;

	private Vector relativeOffset;

	private String strCantGrip;

	public GripJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		yOffset = getConfigFloat("y-offset", 0);
		locationOffset = getConfigFloat("location-offset", 0);

		relativeOffset = getConfigVector("relative-offset", "1,1,0");

		strCantGrip = getConfigString("str-cant-grip", "");

		if (locationOffset != 0) relativeOffset.setX(locationOffset);
		if (yOffset != 0) relativeOffset.setY(yOffset);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			if (!grip(livingEntity.getLocation(), target.getTarget())) return noTarget(livingEntity, strCantGrip);

			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return grip(caster.getLocation(), target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return grip(from, target);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		return grip(from, target);
	}

	private boolean grip(Location from, LivingEntity target) {
		Location loc = from.clone();

		Vector startDir = loc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		loc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		loc.add(loc.getDirection().clone().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		if (!BlockUtils.isPathable(loc.getBlock())) return false;

		playJutsuEffects(EffectPosition.TARGET, target);
		playJutsuEffectsTrail(from, loc);

		return target.teleport(loc);
	}

}
