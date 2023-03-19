package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class MaxManaCondition extends OperatorCondition {

	private ManaHandler mana;

	private int amount = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		mana = MagicSpells.getManaHandler();
		if (mana == null) return false;

		try {
			amount = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return maxMana(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return maxMana(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean maxMana(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return mana.getMaxMana(pl) == amount;
		else if (moreThan) return mana.getMaxMana(pl) > amount;
		else if (lessThan) return mana.getMaxMana(pl) < amount;
		return false;
	}

}
