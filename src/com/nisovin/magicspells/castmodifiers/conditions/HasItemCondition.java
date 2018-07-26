package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.Objects;

public class HasItemCondition extends Condition {

	Material mat;
	short data;
	boolean checkData;
	String name;
	boolean checkName;
	
	@Override
	public boolean setVar(String var) {
		try {
			if (var.contains("|")) {
				String[] subvardata = var.split("\\|");
				var = subvardata[0];
				name = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
				if (name.isEmpty()) name = null;
				checkName = true;
			} else {
				name = null;
				checkName = false;
			}
			if (var.contains(":")) {
				String[] vardata = var.split(":");
				mat = Material.getMaterial(vardata[0]);
				if (vardata[1].equals("*")) {
					data = 0;
					checkData = false;
				} else {
					data = Short.parseShort(vardata[1]);
					checkData = true;
				}
			} else {
				mat = Material.getMaterial(var);
				checkData = false;
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return check(player.getInventory());
	}
	
	private boolean check(Inventory inventory) {
		if (inventory == null) return false;
		if (checkData || checkName) {
			for (ItemStack item : inventory.getContents()) {
				if (item == null) continue;
				String thisname = null;
				try {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
						thisname = item.getItemMeta().getDisplayName();
					}
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
				if (item.getType() == mat && (!checkData || item.getDurability() == data) && (!checkName || Objects.equals(thisname, name))) return true;
			}
			return false;
		}
		return inventory.contains(mat);
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target == null) return false;
		return target instanceof InventoryHolder && check(((InventoryHolder)target).getInventory());
	}
	
	@Override
	public boolean check(Player player, Location location) {
		Block target = location.getBlock();
		if (target == null) return false;
		
		BlockState targetState = target.getState();
		if (targetState == null) return false;
		return targetState instanceof InventoryHolder && check(((InventoryHolder)targetState).getInventory());
	}

}
