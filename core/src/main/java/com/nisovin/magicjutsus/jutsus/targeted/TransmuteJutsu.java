package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class TransmuteJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private List<Material> blockTypes;

	private Material transmuteType;
	
	public TransmuteJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		List<String> list = getConfigStringList("transmutable-types", null);
		blockTypes = new ArrayList<>();
		if (list != null && !list.isEmpty()) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null || !material.isBlock()) continue;
				blockTypes.add(material);
			}
		} else blockTypes.add(Material.IRON_BLOCK);

		String materialName = getConfigString("transmute-type", "gold_block").toUpperCase();
		transmuteType = Material.getMaterial(materialName);
		if (transmuteType == null || !transmuteType.isBlock()) {
			MagicJutsus.error("TransmuteJutsu '" + internalName + "' has an transmute-type defined!");
			transmuteType = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Block block = getTargetedBlock(caster, power);
			if (block == null) return noTarget(caster);
			
			JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, caster, block.getLocation(), power);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(caster);
			block = event.getTargetLocation().getBlock();
			
			if (!canTransmute(block)) return noTarget(caster);

			block.setType(transmuteType);
			playJutsuEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Block block = target.getBlock();
		if (canTransmute(block)) {
			block.setType(transmuteType);
			playJutsuEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
			return true;
		}

		Vector v = target.getDirection();
		block = target.clone().add(v).getBlock();
		if (canTransmute(block)) {
			block.setType(transmuteType);
			playJutsuEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Block block = target.getBlock();
		if (canTransmute(block)) {
			block.setType(transmuteType);
			playJutsuEffects(EffectPosition.TARGET, block.getLocation().add(0.5, 0.5, 0.5));
			return true;
		}
		return false;
	}
	
	private boolean canTransmute(Block block) {
		for (Material m : blockTypes) {
			if (m.equals(block.getType())) return true;
		}
		return false;
	}

}
