package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.util.managers.AttributeManager;

public class EntityEditJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Set<AttributeManager.AttributeInfo> attributes;

	private boolean toggle;

	public EntityEditJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicJutsus.getAttributeManager().getAttributes(attributeList);

		toggle = getConfigBoolean("toggle", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			applyAttributes(target.getTarget());
			playJutsuEffects(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playJutsuEffects(caster, target);
		applyAttributes(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playJutsuEffects(EffectPosition.TARGET, target);
		applyAttributes(target);
		return true;
	}

	private void applyAttributes(LivingEntity entity) {
		if (attributes == null) return;
		if (toggle) {
			boolean apply = false;
			for (AttributeManager.AttributeInfo info : attributes) {
				apply = MagicJutsus.getAttributeManager().hasEntityAttribute(entity, info);
			}
			if (!apply) MagicJutsus.getAttributeManager().addEntityAttributes(entity, attributes);
			else MagicJutsus.getAttributeManager().clearEntityAttributeModifiers(entity, attributes);
			return;
		}

		for (AttributeManager.AttributeInfo info : attributes) {
			if (MagicJutsus.getAttributeManager().hasEntityAttribute(entity, info)) continue;
			MagicJutsus.getAttributeManager().addEntityAttribute(entity, info);
		}
	}

}
