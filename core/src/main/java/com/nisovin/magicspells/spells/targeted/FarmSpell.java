package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class FarmSpell extends TargetedSpell implements TargetedLocationSpell {

	private final ConfigData<BlockData> cropType;

	private final ConfigData<Integer> radius;
	private final ConfigData<Integer> growth;

	private final ConfigData<Boolean> targeted;
	private final ConfigData<Boolean> growWart;
	private final ConfigData<Boolean> growWheat;
	private final ConfigData<Boolean> growCarrots;
	private final ConfigData<Boolean> growPotatoes;
	private final ConfigData<Boolean> growBeetroot;
	private final ConfigData<Boolean> powerAffectsRadius;
	private final ConfigData<Boolean> resolveGrowthPerCrop;
	private final ConfigData<Boolean> resolveCropTypePerCrop;

	public FarmSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		cropType = getConfigDataBlockData("crop-type", Material.WHEAT.createBlockData());

		radius = getConfigDataInt("radius", 3);
		growth = getConfigDataInt("growth", 1);

		targeted = getConfigDataBoolean("targeted", false);
		growWart = getConfigDataBoolean("grow-wart", false);
		growWheat = getConfigDataBoolean("grow-wheat", true);
		growCarrots = getConfigDataBoolean("grow-carrots", true);
		growPotatoes = getConfigDataBoolean("grow-potatoes", true);
		growBeetroot = getConfigDataBoolean("grow-beetroot", false);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", true);
		resolveGrowthPerCrop = getConfigDataBoolean("resolve-growth-per-crop", false);
		resolveCropTypePerCrop = getConfigDataBoolean("resolve-crop-type-per-crop", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (targeted.get(data)) {
			TargetInfo<Location> info = getTargetedBlockLocation(data);
			if (!info.noTarget()) return noTarget(info);
			data = info.spellData();
		} else {
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!event.callEvent()) return noTarget(event);
			data = event.getSpellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Block center = data.location().getBlock();

		int radius = this.radius.get(data);
		if (powerAffectsRadius.get(data)) radius = Math.round(radius * data.power());

		int cx = center.getX();
		int y = center.getY();
		int cz = center.getZ();

		boolean growWart = this.growWart.get(data);
		boolean growWheat = this.growWheat.get(data);
		boolean growCarrots = this.growCarrots.get(data);
		boolean growPotatoes = this.growPotatoes.get(data);
		boolean growBeetroot = this.growBeetroot.get(data);

		boolean resolveGrowthPerCrop = this.resolveGrowthPerCrop.get(data);
		int growth = resolveGrowthPerCrop ? 0 : this.growth.get(data);

		boolean resolveCropTypePerCrop = this.resolveCropTypePerCrop.get(data);
		BlockData cropType = resolveCropTypePerCrop ? null : this.cropType.get(data);

		int count = 0;
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				Block b = center.getWorld().getBlockAt(x, y, z);
				if (b.getType() != Material.FARMLAND && b.getType() != Material.SOUL_SAND) {
					b = b.getRelative(BlockFace.DOWN);
					if (b.getType() != Material.FARMLAND && b.getType() != Material.SOUL_SAND) continue;
				}
				b = b.getRelative(BlockFace.UP);

				if (b.getType().isAir()) {
					if (resolveCropTypePerCrop) cropType = this.cropType.get(data);

					if (cropType instanceof Ageable ageable) {
						if (resolveGrowthPerCrop) growth = this.growth.get(data);
						if (growth > 1) ageable.setAge(Math.clamp(growth - 1, 0, ageable.getMaximumAge()));

						b.setBlockData(ageable);
						count++;
					}

					continue;
				}

				if (!(b.getBlockData() instanceof Ageable ageable)) continue;

				switch (b.getType()) {
					case NETHER_WART -> {
						if (!growWart) continue;
					}
					case WHEAT -> {
						if (!growWheat) continue;
					}
					case CARROTS -> {
						if (!growCarrots) continue;
					}
					case POTATOES -> {
						if (!growPotatoes) continue;
					}
					case BEETROOTS -> {
						if (!growBeetroot) continue;
					}
					default -> {
						continue;
					}
				}

				if (ageable.getAge() == ageable.getMaximumAge()) continue;

				if (resolveGrowthPerCrop) growth = this.growth.get(data);
				ageable.setAge(Math.clamp(ageable.getAge() + growth, 0, ageable.getMaximumAge()));
				b.setBlockData(ageable);
				count++;
			}
		}
		if (count == 0) return noTarget(data);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
