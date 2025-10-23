package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Float> yaw;
	private final ConfigData<Float> pitch;

	private final ConfigData<Vector> relativeOffset;

	private final String strCantTeleport;

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.1, 0));

		strCantTeleport = getConfigString("str-cant-teleport", "");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location targetLoc = data.target().getLocation();
		Location startLoc = data.caster().getLocation();

		Vector relativeOffset = this.relativeOffset.get(data);
		targetLoc.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(targetLoc, relativeOffset.setY(0));

		targetLoc.setPitch(startLoc.getPitch() - pitch.get(data));
		targetLoc.setYaw(startLoc.getYaw() + yaw.get(data));

		if (!targetLoc.getBlock().isPassable()) return noTarget(strCantTeleport, data);

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		playSpellEffects(EffectPosition.TARGET, data.target(), data);
		playSpellEffectsTrail(startLoc, targetLoc, data);

		data.caster().teleportAsync(targetLoc);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
