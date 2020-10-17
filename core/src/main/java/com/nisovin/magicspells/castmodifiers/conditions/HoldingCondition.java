package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import com.nisovin.magicspells.util.Util;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class HoldingCondition extends Condition {

	private Material[] ids;
	private short[] datas;
	private boolean[] checkData;
	private String[] names;
	private boolean[] checkName;
	
	@Override
	public boolean initialize(String var) {
		try {
			String[] vardata = var.split(",");
			ids = new Material[vardata.length];
			datas = new short[vardata.length];
			checkData = new boolean[vardata.length];
			names = new String[vardata.length];
			checkName = new boolean[vardata.length];
			for (int i = 0; i < vardata.length; i++) {
				if (vardata[i].contains("|")) {
					String[] subvardata = vardata[i].split("\\|");
					vardata[i] = subvardata[0];
					names[i] = Util.colorize(subvardata[1]).replace("__", " ");
					if (names[i].isEmpty()) names[i] = null;
					checkName[i] = true;
				} else {
					names[i] = null;
					checkName[i] = false;
				}
				if (vardata[i].contains(":")) {
					String[] subvardata = vardata[i].split(":");
					ids[i] = Material.matchMaterial(subvardata[0], true);
					if (subvardata[1].equals("*")) {
						datas[i] = 0;
						checkData[i] = false;
					} else {
						datas[i] = Short.parseShort(subvardata[1]);
						checkData[i] = true;
					}
				} else {
					ids[i] = Material.matchMaterial(vardata[i], true);
					datas[i] = 0;
					checkData[i] = false;
				}
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		ItemStack item = livingEntity.getEquipment().getItemInMainHand();
		return check(item);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		EntityEquipment equip = target.getEquipment();
		return equip != null && check(equip.getItemInMainHand());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
	private boolean check(ItemStack item) {
		if (item == null) return false;
		Material type = item.getType();
		ItemMeta meta = item.getItemMeta();
		int durability = meta instanceof Damageable ? ((Damageable) meta).getDamage() : 0;
		String name = null;
		try {
			if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) name = item.getItemMeta().getDisplayName();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == type && (!checkData[i] || datas[i] == durability) && (!checkName[i] || Objects.equals(names[i], name))) {
				return true;
			}
		}
		return false;
	}

}
