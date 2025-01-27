package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class NovaSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private final ConfigData<BlockData> blockData;

	private final ConfigData<Vector> relativeOffset;

	private final ConfigData<Integer> radius;
	private final ConfigData<Integer> startRadius;
	private final ConfigData<Integer> heightPerTick;
	private final ConfigData<Integer> expandInterval;
	private final ConfigData<Integer> expandingRadiusChange;

	private final ConfigData<Double> visibleRange;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> circleShape;
	private final ConfigData<Boolean> removePreviousBlocks;

	private Subspell spellOnEnd;
	private Subspell locationSpell;
	private Subspell spellOnWaveRemove;

	private final String spellOnEndName;
	private final String locationSpellName;
	private final String spellOnWaveRemoveName;

	public NovaSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockData = getConfigDataBlockData("type", Bukkit.createBlockData(Material.WATER));

		relativeOffset = getConfigDataVector("relative-offset", new Vector());

		locationSpellName = getConfigString("spell", "");
		spellOnEndName = getConfigString("spell-on-end", "");
		spellOnWaveRemoveName = getConfigString("spell-on-wave-remove", "");

		radius = getConfigDataInt("radius", 3);
		startRadius = getConfigDataInt("start-radius", 0);
		heightPerTick = getConfigDataInt("height-per-tick", 0);
		expandInterval = getConfigDataInt("expand-interval", 5);
		expandingRadiusChange = getConfigDataInt("expanding-radius-change", 1);

		visibleRange = getConfigDataDouble("visible-range", 20);

		pointBlank = getConfigDataBoolean("point-blank", true);
		circleShape = getConfigDataBoolean("circle-shape", false);
		removePreviousBlocks = getConfigDataBoolean("remove-previous-blocks", true);

	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "NovaSpell " + internalName + " has an invalid '%s' defined!";
		locationSpell = initSubspell(locationSpellName,
				error.formatted("spell"),
				true);
		spellOnWaveRemove = initSubspell(spellOnWaveRemoveName,
				error.formatted("spell-on-wave-remove"),
				true);
		spellOnEnd = initSubspell(spellOnEndName,
				error.formatted("spell-on-end"),
				true);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (pointBlank.get(data)) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (circleShape.get(data)) new NovaTrackerCircle(data);
		else new NovaTrackerSquare(data);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		data = data.location(data.target().getLocation());

		if (circleShape.get(data)) new NovaTrackerCircle(data);
		else new NovaTrackerSquare(data);

		playSpellEffects(data.caster(), data.target(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private abstract class NovaTracker implements Runnable {

		protected final SpellData data;

		protected final Location center;

		protected final Set<Block> blocks;
		protected final Set<Player> nearby;

		protected final boolean removePreviousBlocks;

		protected final ScheduledTask task;
		protected final int radius;
		protected final int startRadius;
		protected final int heightPerTick;
		protected final int expandingRadiusChange;

		protected final double visibleRange;

		protected final BlockData blockData;

		protected int currentRadius;
		protected int count;

		public NovaTracker(SpellData data) {
			center = data.location();

			Vector relativeOffset = NovaSpell.this.relativeOffset.get(data);
			center.add(0, relativeOffset.getY(), 0);
			Util.applyRelativeOffset(center, relativeOffset.setY(0));

			data = data.location(center);

			removePreviousBlocks = NovaSpell.this.removePreviousBlocks.get(data);

			radius = NovaSpell.this.radius.get(data);
			startRadius = NovaSpell.this.startRadius.get(data);
			heightPerTick = NovaSpell.this.heightPerTick.get(data);
			expandingRadiusChange = NovaSpell.this.expandingRadiusChange.get(data);

			visibleRange = NovaSpell.this.visibleRange.get(data);

			blockData = NovaSpell.this.blockData.get(data);

			blocks = new HashSet<>();
			nearby = new HashSet<>();

			this.data = data.noTarget();
			task = MagicSpells.scheduleRepeatingTask(this, 0, expandInterval.get(data), center);
		}

		protected boolean step() {
			currentRadius = (startRadius + count) * expandingRadiusChange;
			count++;

			if (removePreviousBlocks) removeBlocks(spellOnWaveRemove);

			if (currentRadius > radius + 1) {
				stop();
				return true;
			}

			if (currentRadius <= radius) {
				nearby.addAll(center.getNearbyPlayers(visibleRange));
				return false;
			}

			return true;
		}

		protected void removeBlocks(Subspell spell) {
			Map<Location, BlockData> update = new HashMap<>();
			for (Block block : blocks) {
				update.put(block.getLocation(), block.getBlockData());

				if (spell != null) {
					SpellData subData = data.location(block.getLocation().add(0.5, 0, 0.5));
					spell.subcast(subData);
				}
			}
			for (Player p : nearby) p.sendMultiBlockChange(update);

			blocks.clear();
			nearby.clear();
		}

		protected void checkBlock(Block block, Map<Location, BlockData> update) {
			if (block.isPassable()) {
				Block under = block.getRelative(BlockFace.DOWN);
				if (under.isPassable()) block = under;
			} else {
				Block upper = block.getRelative(BlockFace.UP);
				if (upper.isPassable()) block = upper;
			}
			if (!block.isPassable() || blocks.contains(block)) return;

			update.put(block.getLocation(), blockData);
			blocks.add(block);

			if (locationSpell != null) {
				SpellData subData = data.location(block.getLocation().add(0.5, 0, 0.5));
				locationSpell.subcast(subData);
			}
		}

		protected void stop() {
			removeBlocks(spellOnEnd);
			MagicSpells.cancelTask(task);
		}

	}

	private class NovaTrackerSquare extends NovaTracker {

		public NovaTrackerSquare(SpellData data) {
			super(data);
		}

		@Override
		public void run() {
			if (step()) return;

			int bx = center.getBlockX();
			int y = center.getBlockY() + count * heightPerTick;
			int bz = center.getBlockZ();

			Map<Location, BlockData> update = new HashMap<>();
			for (int x = bx - currentRadius; x <= bx + currentRadius; x++) {
				for (int z = bz - currentRadius; z <= bz + currentRadius; z++) {
					if (Math.abs(x - bx) != currentRadius && Math.abs(z - bz) != currentRadius) continue;
					checkBlock(center.getWorld().getBlockAt(x, y, z), update);
				}
			}

			for (Player p : nearby) p.sendMultiBlockChange(update);
		}

	}

	private class NovaTrackerCircle extends NovaTracker {

		public NovaTrackerCircle(SpellData data) {
			super(data);
		}

		@Override
		public void run() {
			if (step()) return;

			double cx = center.getBlockX() + 0.5;
			int y = center.getBlockY() + count * heightPerTick;
			double cz = center.getBlockZ() + 0.5;

			Map<Location, BlockData> update = new HashMap<>();

			if (startRadius == 0 && currentRadius == 0) {
				checkBlock(center.getWorld().getBlockAt(Location.locToBlock(cx), y, Location.locToBlock(cz)), update);
				for (Player p : nearby) p.sendMultiBlockChange(update);
				return;
			}

			int amount = currentRadius * 64;
			double inc = 2 * Math.PI / amount;
			for (int i = 0; i < amount; i++) {
				double angle = i * inc;
				int x = Location.locToBlock(cx + currentRadius * Math.cos(angle));
				int z = Location.locToBlock(cz + currentRadius * Math.sin(angle));

				checkBlock(center.getWorld().getBlockAt(x, y, z), update);
			}

			for (Player p : nearby) p.sendMultiBlockChange(update);
		}

	}

}
