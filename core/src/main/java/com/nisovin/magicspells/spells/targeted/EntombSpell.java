package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
	private final ConfigData<Boolean> closeTopAndBottom;
	private final ConfigData<Boolean> powerAffectsDuration;

	private final String blockDestroyMessage;
	
	public EntombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockType = getConfigDataBlockData("block-type", Material.GLASS.createBlockData());

		duration = getConfigDataInt("duration", 20);

		allowBreaking = getConfigBoolean("allow-breaking", true);
		closeTopAndBottom = getConfigDataBoolean("close-top-and-bottom", true);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);

		blockDestroyMessage = getConfigString("block-destroy-message", "");
		
		blocks = new HashSet<>();
	}
	
	@Override
	public void turnOff() {
		super.turnOff();

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
		Block feet = target.getLocation().getBlock();
		float pitch = target.getLocation().getPitch();
		float yaw = target.getLocation().getYaw();

		Location tpLoc = feet.getLocation().add(0.5, 0, 0.5);
		tpLoc.setYaw(yaw);
		tpLoc.setPitch(pitch);
		target.teleportAsync(tpLoc);

		tempBlocks.add(feet.getRelative(1, 0, 0));
		tempBlocks.add(feet.getRelative(1, 1, 0));
		tempBlocks.add(feet.getRelative(-1, 0, 0));
		tempBlocks.add(feet.getRelative(-1, 1, 0));
		tempBlocks.add(feet.getRelative(0, 0, 1));
		tempBlocks.add(feet.getRelative(0, 1, 1));
		tempBlocks.add(feet.getRelative(0, 0, -1));
		tempBlocks.add(feet.getRelative(0, 1, -1));

		if (closeTopAndBottom.get(data)) {
			tempBlocks.add(feet.getRelative(0, -1, 0));
			tempBlocks.add(feet.getRelative(0, 2, 0));
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
			MagicSpells.scheduleDelayedTask(() -> removeTomb(tombBlocks, data), duration, feet.getLocation());

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
