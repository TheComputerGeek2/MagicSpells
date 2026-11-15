package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;

public class ZapSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Set<BlockData> allowedBlockTypes;
	private final Set<BlockData> disallowedBlockTypes;

	private final String strCantZap;

	private final ConfigData<Boolean> dropBlock;
	private final ConfigData<Boolean> dropNormal;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> playBreakEffect;

	public ZapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<String> allowed = getConfigStringList("allowed-block-types", null);
		if (allowed != null && !allowed.isEmpty()) {
			allowedBlockTypes = new HashSet<>();
			for (String s : allowed) {
				try {
					BlockData bd = Bukkit.createBlockData(s.toLowerCase());
					allowedBlockTypes.add(bd);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Invalid allowed block type '" + s + "' in ZapSpell '" + internalName + "'.");
				}
			}
		} else allowedBlockTypes = null;

		List<String> disallowed = getConfigStringList("disallowed-block-types", Arrays.asList("bedrock", "lava", "water"));
		if (disallowed != null && !disallowed.isEmpty()) {
			disallowedBlockTypes = new HashSet<>();
			for (String s : disallowed) {
				try {
					BlockData bd = Bukkit.createBlockData(s.toLowerCase());
					disallowedBlockTypes.add(bd);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Invalid disallowed block type '" + s + "' in ZapSpell '" + internalName + "'.");
				}
			}
		} else disallowedBlockTypes = null;

		strCantZap = getConfigString("str-cant-zap", "");

		dropBlock = getConfigDataBoolean("drop-block", false);
		dropNormal = getConfigDataBoolean("drop-normal", true);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		playBreakEffect = getConfigDataBoolean("play-break-effect", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Location> info = getTargetedBlockLocation(data);
		if (info.noTarget()) return noTarget(strCantZap, info);

		return castAtLocation(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location location = data.location();

		Block target = location.getBlock();
		if (!canZap(target)) return noTarget(strCantZap, data);

		if (checkPlugins.get(data) && data.caster() instanceof Player caster) {
			MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(target, caster);
			if (!event.callEvent()) return noTarget(strCantZap, data);
		}

		if (playBreakEffect.get(data)) target.getWorld().playEffect(target.getLocation(), Effect.DESTROY_BLOCK, target.getBlockData());

		if (dropBlock.get(data)) {
			if (dropNormal.get(data)) target.breakNaturally();
			else location.getWorld().dropItemNaturally(location, ItemStack.of(target.getBlockData().getPlacementMaterial()));
		}

		target.setType(Material.AIR);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private boolean canZap(Block target) {
		BlockData bd = target.getBlockData();

		if (disallowedBlockTypes != null) {
			for (BlockData data : disallowedBlockTypes)
				if (bd.matches(data))
					return false;
		}

		if (allowedBlockTypes == null) return true;

		for (BlockData data : allowedBlockTypes)
			if (bd.matches(data))
				return true;

		return false;
	}

}
