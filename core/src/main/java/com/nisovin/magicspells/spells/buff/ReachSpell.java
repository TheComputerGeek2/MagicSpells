package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
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

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

// TODO this needs exemptions for anticheat
public class ReachSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private final Set<Material> disallowedBreakBlocks;
	private final Set<Material> disallowedPlaceBlocks;

	private boolean dropBlocks;
	private boolean consumeBlocks;
	
	public ReachSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		dropBlocks = getConfigBoolean("drop-blocks", true);
		consumeBlocks = getConfigBoolean("consume-blocks", true);

		entities = new HashMap<>();
		disallowedPlaceBlocks = new HashSet<>();
		disallowedBreakBlocks = new HashSet<>();

		List<String> list = getConfigStringList("disallowed-break-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Util.getMaterial(s);
				if (material == null) continue;
				disallowedBreakBlocks.add(material);
			}
		}

		list = getConfigStringList("disallowed-place-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Util.getMaterial(s);
				if (material == null) continue;
				disallowedBreakBlocks.add(material);
			}
		}

	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!isActive(event.getPlayer())) return;
		Player player = event.getPlayer();
		
		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		SpellData data = entities.get(player.getUniqueId());
		
		// Get targeted block
		Action action = event.getAction();
		List<Block> targets = getLastTwoTargetedBlocks(player, getRange(player, data.power(), data.args()));

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
			MagicSpellsBlockBreakEvent evt = new MagicSpellsBlockBreakEvent(targetBlock, player);
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
			if (inHand.getType() != Material.AIR && inHand.getType().isBlock()) {
				
				// Check for disallowed
				if (disallowedPlaceBlocks.contains(inHand.getType())) return;
				
				BlockState prevState = airBlock.getState();
				
				// Place block
				BlockState state = airBlock.getState();
				state.setType(inHand.getType());
				state.setData(inHand.getData());
				state.update(true);
				
				// Call event
				MagicSpellsBlockPlaceEvent evt = new MagicSpellsBlockPlaceEvent(airBlock, prevState, targetBlock, inHand, player, true);
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

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public Set<Material> getDisallowedBreakBlocks() {
		return disallowedBreakBlocks;
	}

	public Set<Material> getDisallowedPlaceBlocks() {
		return disallowedPlaceBlocks;
	}

	public boolean shouldDropBlocks() {
		return dropBlocks;
	}

	public void setDropBlocks(boolean dropBlocks) {
		this.dropBlocks = dropBlocks;
	}

	public boolean shouldConsumeBlocks() {
		return consumeBlocks;
	}

	public void setConsumeBlocks(boolean consumeBlocks) {
		this.consumeBlocks = consumeBlocks;
	}

}
