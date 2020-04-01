package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class BlinkJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private String strCantBlink;

	private boolean passThroughCeiling;
	
	public BlinkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		strCantBlink = getConfigString("str-cant-blink", "You can't blink there.");

		passThroughCeiling = getConfigBoolean("pass-through-ceiling", false);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			int range = getRange(power);
			if (range <= 0) range = 25;
			if (range > 125) range = 125;
			BlockIterator iter; 
			try {
				iter = new BlockIterator(livingEntity, range > 0 && range <= 125 ? range : 125);
			} catch (IllegalStateException e) {
				iter = null;
			}

			Block b;
			Block prev = null;
			Block found = null;

			if (iter != null) {
				while (iter.hasNext()) {
					b = iter.next();
					if (BlockUtils.isTransparent(this, b)) prev = b;
					else {
						found = b;
						break;
					}
				}
			}

			if (found == null) return noTarget(livingEntity, strCantBlink);

			Location loc = null;
			if (!passThroughCeiling && found.getRelative(0, -1, 0).equals(prev)) {
				// Trying to move upward
				if (BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0, -1, 0))) {
					loc = prev.getRelative(0, -1, 0).getLocation();
				}
			} else if (BlockUtils.isPathable(found.getRelative(0, 1, 0)) && BlockUtils.isPathable(found.getRelative(0, 2, 0))) {
				// Try to stand on top
				loc = found.getLocation();
				loc.setY(loc.getY() + 1);
			} else if (prev != null && BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0, 1, 0))) {
				// No space on top, put adjacent instead
				loc = prev.getLocation();
			}
			if (loc != null) {
				JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, loc, power);
				EventUtil.call(event);

				if (event.isCancelled()) loc = null;
				else loc = event.getTargetLocation();
			}

			if (loc == null) return noTarget(livingEntity, strCantBlink);

			loc.setX(loc.getX() + 0.5);
			loc.setZ(loc.getZ() + 0.5);
			loc.setPitch(livingEntity.getLocation().getPitch());
			loc.setYaw(livingEntity.getLocation().getYaw());

			playJutsuEffects(livingEntity, loc);
			livingEntity.teleport(loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Location location = target.clone();
		location.setYaw(caster.getLocation().getYaw());
		location.setPitch(caster.getLocation().getPitch());

		playJutsuEffects(caster, location);
		caster.teleport(location);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

}
