package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class ShadowstepJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private float yaw;
	private float pitch;

	private double distance;

	private Vector relativeOffset;

	private String strNoLandingSpot;

	public ShadowstepJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		yaw = getConfigFloat("yaw", 0);
		pitch = getConfigFloat("pitch", 0);

		distance = getConfigDouble("distance", -1);

		relativeOffset = getConfigVector("relative-offset", "-1,0,0");

		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");

		if (distance != -1) relativeOffset.setX(distance);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean done = shadowstep(livingEntity, target.getTarget());
			if (!done) return noTarget(livingEntity, strNoLandingSpot);
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return shadowstep(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private boolean shadowstep(LivingEntity caster, LivingEntity target) {
		Location targetLoc = target.getLocation().clone();
		targetLoc.setPitch(0);

		Vector startDir = targetLoc.getDirection().setY(0).normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();

		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(targetLoc.getDirection().setY(0).multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(pitch);
		targetLoc.setYaw(targetLoc.getYaw() + yaw);

		Block b = targetLoc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) return false;

		playJutsuEffects(caster.getLocation(), targetLoc);
		caster.teleport(targetLoc);

		return true;
	}

}
