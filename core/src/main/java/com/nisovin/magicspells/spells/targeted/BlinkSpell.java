package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class BlinkSpell extends TargetedSpell implements TargetedLocationSpell {

	private String strCantBlink;

	private boolean passThroughCeiling;
	
	public BlinkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strCantBlink = getConfigString("str-cant-blink", "You can't blink there.");

		passThroughCeiling = getConfigBoolean("pass-through-ceiling", false);
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = getRange(power);
			if (range <= 0) range = 25;
			if (range > 125) range = 125;
			BlockIterator iter; 
			try {
				iter = new BlockIterator(caster, range);
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

			if (found == null) return noTarget(caster, strCantBlink);

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
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, loc, power, args);
				EventUtil.call(event);

				if (event.isCancelled()) loc = null;
				else loc = event.getTargetLocation();
			}

			if (loc == null) return noTarget(caster, strCantBlink);

			loc.setX(loc.getX() + 0.5);
			loc.setZ(loc.getZ() + 0.5);
			loc.setPitch(caster.getLocation().getPitch());
			loc.setYaw(caster.getLocation().getYaw());

			playSpellEffects(caster, loc);
			caster.teleport(loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Location location = target.clone();
		location.setYaw(caster.getLocation().getYaw());
		location.setPitch(caster.getLocation().getPitch());

		playSpellEffects(caster, location);
		caster.teleport(location);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

}
