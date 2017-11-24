package com.nisovin.magicspells.util;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
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
	
	public static void setTypeAndData(Block block, Material material, byte data, boolean physics) {
		block.setTypeIdAndData(material.getId(), data, physics);
	}
	
	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		block.setTypeIdAndData(fallingBlock.getBlockId(), fallingBlock.getBlockData(), physics);
	}
	
	public static int getWaterLevel(Block block) {
		return block.getData();
	}
	
	public static int getGrowthLevel(Block block) {
		return block.getData();
	}
	
	public static void setGrowthLevel(Block block, int level) {
		block.setData((byte)level);
	}
	
	public static boolean growWarts(NetherWarts wart, int stagesToGrow) {
		if (wart.getState() == NetherWartsState.RIPE) return false;
		int state = wartStateToInt.get(wart.getState());
		state= Math.min(state+stagesToGrow, 4);
		wart.setState(intToWartState.get(state));
		return true;
		
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
				material == Material.SAPLING ||
				material == Material.WATER ||
				material == Material.STATIONARY_WATER ||
				material == Material.POWERED_RAIL ||
				material == Material.DETECTOR_RAIL ||
				material == Material.LONG_GRASS ||
				material == Material.DEAD_BUSH ||
				material == Material.YELLOW_FLOWER ||
				material == Material.RED_ROSE ||
				material == Material.BROWN_MUSHROOM ||
				material == Material.RED_MUSHROOM ||
				material == Material.TORCH ||
				material == Material.FIRE ||
				material == Material.REDSTONE_WIRE ||
				material == Material.CROPS ||
				material == Material.SIGN_POST ||
				material == Material.LADDER ||
				material == Material.RAILS ||
				material == Material.WALL_SIGN ||
				material == Material.LEVER ||
				material == Material.STONE_PLATE ||
				material == Material.WOOD_PLATE ||
				material == Material.REDSTONE_TORCH_OFF ||
				material == Material.REDSTONE_TORCH_ON ||
				material == Material.STONE_BUTTON ||
				material == Material.SNOW ||
				material == Material.SUGAR_CANE_BLOCK ||
				material == Material.VINE ||
				material == Material.WATER_LILY ||
				material == Material.NETHER_STALK ||
				material == Material.CARPET;
	}
	
	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}
	
}
