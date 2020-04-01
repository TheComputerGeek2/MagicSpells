package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusPlayerInteractEvent;

public class TelekinesisJutsu extends TargetedJutsu implements TargetedLocationJutsu {
	
	private boolean checkPlugins;
	
	public TelekinesisJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		losTransparentBlocks = new HashSet<>(losTransparentBlocks);
		losTransparentBlocks.remove(Material.LEVER);

		losTransparentBlocks.remove(Material.OAK_BUTTON);
		losTransparentBlocks.remove(Material.BIRCH_BUTTON);
		losTransparentBlocks.remove(Material.STONE_BUTTON);
		losTransparentBlocks.remove(Material.ACACIA_BUTTON);
		losTransparentBlocks.remove(Material.JUNGLE_BUTTON);
		losTransparentBlocks.remove(Material.SPRUCE_BUTTON);
		losTransparentBlocks.remove(Material.DARK_OAK_BUTTON);

		losTransparentBlocks.remove(Material.OAK_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.BIRCH_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.STONE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.ACACIA_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.JUNGLE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.SPRUCE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.DARK_OAK_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && caster instanceof Player) {
			Block target = getTargetedBlock(caster, power);
			if (target == null) return noTarget(caster);

			JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, caster, target.getLocation(), power);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(caster);
			
			target = event.getTargetLocation().getBlock();
			
			boolean activated = activate((Player) caster, target);
			if (!activated) return noTarget(caster);
			
			playJutsuEffects(caster, target.getLocation());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!(caster instanceof Player)) return false;
		boolean activated = activate((Player) caster, target.getBlock());
		if (activated) playJutsuEffects(caster, target);
		return activated;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
	
	private boolean checkPlugins(Player caster, Block target) {
		if (!checkPlugins) return true;
		MagicJutsusPlayerInteractEvent event = new MagicJutsusPlayerInteractEvent(caster, Action.RIGHT_CLICK_BLOCK, caster.getEquipment().getItemInMainHand(), target, BlockFace.SELF);
		EventUtil.call(event);
		return event.useInteractedBlock() != Result.DENY;
	}

	private boolean activate(Player caster, Block target) {
		Material targetType = target.getType();
		if (targetType == Material.LEVER || targetType == Material.STONE_BUTTON || BlockUtils.isWoodButton(targetType)) {
			if (!checkPlugins(caster, target)) return false;
			BlockUtils.activatePowerable(target);
			return true;
		} else if (BlockUtils.isWoodPressurePlate(targetType) || targetType == Material.STONE_PRESSURE_PLATE || targetType == Material.HEAVY_WEIGHTED_PRESSURE_PLATE || targetType == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
			if (!checkPlugins(caster, target)) return false;
			BlockUtils.activatePowerable(target);
			return true;
		}
		return false;
	}

}
