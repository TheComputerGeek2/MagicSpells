package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class EntombSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private final Set<Block> blocks;

	private final ConfigData<BlockData> blockType;
	
	private final ConfigData<Integer> duration;

	private final boolean allowBreaking;

	private final ConfigData<Boolean> centerTarget;
	private final ConfigData<Boolean> closeTopAndBottom;
	private final ConfigData<Boolean> powerAffectsDuration;

	private final String blockDestroyMessage;
	
	public EntombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockType = getConfigDataBlockData("block-type", Material.GLASS.createBlockData());

		duration = getConfigDataInt("duration", 20);

		allowBreaking = getConfigBoolean("allow-breaking", true);

		centerTarget = getConfigDataBoolean("center-target", true);
		closeTopAndBottom = getConfigDataBoolean("close-top-and-bottom", true);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);

		blockDestroyMessage = getConfigString("block-destroy-message", "");
		
		blocks = new HashSet<>();
	}
	
	@Override
	public void turnOff() {
		for (Block block : blocks) {
			block.setType(Material.AIR);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation(), SpellData.NULL);
		}
		blocks.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		List<Block> tempBlocks = new ArrayList<>();
		List<Block> tombBlocks = new ArrayList<>();

		LivingEntity target = data.target();

		if (centerTarget.get(data)) {
			Location location = target.getLocation();
			location.setX(location.getBlockX() + 0.5);
			location.setZ(location.getBlockZ() + 0.5);
			target.teleport(location);
		}

		BoundingBox box = target.getBoundingBox();
		BoundingBox exp = box.clone().expand(1 - 1e-7);

		int minX = (int) Math.floor(exp.getMinX());
		int minY = (int) Math.floor(exp.getMinY());
		int minZ = (int) Math.floor(exp.getMinZ());

		int maxX = (int) Math.ceil(exp.getMaxX());
		int maxY = (int) Math.ceil(exp.getMaxY());
		int maxZ = (int) Math.ceil(exp.getMaxZ());

		boolean closeTopAndBottom = this.closeTopAndBottom.get(data);
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					Block block = target.getWorld().getBlockAt(x, y, z);
					if (box.overlaps(BoundingBox.of(block))) continue;

					int boundary = 0;
					if (x == minX || x == maxX - 1) boundary++;
					if (y == minY || y == maxY - 1) {
						if (!closeTopAndBottom) continue;
						boundary++;
					}
					if (z == minZ || z == maxZ - 1) boundary++;
					if (boundary > 1) continue;

					tempBlocks.add(block);
				}
			}
		}

		BlockData blockType = this.blockType.get(data);
		for (Block b : tempBlocks) {
			if (!b.getType().isAir()) continue;
			tombBlocks.add(b);
			b.setBlockData(blockType);
			playSpellEffects(EffectPosition.SPECIAL, b.getLocation().add(0.5, 0.5, 0.5), data);
		}

		blocks.addAll(tombBlocks);

		int duration = this.duration.get(data);
		if (powerAffectsDuration.get(data)) duration = Math.round(duration * data.power());

		if (duration > 0 && !tombBlocks.isEmpty())
			MagicSpells.scheduleDelayedTask(() -> removeTomb(tombBlocks, data), duration);

		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void removeTomb(List<Block> entomb, SpellData data) {
		for (Block block : entomb) {
			block.setType(Material.AIR);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation().add(0.5, 0.5, 0.5), data);
		}
		
		entomb.forEach(blocks::remove);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!blocks.contains(event.getBlock())) return;
		event.setCancelled(true);
		if (allowBreaking) event.getBlock().setType(Material.AIR);
		if (!blockDestroyMessage.isEmpty()) MagicSpells.sendMessage(event.getPlayer(), blockDestroyMessage);
	}

}
