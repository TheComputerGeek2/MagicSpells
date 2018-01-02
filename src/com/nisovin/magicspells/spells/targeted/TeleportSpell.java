package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	Vector relativeOffset;
	float pitch;
	float yaw;
	String strCantTeleport;

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0.1,0");
		pitch = getConfigFloat("pitch", 0);
		yaw = getConfigFloat("yaw", 0);
		strCantTeleport = getConfigString("str-cant-teleport", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {

			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			if (!teleport(player, target.getTarget())) return noTarget(player, strCantTeleport);
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	boolean teleport(Player caster, LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location startLoc = caster.getLocation();

		//apply relativeOffset
		Vector startDir = startLoc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(startLoc.getDirection().multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		//set the pitch and yaw
		targetLoc.setPitch(startLoc.getPitch() - pitch);
		targetLoc.setYaw(startLoc.getYaw() + yaw);

		if (!BlockUtils.isPathable(targetLoc.getBlock())) return false;

		playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.TARGET, target);
		playSpellEffectsTrail(startLoc, targetLoc);

		return caster.teleport(targetLoc);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return teleport(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
