package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class FarmSpell extends TargetedSpell implements TargetedLocationSpell {

	private Material cropType;
	private String materialName;

	private ConfigData<Integer> radius;
	private ConfigData<Integer> growth;

	private boolean targeted;
	private boolean growWart;
	private boolean growWheat;
	private boolean growCarrots;
	private boolean growPotatoes;
	private boolean growBeetroot;
	private boolean powerAffectsRadius;
	private boolean resolveGrowthPerCrop;

	public FarmSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("crop-type", "wheat");
		cropType = Util.getMaterial(materialName);
		if (cropType == null) MagicSpells.error("FarmSpell '" + internalName + "' has an invalid crop-type defined!");

		radius = getConfigDataInt("radius", 3);
		growth = getConfigDataInt("growth", 1);

		targeted = getConfigBoolean("targeted", false);
		growWart = getConfigBoolean("grow-wart", false);
		growWheat = getConfigBoolean("grow-wheat", true);
		growCarrots = getConfigBoolean("grow-carrots", true);
		growPotatoes = getConfigBoolean("grow-potatoes", true);
		growBeetroot = getConfigBoolean("grow-beetroot", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		resolveGrowthPerCrop = getConfigBoolean("resolve-growth-per-crop", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block;
			if (targeted) block = getTargetedBlock(caster, power);
			else block = caster.getLocation().subtract(0, 1, 0).getBlock();

			if (block != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block != null) {
				boolean farmed = farm(caster, block, power, args);
				if (!farmed) return noTarget(caster);
				playSpellEffects(EffectPosition.CASTER, caster);
				if (targeted) playSpellEffects(EffectPosition.TARGET, block.getLocation());
			} else return noTarget(caster);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return farm(caster, target.subtract(0, 1, 0).getBlock(), power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return farm(caster, target.subtract(0, 1, 0).getBlock(), power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return farm(null, target.getBlock(), power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return farm(null, target.getBlock(), power, null);
	}

	private boolean farm(LivingEntity caster, Block center, float power, String[] args) {
		int radius = this.radius.get(caster, null, power, args);
		if (powerAffectsRadius) radius = Math.round(radius * power);

		int cx = center.getX();
		int y = center.getY();
		int cz = center.getZ();

		int growth = resolveGrowthPerCrop ? 0 : this.growth.get(caster, null, power, args);

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
						if (resolveGrowthPerCrop) growth = this.growth.get(caster, null, power, args);

						b.setType(cropType);
						if (growth > 1) BlockUtils.setGrowthLevel(b, growth - 1);
						count++;
					}
				} else if ((isWheat(b) || isCarrot(b) || isPotato(b)) && BlockUtils.getGrowthLevel(b) < 7) {
					if (resolveGrowthPerCrop) growth = this.growth.get(caster, null, power, args);

					int newGrowth = BlockUtils.getGrowthLevel(b) + growth;
					if (newGrowth > 7) newGrowth = 7;
					BlockUtils.setGrowthLevel(b, newGrowth);
					count++;
				} else if ((isBeetroot(b) || isWart(b)) && BlockUtils.getGrowthLevel(b) < 3) {
					if (resolveGrowthPerCrop) growth = this.growth.get(caster, null, power, args);

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
