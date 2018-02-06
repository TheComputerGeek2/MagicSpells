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
import com.nisovin.magicspells.castmodifiers.Condition;

public class UnderBlockCondition extends Condition {

	int height;
	String blocks;
	Set<Material> types;
	List<MagicMaterial> mats;
	MagicMaterial mat;


	@Override
	public boolean setVar(String var) {

		String[] variable = var.split(";",2);
		blocks = variable[0];

		//Checks if a height was inserted. Defaults to 10.
		if (variable[1].equals("")) {
			height = 10;
		}
		else {
			height = Interger.parseInt(variable[1]);
		}

		//Checks if they put any blocks to compare with in the first place.
		if (blocks.equals("")) {
			MagicSpells.error("Didn't specify any blocks to compare with.");
		}

		//We need to parse a list of the blocks required and check if they are valid.
		if (blocks.contains(",")) {
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
		mat = MagicSpells.getItemNameResolver().resolveBlock(var);
		return mat != null;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		//The first time around, we look at the block above a 2m tall player.
		Block block = location.clone().add(0, 2, 0).getBlock();

		for (int i = 0; i < height; i++)
			{
				//Compares the material of the block to the list of blocks.
				if (mat != null) return mat.equals(block);
				if (types.contains(block.getType())) {
					for (MagicMaterial m : mats) {
						if (m.equals(block)) return true;
					}

				//Gets position of the next block up
				block = block.getRelative(BlockFace.UP);
			}
		}
		return false;
	}

}
