package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.Objects;

public class WearingCondition extends Condition {

	Material[] ids;
	short[] datas;
	boolean[] checkData;
	String[] names;
	boolean[] checkName;

	@Override
	public boolean setVar(String var) {
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
					names[i] = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
					if (names[i].isEmpty()) names[i] = null;
					checkName[i] = true;
				} else {
					names[i] = null;
					checkName[i] = false;
				}
				if (vardata[i].contains(":")) {
					String[] subvardata = vardata[i].split(":");
					ids[i] = Material.getMaterial(subvardata[0]);
					if (subvardata[1].equals("*")) {
						datas[i] = 0;
						checkData[i] = false;
					} else {
						datas[i] = Short.parseShort(subvardata[1]);
						checkData[i] = true;
					}
				} else {
					ids[i] = Material.getMaterial(vardata[i]);
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
	public boolean check(Player player) {
		PlayerInventory inv = player.getInventory();
		if (check(inv.getHelmet())) return true;
		if (check(inv.getChestplate())) return true;
		if (check(inv.getLeggings())) return true;
		return check(inv.getBoots());
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) return check((Player)target);
		EntityEquipment equip = target.getEquipment();
		if (equip != null) {
			if (check(equip.getHelmet())) return true;
			if (check(equip.getChestplate())) return true;
			if (check(equip.getLeggings())) return true;
			return check(equip.getBoots());
		}
		return false;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}
	
	private boolean check(ItemStack item) {
		if (item == null) return false;
		Material thisid = item.getType();
		short thisdata = item.getDurability();
		String thisname = null;
		try {
			if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
				thisname = item.getItemMeta().getDisplayName();
			}
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == thisid && (!checkData[i] || datas[i] == thisdata) && (!checkName[i] || Objects.equals(names[i], thisname))) {
				return true;
			}
		}
		return false;
	}

}
