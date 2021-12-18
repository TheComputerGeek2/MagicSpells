package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Float> yaw;
	private ConfigData<Float> pitch;

	private Vector relativeOffset;

	private String strCantTeleport;

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		relativeOffset = getConfigVector("relative-offset", "0,0.1,0");

		strCantTeleport = getConfigString("str-cant-teleport", "");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			if (!teleport(caster, target.getTarget(), target.getPower(), args)) return noTarget(caster, strCantTeleport);

			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return teleport(caster, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private boolean teleport(LivingEntity caster, LivingEntity target, float power, String[] args) {
		Location targetLoc = target.getLocation();
		Location startLoc = caster.getLocation();

		Vector startDir = startLoc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(startLoc.getDirection().multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(startLoc.getPitch() - pitch.get(caster, target, power, args));
		targetLoc.setYaw(startLoc.getYaw() + yaw.get(caster, target, power, args));

		if (!BlockUtils.isPathable(targetLoc.getBlock())) return false;

		playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.TARGET, target);
		playSpellEffectsTrail(startLoc, targetLoc);

		return caster.teleport(targetLoc);
	}

}
