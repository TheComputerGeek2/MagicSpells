package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.AnaloguePowerable;

import org.jetbrains.annotations.NotNull;

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

	public static boolean isChest(Block block) {
		return switch (block.getType()) {
			case CHEST, TRAPPED_CHEST -> true;
			default -> false;
		};
	}

	public static boolean isPathable(Material mat) {
		return switch (mat) {
			case LIGHT, SNOW -> true;
			default -> mat.isBlock() && !mat.isCollidable();
		};
	}

	public static Location adjustToSafeLocation(@NotNull Entity entity, @NotNull Location location) {
		return adjustToSafeLocation(entity, location, 0, false);
	}

	public static Location adjustToSafeLocation(@NotNull Entity entity, @NotNull Location location, int requireGroundDistance, boolean snapToGround) {
		Location adjusted = location.clone();

		// Check if the entity collides because the block's collision box.
		if (entity.collidesAt(adjusted)) {
			if (adjusted.getBlock().isPassable()) {
				adjusted.subtract(0, 1, 0);
				if (adjusted.getBlock().isPassable()) return null;
			}

			adjusted = getYMaxCollision(adjusted);

			if (entity.collidesAt(adjusted)) return null;
		}

		if (requireGroundDistance < 1 || !adjusted.getBlock().isPassable()) return adjusted;

		Location ground = adjusted.clone();
		for (int i = 1; i <= requireGroundDistance; i++) {
			ground.subtract(0, 1, 0);
			if (ground.getBlock().isPassable()) continue;

			return snapToGround ? getYMaxCollision(ground) : adjusted;
		}

		return null;
	}

	private static Location getYMaxCollision(@NotNull Location location) {
		// Note: getBoundingBoxes[i] is [0.0, x]
		double maxCollisionY = location.getBlock()
			.getCollisionShape()
			.getBoundingBoxes()
			.stream()
			.mapToDouble(BoundingBox::getMaxY)
			.max()
			.orElse(0);

		return location.clone().add(0, maxCollisionY, 0);
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

}
