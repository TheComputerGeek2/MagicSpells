package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class SignTextCondition extends Condition {

	//world,x,y,z,text

	private MagicLocation location;
	private String[] text;

	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split(",");
			location = new MagicLocation(vars[0], Integer.parseInt(vars[1]), Integer.parseInt(vars[2]), Integer.parseInt(vars[3]));

			String[] sign = vars[4].split("\\\\n");
			text = new String[sign.length];

			for (int i = 0; i < sign.length; i++) {
				String replaced = sign[i].replaceAll("__", " ");
				text[i] = MagicSpells.getVolatileCodeHandler().colorize(replaced);
			}

			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return checkSignText();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return checkSignText();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return checkSignText();
	}

	public boolean checkSignText() {
		Block block = location.getLocation().getBlock();
		if (!block.getType().name().contains("SIGN")) return false;

		Sign sign = (Sign) block.getState();
		for (int i = 0; i < sign.getLines().length; i++) {
			if (text.length > i && !sign.getLine(i).contains(text[i])) return false;
		}

		return true;
	}

}
