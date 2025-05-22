package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SwingHandSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<HandType> hand;

	public SwingHandSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		hand = getConfigDataEnum("hand", HandType.class, HandType.MAINHAND);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		data.target().swingHand(hand.get(data).slot);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private enum HandType {

		MAINHAND(EquipmentSlot.HAND),
		OFFHAND(EquipmentSlot.OFF_HAND);

		private final EquipmentSlot slot;

		HandType(EquipmentSlot slot) {
			this.slot = slot;
		}
	}

}
