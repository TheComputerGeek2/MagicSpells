package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.AnaloguePowerable;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;

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

	public static int getMaxGrowthLevel(Block block) {
		return ((Ageable) block.getBlockData()).getMaximumAge();
	}

	public static boolean isPlant(Block b) {
		return b.getBlockData() instanceof Ageable;
	}

	public static void setGrowthLevel(Block block, int level) {
		Ageable age = ((Ageable) block.getBlockData());
		age.setAge(level);
		block.setBlockData(age);
	}

	public static int getWaterLevel(BlockState blockState) {
		return ((Levelled) blockState.getBlockData()).getLevel();
	}

	public static boolean isChest(Block block) {
		return isChest(block.getType());
	}

	public static boolean isChest(Material m) {
		switch (m) {
			case CHEST:
			case TRAPPED_CHEST:
				return true;
		}
		return false;
	}

	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}

	public static boolean isAir(ItemStack item) {
		return isAir(item.getType());
	}

	public static boolean isAir(Material m) {
		return m == Material.AIR || m.name().contains("_AIR");
	}

	public static boolean isBed(Material m) {
		return m.name().contains("_BED");
	}

	public static boolean isWoodDoor(Material m) {
		switch (m) {
			case OAK_DOOR:
			case ACACIA_DOOR:
			case JUNGLE_DOOR:
			case SPRUCE_DOOR:
			case DARK_OAK_DOOR:
			case BIRCH_DOOR:
				return true;
		}
		return false;
	}

	public static boolean isWoodButton(Material m) {
		switch (m) {
			case OAK_BUTTON:
			case ACACIA_BUTTON:
			case JUNGLE_BUTTON:
			case SPRUCE_BUTTON:
			case DARK_OAK_BUTTON:
			case BIRCH_BUTTON:
				return true;
		}
		return false;
	}

	public static boolean isWoodPressurePlate(Material m) {
		switch (m) {
			case OAK_PRESSURE_PLATE:
			case ACACIA_PRESSURE_PLATE:
			case JUNGLE_PRESSURE_PLATE:
			case SPRUCE_PRESSURE_PLATE:
			case DARK_OAK_PRESSURE_PLATE:
			case BIRCH_PRESSURE_PLATE:
				return true;
		}
		return false;
	}

	public static boolean isWoodTrapdoor(Material m) {
		switch (m) {
			case OAK_TRAPDOOR:
			case ACACIA_TRAPDOOR:
			case JUNGLE_TRAPDOOR:
			case SPRUCE_TRAPDOOR:
			case BIRCH_TRAPDOOR:
			case DARK_OAK_TRAPDOOR:
				return true;
		}
		return false;
	}

	public static boolean isWood(Material m) {
		return m.name().contains("_WOOD");
	}

	public static boolean isLog(Material m) {
		return m.name().contains("_LOG");
	}

	public static boolean isPathable(Material mat) {
		String name = mat.name();
		if (name.contains("SIGN")) return true;
		if (name.contains("CARPET")) return true;
		if (name.contains("PRESSURE_PLATE")) return true;
		if (name.contains("BUTTON")) return true;
		if (name.contains("TULIP")) return true;
		if (name.contains("SAPLING")) return true;
		if (name.contains("FAN")) return true;
		if (name.contains("RAIL")) return true;
		if (name.contains("_AIR")) return true;
		switch (mat) {
			case AIR:
			case WATER:
			case TALL_GRASS:
			case LARGE_FERN:
			case GRASS:
			case DEAD_BUSH:
			case FERN:
			case SEAGRASS:
			case TALL_SEAGRASS:
			case LILY_PAD:
			case DANDELION:
			case POPPY:
			case BLUE_ORCHID:
			case ALLIUM:
			case AZURE_BLUET:
			case OXEYE_DAISY:
			case SUNFLOWER:
			case LILAC:
			case PEONY:
			case ROSE_BUSH:
			case BROWN_MUSHROOM:
			case RED_MUSHROOM:
			case TORCH:
			case FIRE:
			case REDSTONE_WIRE:
			case WHEAT:
			case LADDER:
			case LEVER:
			case REDSTONE_TORCH:
			case SNOW:
			case SUGAR_CANE:
			case VINE:
			case NETHER_WART:
			case TUBE_CORAL:
			case BRAIN_CORAL:
			case BUBBLE_CORAL:
			case FIRE_CORAL:
			case HORN_CORAL:
			case DEAD_TUBE_CORAL:
			case DEAD_BRAIN_CORAL:
			case DEAD_BUBBLE_CORAL:
			case DEAD_FIRE_CORAL:
			case DEAD_HORN_CORAL:
				return true;
		}
		return false;
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
