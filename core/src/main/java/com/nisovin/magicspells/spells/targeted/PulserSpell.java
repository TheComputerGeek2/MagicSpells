package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.util.RayTraceResult;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PulserSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Map<Block, Pulser> pulsers;

	private final ConfigData<BlockData> blockType;

	private final int capPerPlayer;
	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> interval;
	private final ConfigData<Integer> totalPulses;

	private final ConfigData<Double> maxDistance;

	private final ConfigData<Boolean> checkFace;
	private final ConfigData<Boolean> unbreakable;
	private final ConfigData<Boolean> onlyCountOnSuccess;

	private final List<String> spellNames;
	private List<Subspell> spells;

	private final String spellOnBreakName;
	private Subspell spellOnBreak;

	private final String strAtCap;

	public PulserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockType = getConfigDataBlockData("block-type", Material.DIAMOND_BLOCK.createBlockData());

		yOffset = getConfigDataInt("y-offset", 0);
		interval = getConfigDataInt("interval", 30);
		totalPulses = getConfigDataInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistance = getConfigDataDouble("max-distance", 30);

		checkFace = getConfigDataBoolean("check-face", true);
		unbreakable = getConfigDataBoolean("unbreakable", false);
		onlyCountOnSuccess = getConfigDataBoolean("only-count-on-success", false);

		spellNames = getConfigStringList("spells", null);
		spellOnBreakName = getConfigString("spell-on-break", "");

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

		pulsers = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		String prefix = "PulserSpell '" + internalName + "' has ";

		spells = new ArrayList<>();
		if (spellNames != null && !spellNames.isEmpty()) {
			Subspell spell;
			for (String spellName : spellNames) {
				spell = initSubspell(spellName, prefix + "an invalid spell: '" + spellName + "' defined!");
				if (spell == null) continue;

				spells.add(spell);
			}
		}

		spellOnBreak = initSubspell(spellOnBreakName,
			prefix + "an invalid spell-on-break defined!",
			true);

		if (spells.isEmpty()) MagicSpells.error(prefix + "no spells defined!");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (capPerPlayer > 0 && hasReachedCap(data)) return noTarget(strAtCap, data);

		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(data);

		Block block = result.getHitBlock().getRelative(result.getHitBlockFace());

		int yOffset = this.yOffset.get(data);
		if (yOffset != 0) block = block.getRelative(0, yOffset, 0);

		BlockData blockType = this.blockType.get(data);

		if (!block.canPlace(blockType) || !block.isReplaceable()) {
			if (checkFace.get(data)) {
				Block upper = block.getRelative(BlockFace.UP);
				if (!upper.canPlace(blockType) || !upper.isReplaceable()) return noTarget(data);
				block = upper;
			} else return noTarget(data);
		}

		Location location = block.getLocation().add(0.5, 0.5, 0.5);
		location.setDirection(location.toVector().subtract(data.caster().getLocation().toVector()));

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, location);
		if (!event.callEvent()) return noTarget(event);

		event.setTargetLocation(event.getTargetLocation().toCenterLocation());
		block = event.getTargetLocation().getBlock();
		data = event.getSpellData();

		block.setBlockData(blockType);
		pulsers.put(block, new Pulser(block, blockType.getMaterial(), data));

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (capPerPlayer > 0 && data.hasCaster() && hasReachedCap(data)) return noTarget(strAtCap, data);

		Location location = data.location();
		Block block = location.getBlock();

		int yOffset = this.yOffset.get(data);
		if (yOffset != 0) block = block.getRelative(0, yOffset, 0);

		BlockData blockType = this.blockType.get(data);

		if (!block.canPlace(blockType) || !block.isReplaceable()) {
			if (checkFace.get(data)) {
				Block upper = block.getRelative(BlockFace.UP);
				if (!upper.canPlace(blockType) || !upper.isReplaceable()) return noTarget(data);
				block = upper;
			} else return noTarget(data);
		}

		float pitch = location.getPitch(), yaw = location.getYaw();
		location = block.getLocation().toCenterLocation();
		location.setPitch(pitch);
		location.setYaw(yaw);
		data = data.location(location);

		block.setBlockData(blockType);
		pulsers.put(block, new Pulser(block, blockType.getMaterial(), data));

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private boolean hasReachedCap(SpellData data) {
		int count = 0;
		for (Pulser pulser : pulsers.values()) {
			if (!Objects.equals(pulser.data.caster(), data.caster())) continue;
			if (++count >= capPerPlayer) return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (pulsers.isEmpty()) return;

		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser == null) return;

		event.setCancelled(true);
		if (!pulser.unbreakable) pulser.stop();
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockDestroyEvent event) {
		if (pulsers.isEmpty()) return;

		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser == null) return;

		event.setCancelled(true);
		if (!pulser.unbreakable) pulser.stop();
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (pulsers.isEmpty()) return;

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Pulser pulser = pulsers.get(iter.next());
			if (pulser == null) continue;

			iter.remove();
			if (!pulser.unbreakable) pulser.stop();
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (pulsers.isEmpty()) return;

		for (Block block : event.getBlocks()) {
			Pulser pulser = pulsers.get(block);
			if (pulser == null) continue;

			event.setCancelled(true);
			if (!pulser.unbreakable) pulser.stop();
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (pulsers.isEmpty()) return;

		Player player = event.getEntity();

		pulsers.values().removeIf(pulser -> {
			if (player.equals(pulser.data.caster())) {
				pulser.stop(false);
				return true;
			}

			return false;
		});
	}

	public Map<Block, Pulser> getPulsers() {
		return pulsers;
	}

	@Override
	public void turnOff() {
		for (Pulser pulser : pulsers.values()) pulser.stop(false);
		pulsers.clear();
	}

	public class Pulser implements Runnable {

		private final Block block;
		private final Material type;
		private final SpellData data;
		private final Location location;

		private final boolean unbreakable;
		private final boolean onlyCountOnSuccess;

		private final int totalPulses;

		private final double maxDistanceSq;

		private ScheduledTask task;
		private int pulseCount;

		private Pulser(Block block, Material type, SpellData data) {
			this.data = data;
			this.type = type;
			this.block = block;
			this.location = data.location();

			unbreakable = PulserSpell.this.unbreakable.get(data);
			onlyCountOnSuccess = PulserSpell.this.onlyCountOnSuccess.get(data);

			totalPulses = PulserSpell.this.totalPulses.get(data);

			double maxDistance = PulserSpell.this.maxDistance.get(data);
			maxDistanceSq = maxDistance * maxDistance;

			pulseCount = 0;

			task = MagicSpells.scheduleRepeatingTask(this, 0, interval.get(data), location);
		}

		public LivingEntity getCaster() {
			return data.caster();
		}

		@Override
		public void run() {
			if (!type.equals(block.getType()) || !block.getChunk().isLoaded()) {
				stop();
				return;
			}

			if (data.hasCaster()) {
				if (!data.caster().isValid()) {
					stop();
					return;
				}

				if (maxDistanceSq > 0 && (!data.caster().getWorld().equals(location.getWorld()) || location.distanceSquared(data.caster().getLocation()) > maxDistanceSq)) {
					stop();
					return;
				}
			}

			boolean activated = false;
			for (Subspell spell : spells) activated = spell.subcast(data).success() || activated;

			playSpellEffects(EffectPosition.DELAYED, location, data);

			if (totalPulses > 0 && (activated || !onlyCountOnSuccess) && ++pulseCount >= totalPulses)
				stop();
		}

		private void stop() {
			stop(true);
		}

		private void stop(boolean remove) {
			if (task.isCancelled()) return;

			MagicSpells.cancelTask(task);

			if (remove) pulsers.remove(block);

			block.getWorld().getChunkAtAsync(block).thenAccept(chunk -> {
				block.setType(Material.AIR);

				playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation(), data);
				if (spellOnBreak != null) spellOnBreak.subcast(data);
			});
		}

	}

}
