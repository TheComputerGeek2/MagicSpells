package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class ManaCondition extends OperatorCondition {

	private ManaHandler mana;

	private int amount;
	private boolean percent = false;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		String number = var.substring(1);

		mana = MagicSpells.getManaHandler();
		if (mana == null) return false;

		try {
			if (number.endsWith("%")) {
				percent = true;
				number = number.replace("%", "");
			}
			amount = Integer.parseInt(number);
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return mana(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return mana(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean mana(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		double currentMana = mana.getMana(pl);
		double percentMana = currentMana / mana.getMaxMana(pl) * 100D;
		if (equals) {
			if (percent) return percentMana == amount;
			return currentMana == amount;
		} else if (moreThan) {
			if (percent) return percentMana > amount;
			return currentMana > amount;
		} else if (lessThan) {
			if (percent) return percentMana < amount;
			return currentMana < amount;
		}
		return false;
	}

}
