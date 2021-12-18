package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class GripSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private ConfigData<Double> yOffset;
	private ConfigData<Double> locationOffset;

	private boolean checkGround;

	private Vector relativeOffset;

	private String strCantGrip;

	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigDataDouble("y-offset", 0);
		locationOffset = getConfigDataDouble("location-offset", 0);

		checkGround = getConfigBoolean("check-ground", true);

		relativeOffset = getConfigVector("relative-offset", "1,1,0");

		strCantGrip = getConfigString("str-cant-grip", "");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			if (!grip(caster, target.getTarget(), caster.getLocation(), power, args))
				return noTarget(caster, strCantGrip);

			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return grip(caster, target, caster.getLocation(), power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return grip(caster, target, from, power, args);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		return grip(null, target, from, power, args);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	private boolean grip(LivingEntity caster, LivingEntity target, Location from, float power, String[] args) {
		Location loc = from.clone();

		Vector startDir = loc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();

		Vector relativeOffset = this.relativeOffset.clone();

		double yOffset = this.yOffset.get(caster, target, power, args);
		if (yOffset != 0) relativeOffset.setY(yOffset);

		double locationOffset = this.locationOffset.get(caster, target, power, args);
		if (locationOffset != 0) relativeOffset.setX(locationOffset);

		loc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		loc.add(loc.getDirection().clone().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		if (checkGround && !BlockUtils.isPathable(loc.getBlock())) return false;

		playSpellEffects(EffectPosition.TARGET, target);
		playSpellEffectsTrail(from, loc);

		return target.teleport(loc);
	}

}
