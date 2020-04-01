package com.nisovin.magicjutsus.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;

public class BlockUtils {

	public static List<Block> getNearbyBlocks(Location location, int radius, int height) {
		List<Block> blocks = new ArrayList<>();
		for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
			for (int y = location.getBlockY() - height; y <= location.getBlockY() + height; y++) {
				for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
					blocks.add(location.getWorld().getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

	public static boolean isTransparent(Jutsu jutsu, Block block) {
		return jutsu.getLosTransparentBlocks().contains(block.getType());
	}

	public static Block getTargetBlock(Jutsu jutsu, LivingEntity entity, int range) {
		try {
			if (jutsu != null) return entity.getTargetBlock(jutsu.getLosTransparentBlocks(), range);
			return entity.getTargetBlock(MagicJutsus.getTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static List<Block> getLastTwoTargetBlock(Jutsu jutsu, LivingEntity entity, int range) {
		try {
			return entity.getLastTwoTargetBlocks(jutsu.getLosTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static void setTypeAndData(Block block, Material material, BlockData data, boolean physics) {
		block.setType(material);
		block.setBlockData(data, physics);
	}

	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		BlockData blockData = fallingBlock.getBlockData();
		block.setType(blockData.getMaterial());
		block.setBlockData(blockData, physics);
	}

	public static int getWaterLevel(Block block) {
		return ((Levelled) block.getBlockData()).getLevel();
	}

	public static int getGrowthLevel(Block block) {
		return ((Ageable) block.getBlockData()).getAge();
	}

	public static void setGrowthLevel(Block block, int level) {
		Ageable age = ((Ageable) block.getBlockData());
		age.setAge(level);
		block.setBlockData(age);
	}

	public static int getWaterLevel(BlockState blockState) {
		return ((Levelled) blockState.getBlockData()).getLevel();
	}

	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}

	public static boolean isAir(Material m) {
		return
				m == Material.AIR ||
				m == Material.VOID_AIR ||
				m == Material.CAVE_AIR;
	}

	public static boolean isBed(Material m) {
		return
				m == Material.WHITE_BED ||
				m == Material.RED_BED ||
				m == Material.PURPLE_BED ||
				m == Material.PINK_BED ||
				m == Material.ORANGE_BED ||
				m == Material.MAGENTA_BED ||
				m == Material.LIME_BED ||
				m == Material.LIGHT_GRAY_BED ||
				m == Material.LIGHT_BLUE_BED ||
				m == Material.GREEN_BED ||
				m == Material.GRAY_BED ||
				m == Material.CYAN_BED ||
				m == Material.YELLOW_BED ||
				m == Material.BROWN_BED ||
				m == Material.BLUE_BED ||
				m == Material.BLACK_BED;
	}

	public static boolean isWoodDoor(Material m) {
		return
				m == Material.OAK_DOOR ||
				m == Material.ACACIA_DOOR ||
				m == Material.JUNGLE_DOOR ||
				m == Material.SPRUCE_DOOR ||
				m == Material.DARK_OAK_DOOR ||
				m == Material.BIRCH_DOOR;
	}

	public static boolean isWoodButton(Material m) {
		return
				m == Material.OAK_BUTTON ||
				m == Material.ACACIA_BUTTON ||
				m == Material.JUNGLE_BUTTON ||
				m == Material.SPRUCE_BUTTON ||
				m == Material.DARK_OAK_BUTTON ||
				m == Material.BIRCH_BUTTON;
	}

	public static boolean isWoodPressurePlate(Material m) {
		return
				m == Material.OAK_PRESSURE_PLATE ||
				m == Material.ACACIA_PRESSURE_PLATE ||
				m == Material.JUNGLE_PRESSURE_PLATE ||
				m == Material.SPRUCE_PRESSURE_PLATE ||
				m == Material.DARK_OAK_PRESSURE_PLATE ||
				m == Material.BIRCH_PRESSURE_PLATE;
	}

	public static boolean isWoodTrapdoor(Material m) {
		return
				m == Material.OAK_TRAPDOOR ||
				m == Material.ACACIA_TRAPDOOR ||
				m == Material.JUNGLE_TRAPDOOR ||
				m == Material.SPRUCE_TRAPDOOR ||
				m == Material.DARK_OAK_TRAPDOOR ||
				m == Material.BIRCH_TRAPDOOR;
	}

	public static boolean isWood(Material m) {
		return
				m == Material.OAK_WOOD ||
				m == Material.ACACIA_WOOD ||
				m == Material.JUNGLE_WOOD ||
				m == Material.SPRUCE_WOOD ||
				m == Material.DARK_OAK_WOOD ||
				m == Material.BIRCH_WOOD;
	}

	public static boolean isLog(Material m) {
		return
				m == Material.OAK_LOG ||
				m == Material.ACACIA_LOG ||
				m == Material.JUNGLE_LOG||
				m == Material.SPRUCE_LOG ||
				m == Material.DARK_OAK_LOG ||
				m == Material.BIRCH_LOG;
	}

	// TODO try using a switch for this
	public static boolean isPathable(Material mat) {
		return
				mat == Material.AIR ||
				mat == Material.CAVE_AIR ||
				mat == Material.VOID_AIR ||
				mat == Material.OAK_SAPLING ||
				mat == Material.ACACIA_SAPLING ||
				mat == Material.JUNGLE_SAPLING||
				mat == Material.SPRUCE_SAPLING ||
				mat == Material.DARK_OAK_SAPLING ||
				mat == Material.BIRCH_SAPLING ||
				mat == Material.WATER ||
				mat == Material.TALL_GRASS ||
				mat == Material.LARGE_FERN ||
				mat == Material.GRASS ||
				mat == Material.DEAD_BUSH ||
				mat == Material.FERN ||
				mat == Material.SEAGRASS ||
				mat == Material.TALL_SEAGRASS ||
				mat == Material.LILY_PAD ||
				mat == Material.DANDELION ||
				mat == Material.POPPY ||
				mat == Material.BLUE_ORCHID ||
				mat == Material.ALLIUM ||
				mat == Material.AZURE_BLUET ||
				mat == Material.ORANGE_TULIP ||
				mat == Material.PINK_TULIP ||
				mat == Material.RED_TULIP ||
				mat == Material.WHITE_TULIP ||
				mat == Material.OXEYE_DAISY ||
				mat == Material.SUNFLOWER ||
				mat == Material.LILAC ||
				mat == Material.PEONY ||
				mat == Material.ROSE_BUSH ||
				mat == Material.BROWN_MUSHROOM ||
				mat == Material.RED_MUSHROOM ||
				mat == Material.TORCH ||
				mat == Material.FIRE ||
				mat == Material.REDSTONE_WIRE ||
				mat == Material.WHEAT ||
				mat.name().contains("SIGN") ||
				mat == Material.LADDER ||
				mat == Material.RAIL ||
				mat == Material.ACTIVATOR_RAIL ||
				mat == Material.DETECTOR_RAIL ||
				mat == Material.POWERED_RAIL ||
				mat == Material.LEVER ||
				mat == Material.REDSTONE_TORCH ||
				mat == Material.STONE_BUTTON ||
				mat == Material.OAK_BUTTON ||
				mat == Material.ACACIA_BUTTON ||
				mat == Material.JUNGLE_BUTTON||
				mat == Material.SPRUCE_BUTTON ||
				mat == Material.DARK_OAK_BUTTON ||
				mat == Material.BIRCH_BUTTON ||
				mat == Material.SNOW ||
				mat == Material.SUGAR_CANE ||
				mat == Material.VINE ||
				mat == Material.NETHER_WART ||
				mat == Material.BLACK_CARPET ||
				mat == Material.BLUE_CARPET ||
				mat == Material.CYAN_CARPET ||
				mat == Material.BROWN_CARPET ||
				mat == Material.GRAY_CARPET ||
				mat == Material.GREEN_CARPET ||
				mat == Material.LIGHT_BLUE_CARPET ||
				mat == Material.LIGHT_GRAY_CARPET ||
				mat == Material.LIME_CARPET ||
				mat == Material.MAGENTA_CARPET ||
				mat == Material.ORANGE_CARPET ||
				mat == Material.PINK_CARPET ||
				mat == Material.PURPLE_CARPET ||
				mat == Material.RED_CARPET ||
				mat == Material.WHITE_CARPET ||
				mat == Material.YELLOW_CARPET ||
				mat == Material.ACACIA_PRESSURE_PLATE ||
				mat == Material.BIRCH_PRESSURE_PLATE ||
				mat == Material.DARK_OAK_PRESSURE_PLATE ||
				mat == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
				mat == Material.JUNGLE_PRESSURE_PLATE ||
				mat == Material.LIGHT_WEIGHTED_PRESSURE_PLATE ||
				mat == Material.OAK_PRESSURE_PLATE ||
				mat == Material.SPRUCE_PRESSURE_PLATE ||
				mat == Material.STONE_PRESSURE_PLATE ||
				mat == Material.TUBE_CORAL ||
				mat == Material.BRAIN_CORAL ||
				mat == Material.BUBBLE_CORAL ||
				mat == Material.FIRE_CORAL ||
				mat == Material.HORN_CORAL ||
				mat == Material.DEAD_TUBE_CORAL ||
				mat == Material.DEAD_BRAIN_CORAL ||
				mat == Material.DEAD_BUBBLE_CORAL ||
				mat == Material.DEAD_FIRE_CORAL ||
				mat == Material.DEAD_HORN_CORAL ||
				mat == Material.TUBE_CORAL_FAN ||
				mat == Material.BRAIN_CORAL_FAN ||
				mat == Material.BUBBLE_CORAL_FAN ||
				mat == Material.FIRE_CORAL_FAN ||
				mat == Material.HORN_CORAL_FAN ||
				mat == Material.DEAD_TUBE_CORAL_FAN ||
				mat == Material.DEAD_BRAIN_CORAL_FAN ||
				mat == Material.DEAD_BUBBLE_CORAL_FAN ||
				mat == Material.DEAD_FIRE_CORAL_FAN ||
				mat == Material.DEAD_HORN_CORAL_FAN ||
				mat == Material.TUBE_CORAL_WALL_FAN ||
				mat == Material.BRAIN_CORAL_WALL_FAN ||
				mat == Material.BUBBLE_CORAL_WALL_FAN ||
				mat == Material.FIRE_CORAL_WALL_FAN ||
				mat == Material.HORN_CORAL_WALL_FAN ||
				mat == Material.DEAD_TUBE_CORAL_WALL_FAN ||
				mat == Material.DEAD_BRAIN_CORAL_WALL_FAN ||
				mat == Material.DEAD_BUBBLE_CORAL_WALL_FAN ||
				mat == Material.DEAD_FIRE_CORAL_WALL_FAN ||
				mat == Material.DEAD_HORN_CORAL_WALL_FAN;
	}

	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}

	public static void activatePowerable(Block block) {
		if (block.getBlockData() instanceof Powerable) {
			Powerable powerable = (Powerable) block.getBlockData();
			powerable.setPowered(true);
			block.setBlockData(powerable, true);
		}

		if (block.getBlockData() instanceof AnaloguePowerable) {
			AnaloguePowerable analoguePowerable = (AnaloguePowerable) block.getBlockData();
			analoguePowerable.setPower(analoguePowerable.getMaximumPower());
			block.setBlockData(analoguePowerable, true);
		}
	}
}
