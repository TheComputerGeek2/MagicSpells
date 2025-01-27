package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;

public class LifewalkSpell extends BuffSpell {
	
	private final Set<UUID> entities;

	private final Map<Material, Integer> blocks;

	private Grower grower;

	private int tickInterval;
	
	public LifewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tickInterval = getConfigInt("tick-interval", 15);

		blocks = new HashMap<>();

		entities = new HashSet<>();

		List<String> blockList = getConfigStringList("blocks", null);
		if (blockList != null) {
			for (String str : blockList) {
				String[] string = str.split(" ");
				Material material;
				int chance = 0;
				if (string.length < 2) MagicSpells.error("LifewalkSpell " + internalName + " has an invalid block defined");

				material = Util.getMaterial(string[0]);
				if (material == null) MagicSpells.error("LifewalkSpell " + internalName + " has an invalid block defined: " + string[0]);
				if (string.length >= 2 && string[1] == null) MagicSpells.error("LifewalkSpell " + internalName + " has an invalid chance defined for block: " + string[0]);
				else if (string.length >= 2) chance = Integer.parseInt(string[1]);

				if (material != null && chance > 0) blocks.put(material, chance);
			}
		} else {
			blocks.put(Material.TALL_GRASS, 25);
			blocks.put(Material.FERN, 20);
			blocks.put(Material.POPPY, 15);
			blocks.put(Material.DANDELION, 10);
			blocks.put(Material.OAK_SAPLING, 5);
		}

	}

	@Override
	public boolean castBuff(SpellData data) {
		entities.add(data.target().getUniqueId());
		if (grower == null) grower = new Grower();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		if (!entities.isEmpty()) return;
		if (grower == null) return;
		
		grower.stop();
		grower = null;
	}
	
	@Override
	protected void turnOff() {
		entities.clear();
		if (grower == null) return;
		
		grower.stop();
		grower = null;
	}

	public Set<UUID> getEntities() {
		return entities;
	}

	public Map<Material, Integer> getBlocks() {
		return blocks;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	private class Grower implements Runnable {
		
		private final ScheduledTask task;

		private Grower() {
			task = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);
		}
		
		public void stop() {
			MagicSpells.cancelTask(task);
		}
		
		@Override
		public void run() {
			for (UUID id : entities) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null) continue;
				if (!entity.isValid()) continue;
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (isExpired(livingEntity)) {
					turnOff(livingEntity);
					continue;
				}

				Block feet = livingEntity.getLocation().getBlock();
				Block ground = feet.getRelative(BlockFace.DOWN);

				if (!feet.getType().isAir()) continue;
				if (ground.getType() != Material.DIRT && ground.getType() != Material.GRASS_BLOCK) continue;
				if (ground.getType() == Material.DIRT) ground.setType(Material.GRASS_BLOCK);

				int rand = random.nextInt(100);

				for (Material m : blocks.keySet()) {
					int chance = blocks.get(m);

					if (rand > chance) continue;

					feet.setType(m);
					addUseAndChargeCost(livingEntity);

					rand = random.nextInt(100);
				}
			}
		}
	}

}
