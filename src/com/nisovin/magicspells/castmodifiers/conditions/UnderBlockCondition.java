package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.RegexUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class UnderBlockCondition extends Condition {

	int height;
	String blocks;

	@Override
	public boolean setVar(String var) {

		String[] variable = var.split(";",2)
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
			MagicSpells.error("Didn't specify any blocks to compare with.")
		}

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
		mat = MagicSpells.getItemNameResolver().resolveBlock(blocks);
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
		Block block = location.clone().add(0, 2, 0).getBlock();
		if (mat != null) return mat.equals(block);
		if (types.contains(block.getType())) {
			for (MagicMaterial m : mats) {
				if (m.equals(block)) return true;
			}
		}
		return false;
	}

}
