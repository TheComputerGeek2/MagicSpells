package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OverBlockCondition extends Condition {

	private int depth;
	private String blocks;

	//Block Data
	private Set<Material> types;
	private List<Material> mats;

	@Override
	public boolean initialize(String var) {
		//Lets TRY and catch some formatting mistakes for this modifier.
		try {
			String[] variable = var.split(";",2);
			blocks = variable[0];
			depth = Integer.parseInt(variable[1]);
		} catch (NumberFormatException e) { //Oh no, that variable[1] is somehow not a string? give them an Error!
			DebugHandler.debugNumberFormat(e);
			return false;
		} catch (ArrayIndexOutOfBoundsException missingSemiColon) { //No ; in modifier? Just great, give them an Error!
			MagicSpells.error("No ; seperator for depth was found!");
			return false;
		}

		//Checks if they put any blocks to compare with in the first place.
		if (blocks.isEmpty()) {
			MagicSpells.error("Didn't specify any blocks to compare with.");
			return false;
		}

		//We need to parse a list of the blocks required and check if they are valid.
		types = new HashSet<>();
		mats = new ArrayList<>();
		String[] split = blocks.split(",");

		for (String s : split) {
			Material mat = Util.getMaterial(s);
			if (mat == null || !mat.isBlock()) return false;
			types.add(mat);
			mats.add(mat);
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}

	//If target-modifiers are use, lets check based on the target's location.
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		//The first time around, we look at the block right underneath the player.
		Block block = location.clone().subtract(0, 1, 0).getBlock();

		//Alright, lets loop until we reach out depth value.
		//If at any point the block we detect is one of the blocks from our list, we are good to go.
		for (int i = 0; i < depth; i++) {
			//Compares the material of the block to the list of blocks.
			if (types.contains(block.getType())) {
				for (Material m : mats) {
					//If it is true, stops the loop and returns true;
					if (m.equals(block.getType())) return true;
				}
			}
			//Uses position of the next block under
			block = block.getRelative(BlockFace.DOWN);
		}
		return false;
	}

}
