package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class UnderBlockCondition extends Condition {

	//Configuration
	int height;
	String blocks;

	//Block Data
	Set<Material> types;
	List<MagicMaterial> mats;

	@Override
	public boolean setVar(String var) {
		//Lets TRY and catch some formatting mistakes for this modifier.
		try {
			String[] variable = var.split(";",2);
			blocks = variable[0];
			height = Integer.parseInt(variable[1]);
		} catch (NumberFormatException e) { //Oh no, that variable[1] is somehow not a string? give them an Error!
			DebugHandler.debugNumberFormat(e);
			return false;
		} catch (ArrayIndexOutOfBoundsException missingSemiColon) { //No ; in modifier? Just great, give them an Error!
			MagicSpells.error("No ; seperator for height was found!");
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
			MagicMaterial mat = MagicSpells.getItemNameResolver().resolveBlock(s);
			if (mat == null) return false;
			types.add(mat.getMaterial());
			mats.add(mat);
		}
		return true;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	//If target-modifiers are use, lets check based on the target's location.
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		Block block = location.clone().getBlock();

		//Alright, lets loop until we reach out height value.
		//If at any point the block we detect is one of the blocks from our list, we are good to go.
		for (int i = 0; i < height; i++) {
			//Compares the material of the block to the list of blocks.
			if (types.contains(block.getType())) {
				for (MagicMaterial m : mats) {
					//If it is true, stops the loop and returns true;
					if (m.equals(block)) return true;
				}
			}
			//Uses position of the next block up
			block = block.getRelative(BlockFace.UP);
		}
		return false;
	}

}
