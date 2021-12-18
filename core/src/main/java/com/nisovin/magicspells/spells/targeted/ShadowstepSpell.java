package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Float> yaw;
	private ConfigData<Float> pitch;

	private ConfigData<Double> distance;

	private Vector relativeOffset;

	private String strNoLandingSpot;

	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		distance = getConfigDataDouble("distance", -1);

		relativeOffset = getConfigVector("relative-offset", "-1,0,0");

		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);

			boolean done = shadowstep(caster, target.getTarget(), target.getPower(), args);
			if (!done) return noTarget(caster, strNoLandingSpot);
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return shadowstep(caster, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private boolean shadowstep(LivingEntity caster, LivingEntity target, float power, String[] args) {
		Location targetLoc = target.getLocation().clone();
		targetLoc.setPitch(0);

		Vector startDir = targetLoc.getDirection().setY(0).normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();

		double distance = this.distance.get(caster, target, power, args);
		Vector relativeOffset = distance != -1 ? this.relativeOffset.clone().setX(distance) : this.relativeOffset;

		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(targetLoc.getDirection().setY(0).multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(pitch.get(caster, target, power, args));
		targetLoc.setYaw(targetLoc.getYaw() + yaw.get(caster, target, power, args));

		Block b = targetLoc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) return false;

		playSpellEffects(caster.getLocation(), targetLoc);
		caster.teleport(targetLoc);

		return true;
	}

}
