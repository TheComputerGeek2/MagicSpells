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
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class EntombSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private Set<Block> blocks;

	private Material material;
	private String materialName;
	
	private ConfigData<Integer> duration;

	private boolean allowBreaking;
	private boolean closeTopAndBottom;

	private String blockDestroyMessage;
	
	public EntombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("block-type", "glass");
		material = Util.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("EntombSpell '" + internalName + "' has an invalid block defined!");
			material = null;
		}
		
		duration = getConfigDataInt("duration", 20);

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
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
		}
		blocks.clear();
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power, args);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			
			createTomb(caster, target, power, args);
			sendMessages(caster, target, args);
			playSpellEffects(caster, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		playSpellEffects(caster, target);
		createTomb(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		createTomb(null, target, power, args);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void createTomb(LivingEntity caster, LivingEntity target, float power, String[] args) {
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
			playSpellEffects(EffectPosition.SPECIAL, b.getLocation().add(0.5, 0.5, 0.5));
		}
		
		blocks.addAll(tombBlocks);

		int duration = this.duration.get(caster, target, power, args);
		if (duration > 0 && !tombBlocks.isEmpty()) {
			MagicSpells.scheduleDelayedTask(() -> removeTomb(tombBlocks), Math.round(duration * power));
		}
	}
	
	private void removeTomb(List<Block> entomb) {
		for (Block block : entomb) {
			block.setType(Material.AIR);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation().add(0.5, 0.5, 0.5));
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
