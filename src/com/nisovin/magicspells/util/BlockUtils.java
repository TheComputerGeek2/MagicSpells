package com.nisovin.magicspells.util;

import java.util.HashMap;
import java.util.List;

import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;

public class BlockUtils {
	
	private static HashMap<NetherWartsState, Integer> wartStateToInt = new HashMap<>();
	private static HashMap<Integer, NetherWartsState> intToWartState = new HashMap<>();
	
	static {
		wartStateToInt.put(NetherWartsState.SEEDED, 1);
		wartStateToInt.put(NetherWartsState.STAGE_ONE, 2);
		wartStateToInt.put(NetherWartsState.STAGE_TWO, 3);
		wartStateToInt.put(NetherWartsState.RIPE, 4);
		
		intToWartState.put(1, NetherWartsState.SEEDED);
		intToWartState.put(2, NetherWartsState.STAGE_ONE);
		intToWartState.put(3, NetherWartsState.STAGE_TWO);
		intToWartState.put(4, NetherWartsState.RIPE);
	}
	
	public static boolean isTransparent(Spell spell, Block block) {
		return spell.getLosTransparentBlocks().contains(block.getType());
	}
	
	public static Block getTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			if (spell != null) return entity.getTargetBlock(spell.getLosTransparentBlocks(), range);
			return entity.getTargetBlock(MagicSpells.getTransparentBlocks(), range);				
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}
	
	public static List<Block> getLastTwoTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			return entity.getLastTwoTargetBlocks(spell.getLosTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}
	
	public static void setTypeAndData(Block block, Material material, boolean physics) {
		block.setType(material, physics);
	}
	
	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		block.setBlockData(fallingBlock.getBlockData(), physics);
	}
	
	public static int getWaterLevel(Block block) {
		return block.getData();
	}
	
	public static int getGrowthLevel(Block block) {
		return block.getData();
	}

	
	public static boolean growWarts(NetherWarts wart, int stagesToGrow) {
		if (wart.getState() == NetherWartsState.RIPE) return false;
		int state = wartStateToInt.get(wart.getState());
		state= Math.min(state+stagesToGrow, 4);
		wart.setState(intToWartState.get(state));
		return true;
		
	}

	public static void setGrowthLevel(Crops block, int amount){
		if(block.getState() == CropState.RIPE)return;
		block.setState(CropState.getByData((byte) Math.min(block.getState().getData(), 7 - amount)));
	}
	
	public static int getWaterLevel(BlockState blockState) {
		return blockState.getRawData();
	}
	
	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}
	
	// TODO try using a switch for this
	public static boolean isPathable(Material material) {
		return
				material == Material.AIR ||
				material.name().endsWith("SAPLING") ||
				material == Material.WATER ||
				material == Material.POWERED_RAIL ||
				material == Material.DETECTOR_RAIL ||
				material == Material.TALL_GRASS ||
				material == Material.DEAD_BUSH ||
				material == Material.DANDELION ||
				material == Material.POPPY ||
				material == Material.BROWN_MUSHROOM ||
				material == Material.RED_MUSHROOM ||
				material == Material.TORCH ||
				material == Material.FIRE ||
				material == Material.REDSTONE_WIRE ||
				material == Material.WHEAT ||
				material == Material.SIGN ||
				material == Material.LADDER ||
				material == Material.RAIL ||
				material == Material.WALL_SIGN ||
				material == Material.LEVER ||
				material.name().endsWith("PRESSURE_PLATE") ||
				material == Material.REDSTONE_TORCH ||
				material == Material.REDSTONE_WALL_TORCH ||
				material == Material.STONE_BUTTON ||
				material == Material.SNOW ||
				material == Material.SUGAR_CANE ||
				material == Material.VINE ||
				material == Material.LILY_PAD ||
				material == Material.NETHER_WART_BLOCK ||
				material.name().endsWith("CARPET");
	}
	
	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}
	
}
