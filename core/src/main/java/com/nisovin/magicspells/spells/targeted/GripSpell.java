package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class GripSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> locationOffset;

	private final ConfigData<Boolean> checkGround;

	private final ConfigData<Vector> relativeOffset;

	private String strCantGrip;

	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigDataDouble("y-offset", 0);
		locationOffset = getConfigDataDouble("location-offset", 0);

		checkGround = getConfigDataBoolean("check-ground", true);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(1, 1, 0));

		strCantGrip = getConfigString("str-cant-grip", "");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntityFromLocation(info.spellData().location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		Location loc = data.location();

		Vector relativeOffset = this.relativeOffset.get(data);

		double yOffset = this.yOffset.get(data);
		if (yOffset == 0) yOffset = relativeOffset.getY();

		double locationOffset = this.locationOffset.get(data);
		if (locationOffset != 0) relativeOffset.setX(locationOffset);

		Util.applyRelativeOffset(loc, relativeOffset.setY(0));
		loc.add(0, yOffset, 0);

		data = data.location(loc);

		if (checkGround.get(data) && !loc.getBlock().isPassable()) return noTarget(strCantGrip, data);

		Util.tryTeleportMountedAsync(data.target(), loc);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
