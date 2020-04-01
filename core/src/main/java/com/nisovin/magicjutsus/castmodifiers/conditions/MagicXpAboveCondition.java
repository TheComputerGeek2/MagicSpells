package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.MagicXpHandler;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class MagicXpAboveCondition extends Condition {

	private static MagicXpHandler handler;

	private String[] school;
	private int[] amount;
	
	@Override
	public boolean setVar(String var) {
		try {
			handler = MagicJutsus.getMagicXpHandler();
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
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(target instanceof Player)) return false;
		for (int i = 0; i < school.length; i++) {
			if (handler.getXp((Player) target, school[i]) < amount[i]) return false;
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
