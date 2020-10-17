package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class WearingInSlotCondition extends Condition {

	private int slot = -1;

	private Material material;
	
	@Override
	public boolean initialize(String var) {
		try {
			String[] data = var.split("=");
			String s = data[0].toLowerCase();
			if (s.startsWith("helm") || s.startsWith("hat") || s.startsWith("head")) slot = 0;
			else if (s.startsWith("chest") || s.startsWith("tunic")) slot = 1;
			else if (s.startsWith("leg") || s.startsWith("pant")) slot = 2;
			else if (s.startsWith("boot") || s.startsWith("shoe") || s.startsWith("feet")) slot = 3;
			if (slot == -1) return false;
			if (data[1].equals("0") || data[1].equals("air") || data[1].equals("empty")) {
				material = null;
			} else {
				material = Util.getMaterial(data[1]);
				if (material == null) return false;
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
		ItemStack item = target.getEquipment().getArmorContents()[slot];
		if (material == null && (item == null || BlockUtils.isAir(item.getType()))) return true;
		return material != null && item != null && material == item.getType();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
