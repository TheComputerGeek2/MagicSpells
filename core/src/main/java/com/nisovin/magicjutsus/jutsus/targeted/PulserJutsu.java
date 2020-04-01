package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.LocationUtil;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class PulserJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Map<Block, Pulser> pulsers;
	private Material material;
	private String materialName;

	private int yOffset;
	private int interval;
	private int totalPulses;
	private int capPerPlayer;

	private double maxDistanceSquared;

	private boolean checkFace;
	private boolean unbreakable;
	private boolean onlyCountOnSuccess;

	private List<String> jutsuNames;
	private List<TargetedLocationJutsu> jutsus;

	private String jutsuNameOnBreak;
	private TargetedLocationJutsu jutsuOnBreak;

	private String strAtCap;

	private PulserTicker ticker;

	public PulserJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("block-type", "DIAMOND_BLOCK").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("PulserJutsu '" + internalName + "' has an invalid block-type defined");
			material = null;
		}

		yOffset = getConfigInt("y-offset", 0);
		interval = getConfigInt("interval", 30);
		totalPulses = getConfigInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistanceSquared = getConfigDouble("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;

		checkFace = getConfigBoolean("check-face", true);
		unbreakable = getConfigBoolean("unbreakable", false);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);

		jutsuNames = getConfigStringList("jutsus", null);
		jutsuNameOnBreak = getConfigString("jutsu-on-break", "");

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

		pulsers = new HashMap<>();
		ticker = new PulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();

		jutsus = new ArrayList<>();
		if (jutsuNames != null && !jutsuNames.isEmpty()) {
			for (String jutsuName : jutsuNames) {
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(jutsuName);
				if (!(jutsu instanceof TargetedLocationJutsu)) continue;
				jutsus.add((TargetedLocationJutsu) jutsu);
			}
		}

		if (!jutsuNameOnBreak.isEmpty()) {
			Jutsu jutsu = MagicJutsus.getJutsuByInternalName(jutsuNameOnBreak);
			if (jutsu instanceof TargetedLocationJutsu) jutsuOnBreak = (TargetedLocationJutsu) jutsu;
			else MagicJutsus.error("PulserJutsu '" + internalName + "' has an invalid jutsu-on-break defined");
		}

		if (jutsus.isEmpty()) MagicJutsus.error("PulserJutsu '" + internalName + "' has no jutsus defined!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Pulser pulser : pulsers.values()) {
					if (!pulser.caster.equals(livingEntity)) continue;
					
					count++;
					if (count >= capPerPlayer) {
						sendMessage(strAtCap, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			List<Block> lastTwo = getLastTwoTargetedBlocks(livingEntity, power);
			Block target = null;

			if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
			if (target == null) return noTarget(livingEntity);
			if (yOffset > 0) target = target.getRelative(BlockFace.UP, yOffset);
			else if (yOffset < 0) target = target.getRelative(BlockFace.DOWN, yOffset);
			if (!BlockUtils.isPathable(target)) return noTarget(livingEntity);

			if (target != null) {
				JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(livingEntity);
				target = event.getTargetLocation().getBlock();
				power = event.getPower();
			}
			createPulser(livingEntity, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Block block = target.getBlock();
		if (yOffset > 0) block = block.getRelative(BlockFace.UP, yOffset);
		else if (yOffset < 0) block = block.getRelative(BlockFace.DOWN, yOffset);

		if (BlockUtils.isPathable(block)) {
			createPulser(caster, block, power);
			return true;
		}

		if (checkFace) {
			block = block.getRelative(BlockFace.UP);
			if (BlockUtils.isPathable(block)) {
				createPulser(caster, block, power);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

	private void createPulser(LivingEntity caster, Block block, float power) {
		if (material == null) return;
		block.setType(material);
		pulsers.put(block, new Pulser(caster, block, power));
		ticker.start();
		if (caster != null) playJutsuEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
		else playJutsuEffects(EffectPosition.TARGET, block.getLocation().add(0.5, 0.5, 0.5));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser == null) return;
		event.setCancelled(true);
		if (unbreakable) return;
		pulser.stop();
		event.getBlock().setType(Material.AIR);
		pulsers.remove(event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (pulsers.isEmpty()) return;
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block b = iter.next();
			Pulser pulser = pulsers.get(b);
			if (pulser == null) continue;
			iter.remove();

			if (unbreakable) continue;
			pulser.stop();
			pulsers.remove(b);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (pulsers.isEmpty()) return;
		for (Block b : event.getBlocks()) {
			Pulser pulser = pulsers.get(b);
			if (pulser == null) continue;
			event.setCancelled(true);
			if (unbreakable) continue;
			pulser.stop();
			pulsers.remove(b);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (pulsers.isEmpty()) return;
		Player player = event.getEntity();
		Iterator<Pulser> iter = pulsers.values().iterator();
		while (iter.hasNext()) {
			Pulser pulser = iter.next();
			if (pulser.caster == null) continue;
			if (!pulser.caster.equals(player)) continue;
			pulser.stop();
			iter.remove();
		}
	}

	@Override
	public void turnOff() {
		for (Pulser p : new ArrayList<>(pulsers.values())) {
			p.stop();
		}
		pulsers.clear();
		ticker.stop();
	}
	
	private class Pulser {

		private LivingEntity caster;
		private Block block;
		private Location location;
		private float power;
		private int pulseCount;
		
		private Pulser(LivingEntity caster, Block block, float power) {
			this.caster = caster;
			this.block = block;
			this.location = block.getLocation().add(0.5, 0.5, 0.5);
			this.power = power;
			this.pulseCount = 0;
		}

		private boolean pulse() {
			if (caster == null) {
				if (material.equals(block.getType()) && block.getChunk().isLoaded()) return activate();
				stop();
				return true;
			} else if (caster.isValid() && material.equals(block.getType()) && block.getChunk().isLoaded()) {
				if (maxDistanceSquared > 0 && (!LocationUtil.isSameWorld(location, caster) || location.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
					stop();
					return true;
				}
				return activate();
			}
			stop();
			return true;
		}
		
		private boolean activate() {
			boolean activated = false;
			for (TargetedLocationJutsu jutsu : jutsus) {
				if (caster != null) activated = jutsu.castAtLocation(caster, location, power) || activated;
				else activated = jutsu.castAtLocation(location, power) || activated;
			}
			playJutsuEffects(EffectPosition.DELAYED, location);
			if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
				pulseCount += 1;
				if (pulseCount >= totalPulses) {
					stop();
					return true;
				}
			}
			return false;
		}

		private void stop() {
			if (!block.getChunk().isLoaded()) block.getChunk().load();
			block.setType(Material.AIR);
			playJutsuEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
			if (jutsuOnBreak == null) return;
			if (caster == null) jutsuOnBreak.castAtLocation(location, power);
			else if (caster.isValid()) jutsuOnBreak.castAtLocation(caster, location, power);
		}

	}
	
	private class PulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicJutsus.scheduleRepeatingTask(this, 0, interval);
		}

		private void stop() {
			if (taskId > 0) {
				MagicJutsus.cancelTask(taskId);
				taskId = -1;
			}
		}

		@Override
		public void run() {
			for (Map.Entry<Block, Pulser> entry : new HashMap<>(pulsers).entrySet()) {
				boolean remove = entry.getValue().pulse();
				if (remove) pulsers.remove(entry.getKey());
			}
			if (pulsers.isEmpty()) stop();
		}
		
	}

}
