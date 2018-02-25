package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private String strNoLandingSpot;
	private double distance;
	private double height;

	private float yaw;
	private float pitch;
	
	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
		distance = getConfigDouble("distance", -1);
		height = getConfigDouble("height", 0);

		pitch = getConfigFloat("pitch", 0);
		yaw = getConfigFloat("yaw", 0);
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
		Location targetLoc = target.getLocation();
		Vector facing = targetLoc.getDirection().setY(height).multiply(distance);
		Location loc = targetLoc.toVector().add(facing).toLocation(targetLoc.getWorld());
		loc.setPitch(pitch);
		loc.setYaw(targetLoc.getYaw() + yaw);
		
		// Check if clear
		Block b = loc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) {
			// Fail - no landing spot
			return false;
		}
		
		// Ok
		playSpellEffects(player.getLocation(), loc);
		player.teleport(loc);
		
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
