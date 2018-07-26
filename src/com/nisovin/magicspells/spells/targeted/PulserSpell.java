package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nisovin.magicspells.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;

public class PulserSpell extends TargetedSpell implements TargetedLocationSpell {

	private int yOffset;
	int totalPulses;
	int interval;
	private int capPerPlayer;
	double maxDistanceSquared;
	MagicMaterial material;
	private boolean unbreakable;
	boolean onlyCountOnSuccess;
	private List<String> spellNames;
	List<TargetedLocationSpell> spells;
	private String spellNameOnBreak;
	TargetedLocationSpell spellOnBreak;

	private String strAtCap;

	HashMap<Block, Pulser> pulsers;
	private PulserTicker ticker;

	public PulserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigInt("y-offset", 0);
		totalPulses = getConfigInt("total-pulses", 5);
		interval = getConfigInt("interval", 30);
		capPerPlayer = getConfigInt("cap-per-player", 10);
		maxDistanceSquared = getConfigDouble("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;
		material = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("block-type", "diamond_block"));
		unbreakable = getConfigBoolean("unbreakable", false);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);
		spellNames = getConfigStringList("spells", null);
		spellNameOnBreak = getConfigString("spell-on-break", null);

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

		pulsers = new HashMap<>();
		ticker = new PulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();
		spells = new ArrayList<>();
		if (spellNames != null && !spellNames.isEmpty()) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell instanceof TargetedLocationSpell) {
					spells.add((TargetedLocationSpell) spell);
				}
			}
		}
		if (spellNameOnBreak != null) {
			Spell spell = MagicSpells.getSpellByInternalName(spellNameOnBreak);
			if (spell instanceof TargetedLocationSpell) {
				spellOnBreak = (TargetedLocationSpell)spell;
			} else {
				MagicSpells.error("Pulser spell '" + internalName + "' has an invalid spell-on-break spell defined");
			}
		}
		if (spells.isEmpty()) {
			MagicSpells.error("Pulser spell '" + internalName + "' has no spells defined!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Pulser pulser : pulsers.values()) {
					if (!pulser.caster.equals(player)) continue;
					
					count++;
					if (count >= capPerPlayer) {
						sendMessage(strAtCap, player, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			List<Block> lastTwo = getLastTwoTargetedBlocks(player, power);
			Block target = null;
			if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
			if (target == null) return noTarget(player);
			if (yOffset > 0) {
				target = target.getRelative(BlockFace.UP, yOffset);
			} else if (yOffset < 0) {
				target = target.getRelative(BlockFace.DOWN, yOffset);
			}
			if (target.getType() != Material.AIR && target.getType() != Material.SNOW && target.getType() != Material.TALL_GRASS) return noTarget(player);
			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(player);
				target = event.getTargetLocation().getBlock();
				power = event.getPower();
			}
			createPulser(player, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void createPulser(Player caster, Block block, float power) {
		material.setBlock(block);
		pulsers.put(block, new Pulser(caster, block, power));
		ticker.start();
		if (caster != null) {
			playSpellEffects(caster, block.getLocation());
		} else {
			playSpellEffects(EffectPosition.TARGET, block.getLocation());
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		if (yOffset > 0) {
			block = block.getRelative(BlockFace.UP, yOffset);
		} else if (yOffset < 0) {
			block = block.getRelative(BlockFace.DOWN, yOffset);
		}
		if (block.getType() == Material.AIR || block.getType() == Material.SNOW || block.getType() == Material.TALL_GRASS) {
			createPulser(caster, block, power);
			return true;
		}
		block = block.getRelative(BlockFace.UP);
		if (block.getType() == Material.AIR || block.getType() == Material.SNOW || block.getType() == Material.TALL_GRASS) {
			createPulser(caster, block, power);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser == null) return;
		event.setCancelled(true);
		if (!unbreakable) {
			pulser.stop();
			event.getBlock().setType(Material.AIR);
			pulsers.remove(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!pulsers.isEmpty()) {
			Iterator<Block> iter = event.blockList().iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				Pulser pulser = pulsers.get(b);
				if (pulser == null) continue;
				iter.remove();
				if (!unbreakable) {
					pulser.stop();
					pulsers.remove(b);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (pulsers.isEmpty()) return;
		for (Block b : event.getBlocks()) {
			Pulser pulser = pulsers.get(b);
			if (pulser == null) continue;
			event.setCancelled(true);
			if (!unbreakable) {
				pulser.stop();
				pulsers.remove(b);
			}
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
	
	public class Pulser {

		Player caster;
		Block block;
		Location location;
		float power;
		int pulseCount;
		
		public Pulser(Player caster, Block block, float power) {
			this.caster = caster;
			this.block = block;
			this.location = block.getLocation().add(0.5, 0.5, 0.5);
			this.power = power;
			this.pulseCount = 0;
		}

		public boolean pulse() {
			if (caster == null) {
				if (material.equals(block) && block.getChunk().isLoaded()) return activate();
				stop();
				return true;
			} else if (caster.isValid() && caster.isOnline() && material.equals(block) && block.getChunk().isLoaded()) {
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
			for (TargetedLocationSpell spell : spells) {
				if (caster != null) {
					activated = spell.castAtLocation(caster, location, power) || activated;
				} else {
					activated = spell.castAtLocation(location, power) || activated;
				}
			}
			playSpellEffects(EffectPosition.DELAYED, location);
			if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
				pulseCount += 1;
				if (pulseCount >= totalPulses) {
					stop();
					return true;
				}
			}
			return false;
		}

		public void stop() {
			if (!block.getChunk().isLoaded()) block.getChunk().load();
			block.setType(Material.AIR);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
			if (spellOnBreak != null) {
				if (caster == null) {
					spellOnBreak.castAtLocation(location, power);
				} else if (caster.isValid()) {
					spellOnBreak.castAtLocation(caster, location, power);
				}
			}
		}

	}
	
	public class PulserTicker implements Runnable {

		private int taskId = -1;

		public void start() {
			if (taskId < 0) {
				taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);
			}
		}

		public void stop() {
			if (taskId > 0) {
				Bukkit.getScheduler().cancelTask(taskId);
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
