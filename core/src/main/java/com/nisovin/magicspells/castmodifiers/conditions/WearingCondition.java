package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class WearingCondition extends Condition {

	private Material[] ids;
	private short[] datas;
	private Component[] names;
	private boolean[] checkName;
	private boolean[] checkData;

	@Override
	public boolean initialize(String var) {
		try {
			String[] varData = var.split(",");
			ids = new Material[varData.length];
			datas = new short[varData.length];
			checkData = new boolean[varData.length];
			names = new Component[varData.length];
			checkName = new boolean[varData.length];
			for (int i = 0; i < varData.length; i++) {
				if (varData[i].contains("|")) {
					String[] subVarData = varData[i].split("\\|");
					varData[i] = subVarData[0];
					names[i] = Util.getMiniMessage(subVarData[1].replace("__", " "));
					checkName[i] = true;
				} else {
					names[i] = null;
					checkName[i] = false;
				}
				if (varData[i].contains(":")) {
					String[] subVarData = varData[i].split(":");
					ids[i] = Util.getMaterial(subVarData[0]);
					if (subVarData[1].equals("*")) {
						datas[i] = 0;
						checkData[i] = false;
					} else {
						datas[i] = Short.parseShort(subVarData[1]);
						checkData[i] = true;
					}
				} else {
					ids[i] = Util.getMaterial(varData[i]);
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
	public boolean check(LivingEntity caster) {
		return checkInventory(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkInventory(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkInventory(LivingEntity target) {
		EntityEquipment equip = target.getEquipment();
		if (equip == null) return false;

		if (checkItem(equip.getHelmet())) return true;
		if (checkItem(equip.getChestplate())) return true;
		if (checkItem(equip.getLeggings())) return true;
		return checkItem(equip.getBoots());
	}
	
	private boolean checkItem(ItemStack item) {
		if (item == null) return false;
		Material type = item.getType();
		ItemMeta meta = item.getItemMeta();
		int durability = meta instanceof Damageable ? ((Damageable) meta).getDamage() : 0;
		Component name = null;
		try {
			if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) name = item.getItemMeta().displayName();
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
