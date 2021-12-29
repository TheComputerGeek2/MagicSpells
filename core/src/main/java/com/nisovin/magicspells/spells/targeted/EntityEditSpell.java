package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.managers.AttributeManager;

public class EntityEditSpell extends TargetedSpell implements TargetedEntitySpell {

	private Set<AttributeManager.AttributeInfo> attributes;

	private boolean toggle;

	public EntityEditSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicSpells.getAttributeManager().getAttributes(attributeList);

		toggle = getConfigBoolean("toggle", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target == null) return noTarget(caster);

			applyAttributes(target.getTarget());
			playSpellEffects(caster, target.getTarget());
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playSpellEffects(caster, target);
		applyAttributes(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		applyAttributes(target);
		return true;
	}

	private void applyAttributes(LivingEntity entity) {
		if (attributes == null) return;
		if (toggle) {
			boolean apply = false;
			for (AttributeManager.AttributeInfo info : attributes) {
				apply = MagicSpells.getAttributeManager().hasEntityAttribute(entity, info);
			}
			if (!apply) MagicSpells.getAttributeManager().addEntityAttributes(entity, attributes);
			else MagicSpells.getAttributeManager().clearEntityAttributeModifiers(entity, attributes);
			return;
		}

		for (AttributeManager.AttributeInfo info : attributes) {
			if (MagicSpells.getAttributeManager().hasEntityAttribute(entity, info)) continue;
			MagicSpells.getAttributeManager().addEntityAttribute(entity, info);
		}
	}

}
