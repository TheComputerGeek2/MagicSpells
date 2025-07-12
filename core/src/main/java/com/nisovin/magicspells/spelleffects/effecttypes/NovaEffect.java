package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("nova")
public class NovaEffect extends SpellEffect {

	private List<BlockData> blockDataList;

	private ConfigData<BlockData> blockData;

	private ConfigData<Double> range;

	private ConfigData<Integer> radius;
	private ConfigData<Integer> startRadius;
	private ConfigData<Integer> heightPerTick;
	private ConfigData<Integer> expandInterval;
	private ConfigData<Integer> expandingRadiusChange;

	private ConfigData<Boolean> fakeBlocks;
	private ConfigData<Boolean> circleShape;
	private ConfigData<Boolean> removePreviousBlocks;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		List<String> materialList = config.getStringList("types");
		if (!materialList.isEmpty()) {
			blockDataList = new ArrayList<>();
			for (String str : materialList) {
				BlockData data;
				try {
					data = Bukkit.createBlockData(str.toLowerCase());
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Wrong nova type defined: '" + str + "'");
					continue;
				}

				if (!data.getMaterial().isBlock()) {
					MagicSpells.error("Wrong nova type defined: '" + str + "'");
					continue;
				}

				blockDataList.add(data);
			}
		}

		blockData = ConfigDataUtil.getBlockData(config, "type", Bukkit.createBlockData(Material.FIRE));

		range = ConfigDataUtil.getDouble(config, "range", 20);

		radius = ConfigDataUtil.getInteger(config, "radius", 3);
		startRadius = ConfigDataUtil.getInteger(config, "start-radius", 0);
		heightPerTick = ConfigDataUtil.getInteger(config, "height-per-tick", 0);
		expandInterval = ConfigDataUtil.getInteger(config, "expand-interval", 5);
		expandingRadiusChange = ConfigDataUtil.getInteger(config, "expanding-radius-change", 1);

		fakeBlocks = ConfigDataUtil.getBoolean(config, "fake-blocks", true);
		circleShape = ConfigDataUtil.getBoolean(config, "circle-shape", false);
		removePreviousBlocks = ConfigDataUtil.getBoolean(config, "remove-previous-blocks", true);
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		if (blockData == null) return null;

		double range = this.range.get(data);
		range = Math.max(Math.min(range, MagicSpells.getGlobalRadius()), 1);

		Collection<Player> nearbyPlayers = location.getWorld().getNearbyPlayers(location, range, range, range);

