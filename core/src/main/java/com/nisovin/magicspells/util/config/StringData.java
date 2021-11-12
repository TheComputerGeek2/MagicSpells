package com.nisovin.magicspells.util.config;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class StringData implements ConfigData<String> {

	private final boolean targeted;
	private final String value;

	public StringData(String value, boolean targeted) {
		this.targeted = targeted;
		this.value = value;
	}

	@Override
	public String get(LivingEntity caster, LivingEntity target, float power, String[] args) {
		String ret = MagicSpells.doArgumentSubstitution(value, args);

		Player playerCaster = caster instanceof Player ? (Player) caster : null;
		Player playerTarget = target instanceof Player ? (Player) target : null;

		ret = MagicSpells.doVariableReplacements(playerCaster, ret);
		ret = MagicSpells.doTargetedVariableReplacements(playerCaster, playerTarget, ret);

		return ret;
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean isTargeted() {
		return targeted;
	}

}
