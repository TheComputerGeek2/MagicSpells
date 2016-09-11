package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class MoneyMoreThanCondition extends Condition {

	float money;
	
	@Override
	public boolean setVar(String var) {
		try {
			money = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return MagicSpells.getMoneyHandler().checkMoney(player) > money;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target instanceof Player? check((Player)target): false;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
