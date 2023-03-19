package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.handlers.MagicXpHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class MagicXpAboveCondition extends Condition {

	private static MagicXpHandler handler;

	private String[] school;
	private int[] amount;
	
	@Override
	public boolean initialize(String var) {
		try {
			handler = MagicSpells.getMagicXpHandler();
			if (handler == null) return false;
			
			String[] vars = var.split(",");
			school = new String[vars.length];
			amount = new int[vars.length];
			for (int i = 0; i < vars.length; i++) {
				String[] split = vars[i].split(":");
				school[i] = split[0];
				amount[i] = Integer.parseInt(split[1]);
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return xpAbove(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return xpAbove(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean xpAbove(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;

		for (int i = 0; i < school.length; i++) {
			if (handler.getXp(pl, school[i]) < amount[i]) return false;
		}

		return true;
	}

}
