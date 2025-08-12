package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class EntityDataApplySpell extends TargetedSpell implements TargetedEntitySpell {

	private final EntityData entityData;

	public EntityDataApplySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entityData = new EntityData(getConfigSection("entity"));
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasTarget()) return noTarget(data);

		entityData.apply(data.target(), data);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
