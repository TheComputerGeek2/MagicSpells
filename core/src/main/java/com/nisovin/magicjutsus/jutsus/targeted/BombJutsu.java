package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuAnimation;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class BombJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Set<Block> blocks;

	private Material material;
	private String materialName;

	private int fuse;
	private int interval;

	private Ninjutsu targetJutsu;
	private String targetJutsuName;
	
	public BombJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("block", "stone").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("BombJutsu '" + internalName + "' has an invalid block defined!");
			material = null;
		}

		fuse = getConfigInt("fuse", 100);
		interval = getConfigInt("interval", 20);

		targetJutsuName = getConfigString("jutsu", "");

		blocks = new HashSet<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		targetJutsu = new Ninjutsu(targetJutsuName);
		if (!targetJutsu.process() || !targetJutsu.isTargetedLocationJutsu()) {
			if (!targetJutsuName.isEmpty()) MagicJutsus.error("BombJutsu '" + internalName + "' has an invalid jutsu defined!");
			targetJutsu = null;
		}
	}

	@Override
	public void turnOff() {
		super.turnOff();

		for (Block b : blocks) {
			b.setType(Material.AIR);
		}

		blocks.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			List<Block> blocks = getLastTwoTargetedBlocks(livingEntity, power);
			if (blocks.size() != 2) return noTarget(livingEntity);
			if (!blocks.get(1).getType().isSolid()) return noTarget(livingEntity);

			Block target = blocks.get(0);
			boolean ok = bomb(livingEntity, target.getLocation(), power);
			if (!ok) return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return bomb(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return bomb(null, target, power);
	}

	private boolean bomb(LivingEntity livingEntity, Location loc, float power) {
		if (material == null) return false;
		Block block = loc.getBlock();
		if (!BlockUtils.isAir(block.getType())) return false;

		blocks.add(block);
		block.setType(material);
		if (livingEntity != null) playJutsuEffects(livingEntity, loc.add(0.5, 0, 0.5));
		else playJutsuEffects(EffectPosition.TARGET, loc.add(0.5, 0, 0.5));

		new JutsuAnimation(interval, interval, true) {
				
			int time = 0;
			Location l = block.getLocation().add(0.5, 0, 0.5);
			@Override
			protected void onTick(int tick) {
				time += interval;
				if (time >= fuse) {
					stop();
					if (material.equals(block.getType())) {
						blocks.remove(block);
						block.setType(Material.AIR);
						playJutsuEffects(EffectPosition.DELAYED, l);
						if (targetJutsu != null) targetJutsu.castAtLocation(livingEntity, l, power);
					}
				} else if (!material.equals(block.getType())) stop();
				else playJutsuEffects(EffectPosition.SPECIAL, l);
			}
				
		};

		return true;
	}

}
