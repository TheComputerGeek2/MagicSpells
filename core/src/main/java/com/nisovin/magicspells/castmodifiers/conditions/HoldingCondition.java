package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("holding")
public class HoldingCondition extends Condition {

	private Material[] ids;
	private short[] datas;
	private boolean[] checkData;
	private Component[] names;
	private boolean[] checkName;
	
	@Override
	public boolean initialize(@NotNull String var) {
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
					names[i] = Util.getItemMiniMessage(subVarData[1].replace("__", " "));
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
		return checkHolding(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkHolding(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}
	
	private boolean checkHolding(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;

		ItemStack item = equipment.getItemInMainHand();
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
