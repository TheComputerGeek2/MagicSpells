package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Pose;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class PoseSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Pose> pose;

	private final ConfigData<Boolean> fixed;

	public PoseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		pose = getConfigDataEnum("pose", Pose.class, null);
		fixed = getConfigDataBoolean("fixed", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		Pose pose = this.pose.get(data);
		if (pose == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		try {
			data.target().setPose(pose, fixed.get(data));
		} catch (IllegalArgumentException e) {
			MagicSpells.error("PoseSpell '" + internalName + "' failed to set pose: " + e.getMessage());
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
