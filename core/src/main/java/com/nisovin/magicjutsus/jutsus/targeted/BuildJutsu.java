package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.MagicJutsusBlockPlaceEvent;

public class BuildJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Set<Material> allowedTypes;

	private String strCantBuild;
	private String strInvalidBlock;

	private int slot;

	private boolean consumeBlock;
	private boolean checkPlugins;
	private boolean playBreakEffect;

	public BuildJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		strCantBuild = getConfigString("str-cant-build", "You can't build there.");
		strInvalidBlock = getConfigString("str-invalid-block", "You can't build that block.");

		slot = getConfigInt("slot", 0);

		consumeBlock = getConfigBoolean("consume-block", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("show-effect", true);

		List<String> materials = getConfigStringList("allowed-types", null);
		if (materials == null) {
			materials = new ArrayList<>();
			materials.add("GRASS_BLOCK");
			materials.add("STONE");
			materials.add("DIRT");
		}

		allowedTypes = new HashSet<>();
		for (String str : materials) {
			Material material = Material.getMaterial(str.toUpperCase());
			if (material == null) {
				MagicJutsus.error("BuildJutsu '" + internalName + "' has an invalid material '" + str + "' defined!");
				continue;
			}
			if (!material.isBlock()) {
				MagicJutsus.error("BuildJutsu '" + internalName + "' has a non block material '" + str + "' defined!");
				continue;
			}

			allowedTypes.add(material);
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) return noTarget(player, strInvalidBlock);
			
			List<Block> lastBlocks;
			try {
				lastBlocks = getLastTwoTargetedBlocks(player, power);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				lastBlocks = null;
			}

			if (lastBlocks == null || lastBlocks.size() < 2 || BlockUtils.isAir(lastBlocks.get(1).getType())) return noTarget(player, strCantBuild);

			boolean built = build(player, lastBlocks.get(0), lastBlocks.get(1), item);
			if (!built) return noTarget(player, strCantBuild);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!(caster instanceof Player)) return false;
		ItemStack item = ((Player) caster).getInventory().getItem(slot);
		if (item == null || !isAllowed(item.getType())) return false;

		Block block = target.getBlock();

		return build((Player) caster, block, block, item);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private boolean isAllowed(Material mat) {
		if (!mat.isBlock()) return false;
		if (allowedTypes == null) return false;
		return allowedTypes.contains(mat);
	}

	private boolean build(Player player, Block block, Block against, ItemStack item) {
		BlockState previousState = block.getState();
		block.setType(item.getType());

		if (checkPlugins) {
			MagicJutsusBlockPlaceEvent event = new MagicJutsusBlockPlaceEvent(block, previousState, against, player.getEquipment().getItemInMainHand(), player, true);
			EventUtil.call(event);
			if (event.isCancelled() && block.getType() == item.getType()) {
				previousState.update(true);
				return false;
			}
		}

		if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

		playJutsuEffects(player, block.getLocation());

		if (consumeBlock) {
			int amt = item.getAmount() - 1;
			if (amt > 0) {
				item.setAmount(amt);
				player.getInventory().setItem(slot, item);
			} else player.getInventory().setItem(slot, null);
		}

		return true;
	}

}
