package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ManaSpell extends InstantSpell {

	private ConfigData<Integer> mana;

	private boolean powerAffectsMana;

	public ManaSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		mana = getConfigDataInt("mana", 25);
		powerAffectsMana = getConfigBoolean("power-affects-mana", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			int amount = mana.get(caster, null, power, args);
			if (powerAffectsMana) amount = Math.round(amount * power);

			boolean added = MagicSpells.getManaHandler().addMana(player, amount, ManaChangeReason.OTHER);
			if (!added) return PostCastAction.ALREADY_HANDLED;
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
