package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class GripSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	float locationOffset;
	float yOffset;
	Vector relativeOffset;
	String strCantGrip;

	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigFloat("y-offset", 0);
		locationOffset = getConfigFloat("location-offset", 0);
		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");

		if (locationOffset != 0) relativeOffset.setX(locationOffset);
		if (yOffset != 0) relativeOffset.setY(yOffset);

		strCantGrip = getConfigString("str-cant-grip", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {

			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			if (!grip(player.getLocation(), target.getTarget())) return noTarget(player, strCantGrip);
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return grip(caster.getLocation(), target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		return grip(from, target);
	}

	private boolean grip(Location from, LivingEntity target) {
		Location loc = from.clone();

		Vector startDir = loc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		loc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		loc.add(loc.getDirection().clone().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		if (!BlockUtils.isPathable(loc.getBlock())) return false;

		playSpellEffects(EffectPosition.TARGET, target);
		playSpellEffectsTrail(from, loc);

		return target.teleport(loc);
	}

}
