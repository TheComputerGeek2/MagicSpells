package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import io.papermc.paper.event.entity.EntityMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class FrostwalkSpell extends BuffSpell {

	private final Map<UUID, BlockPlatform> entities;

	private final ConfigData<Integer> size;

	private final ConfigData<Boolean> leaveFrozen;

	public FrostwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		size = getConfigDataInt("size", 2);

		leaveFrozen = getConfigDataBoolean("leave-frozen", false);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		entities.put(data.target().getUniqueId(), new BlockPlatform(
			Material.ICE,
			Material.WATER,
			data.target().getLocation().getBlock().getRelative(0, -1, 0),
			size.get(data),
			!leaveFrozen.get(data),
			"square"
		));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());

		BlockPlatform old = entities.get(data.target().getUniqueId());
		if (old != null) old.destroyPlatform();

		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		BlockPlatform platform = entities.remove(entity.getUniqueId());
		if (platform != null) platform.destroyPlatform();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	private void handleMove(LivingEntity entity, Location to, Location from) {
		if (!isActive(entity)) return;
		if (isExpired(entity)) {
			turnOff(entity);
			return;
		}

		Block block;
		boolean teleportUp = false;

		double locationToY = to.getY();
		double locationFromY = from.getY();

		Block locationToBlock = to.getBlock();

		if (locationToY > locationFromY && locationToY % 1 > .62 && locationToBlock.getType() == Material.WATER && locationToBlock.getRelative(0, 1, 0).getType().isAir()) {
			block = locationToBlock;
			teleportUp = true;
		} else block = locationToBlock.getRelative(0, -1, 0);

		boolean moved = entities.get(entity.getUniqueId()).movePlatform(block);
		if (!moved) return;

		addUseAndChargeCost(entity);

		if (teleportUp) {
			Location loc = entity.getLocation().clone();
			loc.setY(to.getBlockY() + 1);
			Util.tryTeleportMounted(entity, loc);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		handleMove(event.getPlayer(), event.getTo(), event.getFrom());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityMove(EntityMoveEvent event) {
		handleMove(event.getEntity(), event.getTo(), event.getFrom());
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (entities.isEmpty()) return;
		Block block = event.getBlock();
		if (block.getType() != Material.ICE) return;
		if (Util.containsValueParallel(entities, platform -> platform.blockInPlatform(block))) event.setCancelled(true);
	}

	public Map<UUID, BlockPlatform> getEntities() {
		return entities;
	}

}
