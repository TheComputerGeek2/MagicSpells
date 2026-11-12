package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class BlinkSpell extends TargetedSpell implements TargetedLocationSpell {

	private final String strCantBlink;

	private final ConfigData<Integer> requireGroundDistance;

	private final ConfigData<Boolean> snapToGround;
	private final ConfigData<Boolean> passThroughCeiling;

	public BlinkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strCantBlink = getConfigString("str-cant-blink", "You can't blink there.");

		requireGroundDistance = getConfigDataInt("require-ground-distance", 0);

		snapToGround = getConfigDataBoolean("snap-to-ground", false);
		passThroughCeiling = getConfigDataBoolean("pass-through-ceiling", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(strCantBlink, data);

		Block found = result.getHitBlock();
		BlockFace face = result.getHitBlockFace();
		Block prev = found.getRelative(face);

		Location loc = null;

		boolean snapToGround = this.snapToGround.get(data);
		int requireGroundDistance = this.requireGroundDistance.get(data);

		// Under
		if (face == BlockFace.DOWN && !passThroughCeiling.get(data)) {
			Location target = prev.getLocation().add(0.5, -1, 0.5);
			loc = BlockUtils.adjustToSafeLocation(data.caster(), target, requireGroundDistance, snapToGround);
		}
		// Above
		if (loc == null) loc = BlockUtils.adjustToSafeLocation(data.caster(), found.getLocation().add(0.5, 0, 0.5));
		// Side
		if (loc == null) loc = BlockUtils.adjustToSafeLocation(data.caster(), prev.getLocation().add(0.5, 0, 0.5), requireGroundDistance, snapToGround);

		if (loc == null) return noTarget(strCantBlink, data);

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
