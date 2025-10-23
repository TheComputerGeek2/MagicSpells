package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class BlinkSpell extends TargetedSpell implements TargetedLocationSpell {

	private final ConfigData<Boolean> passThroughCeiling;

	private final String strCantBlink;

	public BlinkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strCantBlink = getConfigString("str-cant-blink", "You can't blink there.");

		passThroughCeiling = getConfigDataBoolean("pass-through-ceiling", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(strCantBlink, data);

		Block found = result.getHitBlock();
		Block prev = found.getRelative(result.getHitBlockFace());

		Location loc = null;
		if (!passThroughCeiling.get(data) && found.getRelative(0, -1, 0).equals(prev) && prev.isPassable()) {
			Block under = prev.getRelative(0, -1, 0);
			if (under.isPassable()) loc = under.getLocation().add(0.5, 0, 0.5);
		} else if (found.getRelative(0, 1, 0).isPassable() && found.getRelative(0, 2, 0).isPassable()) {
			loc = found.getLocation().add(0.5, 1, 0.5);
		} else if (prev.isPassable() && prev.getRelative(0, 1, 0).isPassable()) {
			loc = prev.getLocation().add(0.5, 0, 0.5);
		}
		if (loc == null) return noTarget(strCantBlink, data);

		loc.setPitch(data.caster().getPitch());
		loc.setYaw(data.caster().getYaw());

		SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, loc);
		if (!targetEvent.callEvent()) return noTarget(strCantBlink, targetEvent);

		return blink(targetEvent.getSpellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		return data.hasCaster() ? blink(data) : new CastResult(PostCastAction.ALREADY_HANDLED, data);
	}

	public CastResult blink(SpellData data) {
		Location target = data.location();

		target.setPitch(data.caster().getPitch());
		target.setYaw(data.caster().getYaw());
		data = data.location(target);

		playSpellEffects(data);
		data.caster().teleportAsync(target);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
