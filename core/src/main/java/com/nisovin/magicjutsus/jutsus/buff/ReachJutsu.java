package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.MagicJutsusBlockBreakEvent;
import com.nisovin.magicjutsus.events.MagicJutsusBlockPlaceEvent;

// TODO this needs exemptions for anticheat
public class ReachJutsu extends BuffJutsu {

	private Set<UUID> reaching;

	private int range;
	private boolean dropBlocks;
	private boolean consumeBlocks;

	private Set<Material> disallowedBreakBlocks;
	private Set<Material> disallowedPlaceBlocks;
	
	public ReachJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		range = getConfigInt("range", 15);
		dropBlocks = getConfigBoolean("drop-blocks", true);
		consumeBlocks = getConfigBoolean("consume-blocks", true);

		reaching = new HashSet<>();
		disallowedPlaceBlocks = new HashSet<>();
		disallowedBreakBlocks = new HashSet<>();

		List<String> list = getConfigStringList("disallowed-break-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) continue;
				disallowedBreakBlocks.add(material);
			}
		}

		list = getConfigStringList("disallowed-place-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) continue;
				disallowedBreakBlocks.add(material);
			}
		}

	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		reaching.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return reaching.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		reaching.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		reaching.clear();
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!isActive(event.getPlayer())) return;
		Player player = event.getPlayer();
		
		if (isExpired(player)) {
			turnOff(player);
			return;
		}
		
		// Get targeted block
		Action action = event.getAction();
		List<Block> targets = getLastTwoTargetedBlocks(player, range);

		Block airBlock;
		Block targetBlock;
		if (targets == null) return;
		if (targets.size() != 2) return;

		airBlock = targets.get(0);
		targetBlock = targets.get(1);
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && !BlockUtils.isAir(targetBlock.getType())) {
			// Break
			
			// Check for disallowed
			if (disallowedBreakBlocks.contains(targetBlock.getType())) return;
			
			// Call break event
			MagicJutsusBlockBreakEvent evt = new MagicJutsusBlockBreakEvent(targetBlock, player);
			EventUtil.call(evt);
			if (evt.isCancelled()) return;
			// Remove block
			targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, targetBlock.getType());
			// Drop item
			if (dropBlocks && player.getGameMode() == GameMode.SURVIVAL) targetBlock.breakNaturally();
			else targetBlock.setType(Material.AIR);
			addUseAndChargeCost(player);

		} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && !BlockUtils.isAir(targetBlock.getType())) {
			// Place
			
			// Check for block in hand
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if (inHand != null && inHand.getType() != Material.AIR && inHand.getType().isBlock()) {
				
				// Check for disallowed
				if (disallowedPlaceBlocks.contains(inHand.getType())) return;
				
				BlockState prevState = airBlock.getState();
				
				// Place block
				BlockState state = airBlock.getState();
				state.setType(inHand.getType());
				state.setData(inHand.getData());
				state.update(true);
				
				// Call event
				MagicJutsusBlockPlaceEvent evt = new MagicJutsusBlockPlaceEvent(airBlock, prevState, targetBlock, inHand, player, true);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					// Cancelled, revert
					prevState.update(true);
					return;
				}
				// Remove item from hand
				if (consumeBlocks && player.getGameMode() != GameMode.CREATIVE) {
					if (inHand.getAmount() > 1) {
						inHand.setAmount(inHand.getAmount() - 1);
						player.getEquipment().setItemInMainHand(inHand);
					} else {
						player.getEquipment().setItemInMainHand(null);
					}

					addUseAndChargeCost(player);
					event.setCancelled(true);
				}
			}
		}
	}

}
