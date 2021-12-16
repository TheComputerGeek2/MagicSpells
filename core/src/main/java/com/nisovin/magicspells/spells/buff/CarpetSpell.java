package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BlockPlatform;
import com.nisovin.magicspells.util.config.ConfigData;

public class CarpetSpell extends BuffSpell {

	private final Map<UUID, BlockPlatform> entities;

	private final Set<UUID> falling;

	private Material platformMaterial;

	private ConfigData<Integer> platformSize;

	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String materialName = getConfigString("platform-block", "GLASS");
		platformMaterial = Util.getMaterial(materialName);

		if (platformMaterial == null || !platformMaterial.isBlock()) {
			platformMaterial = null;
			MagicSpells.error("CarpetSpell " + internalName + " has an invalid platform-block defined! '" + materialName + "'");
		}

		platformSize = getConfigDataInt("size", 2);

		entities = new HashMap<>();
		falling = new HashSet<>();
	}
	
	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new BlockPlatform(platformMaterial, Material.AIR, entity.getLocation().getBlock().getRelative(0, -1, 0), platformSize.get(entity, null, power, args), true, "square"));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		BlockPlatform platform = entities.get(entity.getUniqueId());
		if (platform == null) return;

		platform.destroyPlatform();
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(entities, BlockPlatform::destroyPlatform);
		entities.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		BlockPlatform platform = entities.get(player.getUniqueId());
		if (platform == null) return;

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = event.getTo().getBlock().getRelative(BlockFace.DOWN);
		if (falling.contains(player.getUniqueId())) block = event.getTo().getBlock().getRelative(BlockFace.DOWN, 2);

		boolean moved = platform.isMoved(block, true);
		if (moved) {
			platform.movePlatform(block, true);
			addUseAndChargeCost(player);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		if (player.isSneaking()) {
			falling.remove(player.getUniqueId());
			return;
		}

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2);
		boolean moved = entities.get(player.getUniqueId()).movePlatform(block);

		if (moved) {
			falling.add(player.getUniqueId());
			addUseAndChargeCost(player);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (entities.isEmpty()) return;
		final Block block = event.getBlock();
		if (block.getType() != platformMaterial) return;
		if (Util.containsValueParallel(entities, platform -> platform.blockInPlatform(block))) event.setCancelled(true);
	}

	public Map<UUID, BlockPlatform> getEntities() {
		return entities;
	}

	public Set<UUID> getFalling() {
		return falling;
	}

	public Material getPlatformMaterial() {
		return platformMaterial;
	}

	public void setPlatformMaterial(Material platformMaterial) {
		this.platformMaterial = platformMaterial;
	}

}