		// Start animation
		if (circleShape.get(data)) {
			if (blockDataList != null && !blockDataList.isEmpty())
				new NovaAnimationCircle(nearbyPlayers, location.getBlock(), blockDataList, data);
			else
				new NovaAnimationCircle(nearbyPlayers, location.getBlock(), blockData, data);
		} else {
			if (blockDataList != null && !blockDataList.isEmpty())
				new NovaAnimationSquare(nearbyPlayers, location.getBlock(), blockDataList, data);
			else
				new NovaAnimationSquare(nearbyPlayers, location.getBlock(), blockData, data);
		}
		return null;
	}

	private abstract class NovaAnimation extends SpellAnimation {

		protected final SpellData data;

		protected final Collection<Player> nearby;
		protected final Block center;

		protected final List<BlockData> blockDataList;
		protected final ConfigData<BlockData> blockData;

		protected final Map<Location, BlockData> previousBlocks;
		protected final Map<Location, BlockData> currentBlocks;

		protected final boolean fakeBlocks;
		protected final boolean removePreviousBlocks;

		protected final int radius;
		protected final int startRadius;
		protected final int heightPerTick;
		protected final int expandingRadiusChange;

		public NovaAnimation(Collection<Player> nearby, Block center, ConfigData<BlockData> blockData, List<BlockData> blockDataList, SpellData data) {
			super(0, expandInterval.get(data), true, false);

			this.data = data;
			this.nearby = nearby;
			this.center = center;
			this.blockData = blockData;
			this.blockDataList = blockDataList;

			previousBlocks = new HashMap<>();
			currentBlocks = new HashMap<>();

			fakeBlocks = NovaEffect.this.fakeBlocks.get(data);
			removePreviousBlocks = NovaEffect.this.removePreviousBlocks.get(data);

			radius = NovaEffect.this.radius.get(data);
			startRadius = NovaEffect.this.startRadius.get(data);
			heightPerTick = NovaEffect.this.heightPerTick.get(data);

			int radiusChange = NovaEffect.this.expandingRadiusChange.get(data);
			if (radiusChange < 1) radiusChange = 1;
			expandingRadiusChange = radiusChange;
		}

		public void updateBlocks(Map<Location, BlockData> update) {
			if (fakeBlocks) {
				nearby.forEach(p -> p.sendMultiBlockChange(update));
				return;
			}
			update.forEach((location, data) -> location.getBlock().setBlockData(data, false));
		}

		@Override
		public void stop() {
			super.stop();

			updateBlocks(previousBlocks);
			previousBlocks.clear();
			currentBlocks.clear();
		}

	}

	private class NovaAnimationSquare extends NovaAnimation {

		public NovaAnimationSquare(Collection<Player> nearby, Block center, ConfigData<BlockData> blockData, SpellData data) {
			super(nearby, center, blockData, null, data);
		}

		public NovaAnimationSquare(Collection<Player> nearby, Block center, List<BlockData> blockDataList, SpellData data) {
			super(nearby, center, null, blockDataList, data);
		}

		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= expandingRadiusChange;

			// Remove old blocks
			if (removePreviousBlocks) {
				updateBlocks(previousBlocks);
				previousBlocks.clear();
			}

			if (tick > radius + 1) {
				stop();
				return;
			}

			if (tick > radius) {
				return;
			}

			// Set next ring
			int bx = center.getX();
			int y = center.getY();
			int bz = center.getZ();
			y += tick * heightPerTick;

			Block b;
			Block under;
			for (int x = bx - tick; x <= bx + tick; x++) {
				for (int z = bz - tick; z <= bz + tick; z++) {
					if (Math.abs(x - bx) != tick && Math.abs(z - bz) != tick) continue;

					b = center.getWorld().getBlockAt(x, y, z);
					if (b.isPassable() && !b.isLiquid()) {
						under = b.getRelative(BlockFace.DOWN);
						if (under.isPassable() && !under.isLiquid()) b = under;
					} else if (b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
						b = b.getRelative(BlockFace.UP);
					}

					if (!b.isPassable() || b.isLiquid()) continue;
					if (previousBlocks.containsKey(b.getLocation()) || currentBlocks.containsKey(b.getLocation()))
						continue;

					if (blockDataList != null && !blockDataList.isEmpty()) {
						currentBlocks.put(b.getLocation(), blockDataList.get(random.nextInt(blockDataList.size())));
					} else if (blockData != null) {
						currentBlocks.put(b.getLocation(), blockData.get(data));
					}

					previousBlocks.put(b.getLocation(), b.getBlockData());
				}
			}

			updateBlocks(currentBlocks);
			currentBlocks.clear();
		}

	}

	private class NovaAnimationCircle extends NovaAnimation {

		public NovaAnimationCircle(Collection<Player> nearby, Block center, ConfigData<BlockData> blockData, SpellData data) {
			super(nearby, center, blockData, null, data);
		}

		public NovaAnimationCircle(Collection<Player> nearby, Block center, List<BlockData> blockDataList, SpellData data) {
			super(nearby, center, null, blockDataList, data);
		}

		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= expandingRadiusChange;

			// Remove old blocks
			if (removePreviousBlocks) {
				updateBlocks(previousBlocks);
				previousBlocks.clear();
			}

			if (tick > radius + 1) {
				stop();
				return;
			}

			if (tick > radius) {
				return;
			}

			// Generate the bottom block
			Location centerLocation = center.getLocation().clone();
			centerLocation.add(0.5, tick * heightPerTick, 0.5);

			Block b;
			Block under;
			if (startRadius == 0 && tick == 0) {
				b = centerLocation.getBlock();
				if (b.isPassable() && !b.isLiquid()) {
					under = b.getRelative(BlockFace.DOWN);
					if (under.isPassable() && !under.isLiquid()) b = under;
				} else if (b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					b = b.getRelative(BlockFace.UP);
				}

				if (!b.isPassable() || b.isLiquid()) return;
				if (previousBlocks.containsKey(b.getLocation()) || currentBlocks.containsKey(b.getLocation())) return;

				if (blockDataList != null && !blockDataList.isEmpty()) {
					currentBlocks.put(b.getLocation(), blockDataList.get(random.nextInt(blockDataList.size())));
				} else if (blockData != null) {
					currentBlocks.put(b.getLocation(), blockData.get(data));
				}

				previousBlocks.put(b.getLocation(), b.getBlockData());
			}

			// Generate the circle
			Vector v;
			double angle, x, z;
			double amount = tick * 64;
			double inc = (2 * Math.PI) / amount;
			for (int i = 0; i < amount; i++) {
				angle = i * inc;

				x = tick * Math.cos(angle);
				z = tick * Math.sin(angle);

				v = new Vector(x, 0, z);

				b = centerLocation.add(v).getBlock();
				centerLocation.subtract(v);

				if (b.isPassable() && !b.isLiquid()) {
					under = b.getRelative(BlockFace.DOWN);
					if (under.isPassable() && !under.isLiquid()) b = under;
				} else if (b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					b = b.getRelative(BlockFace.UP);
				}

				if (!b.isPassable() || b.isLiquid()) continue;
				if (previousBlocks.containsKey(b.getLocation()) || currentBlocks.containsKey(b.getLocation())) continue;

				if (blockDataList != null && !blockDataList.isEmpty()) {
					currentBlocks.put(b.getLocation(), blockDataList.get(random.nextInt(blockDataList.size())));
				} else if (blockData != null) {
					currentBlocks.put(b.getLocation(), blockData.get(data));
				}

				previousBlocks.put(b.getLocation(), b.getBlockData());
			}

			updateBlocks(currentBlocks);
			currentBlocks.clear();
		}

	}

}
