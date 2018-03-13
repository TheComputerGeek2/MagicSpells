package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private String strNoLandingSpot;
	private double distance;

	private float yaw;
	private float pitch;
	Vector relativeOffset;
	
	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
		distance = getConfigDouble("distance", -1);

		pitch = getConfigFloat("pitch", 0);
		yaw = getConfigFloat("yaw", 0);
		relativeOffset = getConfigVector("relative-offset", "-1,0,0");
		if (distance != -1) relativeOffset.setX(distance);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// Fail
				return noTarget(player);
			}
			
			boolean done = shadowstep(player, target.getTarget());
			if (!done) return noTarget(player, strNoLandingSpot);
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean shadowstep(Player player, LivingEntity target) {
		// Get landing location
		Location targetLoc = target.getLocation().clone();
		targetLoc.setPitch(0);

		Vector startDir = targetLoc.getDirection().setY(0).normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();

		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(targetLoc.getDirection().setY(0).multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(pitch);
		targetLoc.setYaw(targetLoc.getYaw() + yaw);

		// Check if clear
		Block b = targetLoc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) {
			// Fail - no landing spot
			return false;
		}

		// Ok
		playSpellEffects(player.getLocation(), targetLoc);
		player.teleport(targetLoc);

		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return shadowstep(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
