package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class FarmJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Material cropType;
	private String materialName;

	private int radius;
	private int growth;

	private boolean targeted;
	private boolean growWart;
	private boolean growWheat;
	private boolean growCarrots;
	private boolean growPotatoes;
	private boolean growBeetroot;

	public FarmJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("crop-type", "wheat").toUpperCase();
		cropType = Material.getMaterial(materialName);
		if (cropType == null) MagicJutsus.error("FarmJutsu '" + internalName + "' has an invalid crop-type defined!");

		radius = getConfigInt("radius", 3);
		growth = getConfigInt("growth", 1);

		targeted = getConfigBoolean("targeted", false);
		growWart = getConfigBoolean("grow-wart", false);
		growWheat = getConfigBoolean("grow-wheat", true);
		growCarrots = getConfigBoolean("grow-carrots", true);
		growPotatoes = getConfigBoolean("grow-potatoes", true);
		growBeetroot = getConfigBoolean("grow-beetroot", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Block block;
			if (targeted) block = getTargetedBlock(livingEntity, power);
			else block = livingEntity.getLocation().subtract(0, 1, 0).getBlock();

			if (block != null) {
				JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block != null) {
				boolean farmed = farm(block, Math.round(radius * power));
				if (!farmed) return noTarget(livingEntity);
				playJutsuEffects(EffectPosition.CASTER, livingEntity);
				if (targeted) playJutsuEffects(EffectPosition.TARGET, block.getLocation());
			} else return noTarget(livingEntity);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return farm(target.subtract(0, 1, 0).getBlock(), Math.round(radius * power));
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return farm(target.getBlock(), Math.round(radius * power));
	}
	
	private boolean farm(Block center, int radius) {
		int cx = center.getX();
		int y = center.getY();
		int cz = center.getZ();

		int count = 0;
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				Block b = center.getWorld().getBlockAt(x, y, z);
				if (b.getType() != Material.FARMLAND && b.getType() != Material.SOUL_SAND) {
					b = b.getRelative(BlockFace.DOWN);
					if (b.getType() != Material.FARMLAND && b.getType() != Material.SOUL_SAND) continue;
				}

				b = b.getRelative(BlockFace.UP);
				if (BlockUtils.isAir(b.getType())) {
					if (cropType != null) {
						b.setType(cropType);
						if (growth > 1) BlockUtils.setGrowthLevel(b, growth - 1);
						count++;
					}
				} else if ((isWheat(b) || isCarrot(b) || isPotato(b)) && BlockUtils.getGrowthLevel(b) < 7) {
					int newGrowth = BlockUtils.getGrowthLevel(b) + growth;
					if (newGrowth > 7) newGrowth = 7;
					BlockUtils.setGrowthLevel(b, newGrowth);
					count++;
				} else if ((isBeetroot(b) || isWart(b)) && BlockUtils.getGrowthLevel(b) < 3) {
					int newGrowth = BlockUtils.getGrowthLevel(b) + growth;
					if (newGrowth > 3) newGrowth = 3;
					BlockUtils.setGrowthLevel(b, newGrowth);
					count++;
				}
			}
		}

		return count > 0;
	}

	private boolean isWheat(Block b) {
		return growWheat && b.getType() == Material.WHEAT;
	}

	private boolean isBeetroot(Block b) {
		return growBeetroot && b.getType() == Material.BEETROOTS;
	}

	private boolean isCarrot(Block b) {
		return growCarrots && b.getType() == Material.CARROTS;
	}

	private boolean isPotato(Block b) {
		return growPotatoes && b.getType() == Material.POTATOES;
	}

	private boolean isWart(Block b) {
		return growWart && b.getType() == Material.NETHER_WART;
	}

}
