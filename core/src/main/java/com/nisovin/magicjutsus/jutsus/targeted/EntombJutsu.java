package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class EntombJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private Set<Block> blocks;

	private Material material;
	private String materialName;
	
	private int duration;

	private boolean allowBreaking;
	private boolean closeTopAndBottom;

	private String blockDestroyMessage;
	
	public EntombJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("block-type", "glass").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("EntombJutsu '" + internalName + "' has an invalid block defined!");
			material = null;
		}
		
		duration = getConfigInt("duration", 20);

		allowBreaking = getConfigBoolean("allow-breaking", true);
		closeTopAndBottom = getConfigBoolean("close-top-and-bottom", true);

		blockDestroyMessage = getConfigString("block-destroy-message", "");
		
		blocks = new HashSet<>();
	}
	
	@Override
	public void turnOff() {
		super.turnOff();
		
		for (Block block : blocks) {
			block.setType(Material.AIR);
			playJutsuEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
		}
		blocks.clear();
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			
			createTomb(target, power);
			sendMessages(livingEntity, target);
			playJutsuEffects(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		playJutsuEffects(caster, target);
		createTomb(target, power);
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		createTomb(target, power);
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	private void createTomb(LivingEntity target, float power) {
		List<Block> tempBlocks = new ArrayList<>();
		List<Block> tombBlocks = new ArrayList<>();
		
		Block feet = target.getLocation().getBlock();
		float pitch = target.getLocation().getPitch();
		float yaw = target.getLocation().getYaw();
		
		Location tpLoc = feet.getLocation().add(0.5, 0, 0.5);
		tpLoc.setYaw(yaw);
		tpLoc.setPitch(pitch);
		target.teleport(tpLoc);
		
		tempBlocks.add(feet.getRelative(1, 0, 0));
		tempBlocks.add(feet.getRelative(1, 1, 0));
		tempBlocks.add(feet.getRelative(-1, 0, 0));
		tempBlocks.add(feet.getRelative(-1, 1, 0));
		tempBlocks.add(feet.getRelative(0, 0, 1));
		tempBlocks.add(feet.getRelative(0, 1, 1));
		tempBlocks.add(feet.getRelative(0, 0, -1));
		tempBlocks.add(feet.getRelative(0, 1, -1));
		
		if (closeTopAndBottom) {
			tempBlocks.add(feet.getRelative(0, -1, 0));
			tempBlocks.add(feet.getRelative(0, 2, 0));
		}
		
		for (Block b : tempBlocks) {
			if (!BlockUtils.isAir(b.getType())) continue;
			tombBlocks.add(b);
			b.setType(material);
			playJutsuEffects(EffectPosition.SPECIAL, b.getLocation().add(0.5, 0.5, 0.5));
		}
		
		blocks.addAll(tombBlocks);
		
		if (duration > 0 && !tombBlocks.isEmpty()) {
			MagicJutsus.scheduleDelayedTask(() -> removeTomb(tombBlocks), Math.round(duration * power));
		}
	}
	
	private void removeTomb(List<Block> entomb) {
		for (Block block : entomb) {
			block.setType(Material.AIR);
			playJutsuEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation().add(0.5, 0.5, 0.5));
		}
		
		blocks.removeAll(entomb);
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!blocks.contains(event.getBlock())) return;
		event.setCancelled(true);
		if (allowBreaking) event.getBlock().setType(Material.AIR);
		if (!blockDestroyMessage.isEmpty()) MagicJutsus.sendMessage(event.getPlayer(), blockDestroyMessage);
	}
	
}
