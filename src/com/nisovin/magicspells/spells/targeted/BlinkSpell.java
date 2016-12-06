package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
public class BlinkSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private boolean passThroughCeiling;
	private boolean smokeTrail;
	private String strCantBlink = null;
	
	public BlinkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		passThroughCeiling = getConfigBoolean("pass-through-ceiling", false);
		smokeTrail = getConfigBoolean("smoke-trail", true);
		strCantBlink = getConfigString("str-cant-blink", "You can't blink there.");
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = getRange(power);
			if (range <= 0) range = 25;
			if (range > 125) range = 125;
			BlockIterator iter; 
			try {
				iter = new BlockIterator(player, range>0&&range<150?range:150);
			} catch (IllegalStateException e) {
				iter = null;
			}
			HashSet<Location> smokes = null;
			if (smokeTrail) {
				smokes = new HashSet<Location>();
			}
			Block prev = null;
			Block found = null;
			Block b;
			if (iter != null) {
				while (iter.hasNext()) {
					b = iter.next();
					if (BlockUtils.isTransparent(this, b)) {
						prev = b;
						if (smokeTrail) {
							if (b != null && smokes != null) {
								smokes.add(b.getLocation()); //TODO check null access here
							}
						}
					} else {
						found = b;
						break;
					}
				}
			}
			
			if (found != null) {
				Location loc = null;
				if (range > 0 && !inRange(found.getLocation(), player.getLocation(), range)) {
					//no op
				} else if (!passThroughCeiling && found.getRelative(0,-1,0).equals(prev)) {
					// trying to move upward
					if (BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0,-1,0))) { //TODO check null access here
						loc = prev.getRelative(0,-1,0).getLocation();
					}
				} else if (BlockUtils.isPathable(found.getRelative(0,1,0)) && BlockUtils.isPathable(found.getRelative(0,2,0))) {
					// try to stand on top
					loc = found.getLocation();
					loc.setY(loc.getY() + 1);
				} else if (prev != null && BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0,1,0))) {
					// no space on top, put adjacent instead
					loc = prev.getLocation();
				}
				if (loc != null) {
					SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc, power);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						loc = null;
					} else {
						loc = event.getTargetLocation();
					}
				}
				if (loc != null) {
					loc.setX(loc.getX()+.5);
					loc.setZ(loc.getZ()+.5);
					loc.setPitch(player.getLocation().getPitch());
					loc.setYaw(player.getLocation().getYaw());
					Location origLoc = player.getLocation();
					playSpellEffects(EffectPosition.CASTER, origLoc);
					teleport(player, loc, smokes);
					playSpellEffects(EffectPosition.TARGET, loc);
					playSpellEffectsTrail(origLoc, loc);
				} else {
					return noTarget(player, strCantBlink);
				}
			} else {
				return noTarget(player, strCantBlink);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void teleport(Player player, Location location, HashSet<Location> smokeLocs) {
		playSpellEffects(player.getLocation(), location);
		player.teleport(location);
		if (smokeTrail && smokeLocs != null) {
			for (Location l : smokeLocs) {
				l.getWorld().playEffect(l, Effect.SMOKE, 4);
			}
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		target.setYaw(caster.getLocation().getYaw());
		target.setPitch(caster.getLocation().getPitch());
		teleport(caster, target, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

}
