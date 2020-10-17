package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class MoneyCondition extends OperatorCondition {

	private float money;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			money = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return money(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return money(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean money(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		if (equals) return MagicSpells.getMoneyHandler().checkMoney((Player) livingEntity) == money;
		else if (moreThan) return MagicSpells.getMoneyHandler().checkMoney((Player) livingEntity) > money;
		else if (lessThan) return MagicSpells.getMoneyHandler().checkMoney((Player) livingEntity) < money;
		return false;
	}

}
