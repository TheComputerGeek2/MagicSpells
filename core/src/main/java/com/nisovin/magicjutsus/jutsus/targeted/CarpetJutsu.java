package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class CarpetJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Map<Block, Player> blocks;

	private Material material;
	private String materialName;

	private int radius;
	private int duration;
	private int touchCheckInterval;

	private boolean circle;
	private boolean removeOnTouch;

	private String jutsuOnTouchName;
	private Ninjutsu jutsuOnTouch;

	private TouchChecker checker;
	
	public CarpetJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("block", "white_carpet").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("CarpetJutsu '" + internalName + "' has an invalid block defined!");
			material = null;
		}

		radius = getConfigInt("radius", 1);
		duration = getConfigInt("duration", 0);
		touchCheckInterval = getConfigInt("touch-check-interval", 3);

		circle = getConfigBoolean("circle", false);
		removeOnTouch = getConfigBoolean("remove-on-touch", true);

		jutsuOnTouchName = getConfigString("jutsu-on-touch", "");

		blocks = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuOnTouch = new Ninjutsu(jutsuOnTouchName);
		if (!jutsuOnTouch.process() || !jutsuOnTouch.isTargetedEntityJutsu()) {
			if (!jutsuOnTouchName.isEmpty()) MagicJutsus.error("CarpetJutsu '" + internalName + "' has an invalid jutsu-on-touch defined!");
			jutsuOnTouch = null;
		}

		if (jutsuOnTouch != null) checker = new TouchChecker();
	}
	
	@Override
	public void turnOff() {
		super.turnOff();

		blocks.clear();
		if (checker != null) checker.stop();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Location loc = null;
			if (targetSelf) loc = player.getLocation();
			else {
				Block b = getTargetedBlock(player, power);
				if (b != null && b.getType() != Material.AIR) loc = b.getLocation();
			}

			if (loc == null) return noTarget(player);

			layCarpet(player, loc, power);
		}
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!(caster instanceof Player)) return false;
		if (targetSelf) layCarpet((Player) caster, caster.getLocation(), power);
		else layCarpet((Player) caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		layCarpet(null, target, power);
		return true;
	}
	
	private void layCarpet(Player player, Location loc, float power) {
		if (!loc.getBlock().getType().isOccluding()) {
			int c = 0;
			while (!loc.getBlock().getRelative(0, -1, 0).getType().isOccluding() && c <= 2) {
				loc.subtract(0, 1, 0);
				c++;
			}
		} else {
			int c = 0;
			while (loc.getBlock().getType().isOccluding() && c <= 2) {
				loc.add(0, 1, 0);
				c++;
			}
		}

		Block b;
		int y = loc.getBlockY();
		int rad = Math.round(radius * power);

		final List<Block> blockList = new ArrayList<>();
		for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
			for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
				b = loc.getWorld().getBlockAt(x, y, z);
				if (circle && loc.getBlock().getLocation().distanceSquared(b.getLocation()) > radius * radius) continue;

				if (b.getType().isOccluding()) b = b.getRelative(0, 1, 0);
				else if (!b.getRelative(0, -1, 0).getType().isOccluding()) b = b.getRelative(0, -1, 0);

				if (!BlockUtils.isAir(b.getType()) && !b.getRelative(0, -1, 0).getType().isSolid()) continue;

				b.setType(material, false);
				blockList.add(b);
				blocks.put(b, player);
				playJutsuEffects(EffectPosition.TARGET, b.getLocation().add(0.5, 0, 0.5));
			}
		}

		if (duration > 0 && !blockList.isEmpty()) {
			MagicJutsus.scheduleDelayedTask(() -> {
				for (Block b1 : blockList) {
					if (!material.equals(b1.getType())) continue;
					b1.setType(Material.AIR);
					if (blocks != null) blocks.remove(b1);
				}
			}, duration);
		}
		if (player != null) playJutsuEffects(EffectPosition.CASTER, player);
	}
	
	private class TouchChecker implements Runnable {
		
		private int taskId;
		
		private TouchChecker() {
			taskId = MagicJutsus.scheduleRepeatingTask(this, touchCheckInterval, touchCheckInterval);
		}
		
		@Override
		public void run() {
			if (blocks.isEmpty()) return;
			for (Player player : Bukkit.getOnlinePlayers()) {

				Block b = player.getLocation().getBlock();
				Player caster = blocks.get(b);

				if (caster == null) continue;
				if (player.equals(caster)) continue;
				if (!material.equals(b.getType())) continue;
				
				JutsuTargetEvent event = new JutsuTargetEvent(jutsuOnTouch.getJutsu(), caster, player, 1F);
				EventUtil.call(event);
				if (event.isCancelled()) continue;

				if (jutsuOnTouch != null) jutsuOnTouch.castAtEntity(caster, player, event.getPower());
				if (!removeOnTouch) continue;

				b.setType(Material.AIR);
				blocks.remove(b);
			}
		}
		
		private void stop() {
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
