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
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
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
		return switch (m) {
			case CHEST, TRAPPED_CHEST -> true;
			default -> false;
		};
	}

	public static boolean isLiquid(Block block) {
		return isLiquid(block.getType());
	}

	public static boolean isLiquid(Material m) {
		return switch (m) {
			case WATER, LAVA -> true;
			default -> false;
		};

	}

	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}

	public static boolean isAir(ItemStack item) {
		return isAir(item.getType());
	}

	public static boolean isAir(Material m) {
		return m.isAir();
	}

	public static boolean isBed(Material m) {
		return m.name().contains("_BED");
	}

	public static boolean isWoodDoor(Material m) {
		return switch (m) {
			case OAK_DOOR, ACACIA_DOOR, JUNGLE_DOOR, SPRUCE_DOOR, DARK_OAK_DOOR, BIRCH_DOOR, CRIMSON_DOOR, WARPED_DOOR -> true;
			default -> false;
		};
	}

	public static boolean isWoodButton(Material m) {
		return switch (m) {
			case OAK_BUTTON, ACACIA_BUTTON, JUNGLE_BUTTON, SPRUCE_BUTTON, DARK_OAK_BUTTON, BIRCH_BUTTON, CRIMSON_BUTTON, WARPED_BUTTON -> true;
			default -> false;
		};
	}

	public static boolean isWoodPressurePlate(Material m) {
		return switch (m) {
			case OAK_PRESSURE_PLATE, ACACIA_PRESSURE_PLATE, JUNGLE_PRESSURE_PLATE, SPRUCE_PRESSURE_PLATE, DARK_OAK_PRESSURE_PLATE, BIRCH_PRESSURE_PLATE, CRIMSON_PRESSURE_PLATE, WARPED_PRESSURE_PLATE -> true;
			default -> false;
		};
	}

	public static boolean isWoodTrapdoor(Material m) {
		return switch (m) {
			case OAK_TRAPDOOR, ACACIA_TRAPDOOR, JUNGLE_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, DARK_OAK_TRAPDOOR, CRIMSON_TRAPDOOR, WARPED_TRAPDOOR -> true;
			default -> false;
		};
	}

	public static boolean isWoodFenceGate(Material m) {
		return switch (m) {
			case OAK_FENCE_GATE, ACACIA_FENCE_GATE, JUNGLE_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, DARK_OAK_FENCE_GATE, CRIMSON_FENCE_GATE, WARPED_FENCE_GATE -> true;
			default -> false;
		};
	}

	public static boolean isShulkerBox(Material m) {
		return switch (m) {
			case BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX, GRAY_SHULKER_BOX, GREEN_SHULKER_BOX,
					LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX,
					PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX, SHULKER_BOX, SHULKER_SHELL, SHULKER_SPAWN_EGG,
					WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> true;
			default -> false;
		};
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
		return switch (mat) {
			case AIR, WATER, LAVA, TALL_GRASS, LARGE_FERN, GRASS, DEAD_BUSH, FERN, SEAGRASS, TALL_SEAGRASS, LILY_PAD,
					DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET, OXEYE_DAISY, SUNFLOWER, LILAC, PEONY, ROSE_BUSH,
					BROWN_MUSHROOM, RED_MUSHROOM, TORCH, FIRE, REDSTONE_WIRE, WHEAT, LADDER, LEVER, REDSTONE_TORCH, SNOW,
					SUGAR_CANE, VINE, SCULK_VEIN, NETHER_WART, TUBE_CORAL, BRAIN_CORAL, BUBBLE_CORAL, FIRE_CORAL, HORN_CORAL,
					DEAD_TUBE_CORAL, DEAD_BRAIN_CORAL, DEAD_BUBBLE_CORAL, DEAD_FIRE_CORAL, DEAD_HORN_CORAL -> true;
			default -> false;
		};
	}

	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}

	public static void activatePowerable(Block block) {
		if (block.getBlockData() instanceof Powerable powerable) {
			powerable.setPowered(true);
			block.setBlockData(powerable, true);
		}

		if (block.getBlockData() instanceof AnaloguePowerable powerable) {
			powerable.setPower(powerable.getMaximumPower());
			block.setBlockData(powerable, true);
		}
	}

	public static void setBlockData(Block block, BlockData oldBlockData, BlockData newBlockData) {
		BlockData clone = newBlockData.clone();

		if (oldBlockData instanceof Directional && clone instanceof Directional) {
			if (((Directional) clone).getFaces().contains(((Directional) oldBlockData).getFacing())) {
				((Directional) clone).setFacing(((Directional) oldBlockData).getFacing());
			}
		}

		if (oldBlockData instanceof Stairs && clone instanceof Stairs) {
			((Stairs) clone).setShape(((Stairs) oldBlockData).getShape());
		}

		if (oldBlockData instanceof Slab && clone instanceof Slab) {
			((Slab) clone).setType(((Slab) oldBlockData).getType());
		}

		block.setBlockData(clone);
	}
}
