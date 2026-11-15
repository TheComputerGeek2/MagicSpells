package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

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
import org.bukkit.util.RayTraceResult;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

// TODO this needs exemptions for anticheat
public class ReachSpell extends BuffSpell {

	private final Map<UUID, ReachData> players;

	private final Set<Material> disallowedBreakBlocks;
	private final Set<Material> disallowedPlaceBlocks;

	private final ConfigData<Boolean> dropBlocks;
	private final ConfigData<Boolean> consumeBlocks;

	public ReachSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		dropBlocks = getConfigDataBoolean("drop-blocks", true);
		consumeBlocks = getConfigDataBoolean("consume-blocks", true);

		players = new HashMap<>();
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
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;
		players.put(target.getUniqueId(), new ReachData(data, dropBlocks.get(data), consumeBlocks.get(data)));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		players.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return players.keySet();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!isActive(event.getPlayer())) return;
		Player player = event.getPlayer();

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		ReachData data = players.get(player.getUniqueId());

		// Get targeted block
		Action action = event.getAction();
		RayTraceResult result = rayTraceBlocks(data.spellData);
		if (result == null) return;

		Block targetBlock = result.getHitBlock();
		Block airBlock = result.getHitBlock().getRelative(result.getHitBlockFace());

		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			// Break

			// Check for disallowed
			if (disallowedBreakBlocks.contains(targetBlock.getType())) return;

			MagicSpellsBlockBreakEvent evt = new MagicSpellsBlockBreakEvent(targetBlock, player);
			if (!evt.callEvent()) return;

			// Remove block
			targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.DESTROY_BLOCK, targetBlock.getBlockData());

			if (!data.dropBlocks) targetBlock.setType(Material.AIR);
			else {
				GameMode mode = player.getGameMode();
				if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) targetBlock.breakNaturally();
			}

			addUseAndChargeCost(player);
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK && airBlock.isReplaceable()) {
			// Place
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.isEmpty()) return;

			// Check for block in hand and for disallowed
			Material type = item.getType();
			if (!type.isBlock() || disallowedPlaceBlocks.contains(type)) return;

			// Place block
			BlockData blockData = type.createBlockData();
			if (!airBlock.canPlace(blockData)) return;

			BlockState oldState = airBlock.getState();
			airBlock.setBlockData(blockData);

			// Call event
			MagicSpellsBlockPlaceEvent evt = new MagicSpellsBlockPlaceEvent(airBlock, oldState, targetBlock, item, player, true);
			if (!evt.callEvent()) {
				// Cancelled, revert
				oldState.update(true);
				return;
			}

			// Remove item from hand
			if (data.consumeBlocks && player.getGameMode() != GameMode.CREATIVE) {
				if (item.getAmount() > 1) {
					item.setAmount(item.getAmount() - 1);
					player.getEquipment().setItemInMainHand(item);
				} else {
					player.getEquipment().setItemInMainHand(null);
				}

				addUseAndChargeCost(player);
				event.setCancelled(true);
			}
		}
	}

	public Map<UUID, ReachData> getPlayers() {
		return players;
	}

	public Set<Material> getDisallowedBreakBlocks() {
		return disallowedBreakBlocks;
	}

	public Set<Material> getDisallowedPlaceBlocks() {
		return disallowedPlaceBlocks;
	}

	public record ReachData(SpellData spellData, boolean dropBlocks, boolean consumeBlocks) {
	}

}
