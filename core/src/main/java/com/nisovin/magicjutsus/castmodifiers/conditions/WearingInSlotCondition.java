package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.materials.MagicMaterial;

public class WearingInSlotCondition extends Condition {

	private int slot = -1;
	private MagicMaterial mat = null;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] data = var.split("=");
			String s = data[0].toLowerCase();
			if (s.startsWith("helm") || s.startsWith("hat") || s.startsWith("head")) slot = 0;
			else if (s.startsWith("chest") || s.startsWith("tunic")) slot = 1;
			else if (s.startsWith("leg") || s.startsWith("pant")) slot = 2;
			else if (s.startsWith("boot") || s.startsWith("shoe") || s.startsWith("feet")) slot = 3;
			if (slot == -1) return false;
			if (data[1].equals("0") || data[1].equals("air") || data[1].equals("empty")) {
				mat = null;
			} else {
				mat = MagicJutsus.getItemNameResolver().resolveItem(data[1]);
				if (mat == null) return false;
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
		if (mat == null && (item == null || BlockUtils.isAir(item.getType()))) return true;
		return mat != null && item != null && mat.getMaterial() == item.getType();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
