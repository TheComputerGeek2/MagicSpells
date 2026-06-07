package com.nisovin.magicspells.spells.targeted.ext;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class GlowSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final DeprecationNotice DEPRECATION_NOTICE = new DeprecationNotice(
		"The '.targeted.ext.GlowSpell' spell class does not function, as the XGlow plugin is abandoned.",
		"Use the '.targeted.GlowSpell' spell class."
	);

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		MagicSpells.getDeprecationManager().addDeprecation(this, DEPRECATION_NOTICE);
	}

	@Override
	public CastResult cast(SpellData data) {
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
