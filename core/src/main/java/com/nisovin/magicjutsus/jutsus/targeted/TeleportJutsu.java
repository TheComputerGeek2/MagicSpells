package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class TeleportJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private float yaw;
	private float pitch;

	private Vector relativeOffset;

	private String strCantTeleport;

	public TeleportJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		yaw = getConfigFloat("yaw", 0);
		pitch = getConfigFloat("pitch", 0);

		relativeOffset = getConfigVector("relative-offset", "0,0.1,0");

		strCantTeleport = getConfigString("str-cant-teleport", "");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			if (!teleport(caster, target.getTarget())) return noTarget(caster, strCantTeleport);

			sendMessages(caster, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return teleport(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private boolean teleport(LivingEntity caster, LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location startLoc = caster.getLocation();

		Vector startDir = startLoc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(startLoc.getDirection().multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(startLoc.getPitch() - pitch);
		targetLoc.setYaw(startLoc.getYaw() + yaw);

		if (!BlockUtils.isPathable(targetLoc.getBlock())) return false;

		playJutsuEffects(EffectPosition.CASTER, caster);
		playJutsuEffects(EffectPosition.TARGET, target);
		playJutsuEffectsTrail(startLoc, targetLoc);

		return caster.teleport(targetLoc);
	}

}
