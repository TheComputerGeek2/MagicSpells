package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class ReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Map<Block, BlockData> blocks;

	private final boolean replaceAll;
	private final List<BlockData> replace;
	private final List<BlockData> replaceWith;
	private final List<BlockData> replaceBlacklist;

	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> radiusUp;
	private final ConfigData<Integer> radiusDown;
	private final ConfigData<Integer> radiusHoriz;
	private final ConfigData<Integer> replaceDuration;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> replaceRandom;
	private final ConfigData<Boolean> powerAffectsRadius;
	private final ConfigData<Boolean> resolveDurationPerBlock;

	public ReplaceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blocks = new HashMap<>();
		replace = new ArrayList<>();
		replaceWith = new ArrayList<>();
		replaceBlacklist = new ArrayList<>();

		yOffset = getConfigDataInt("y-offset", 0);
		radiusUp = getConfigDataInt("radius-up", 1);
		radiusDown = getConfigDataInt("radius-down", 1);
		radiusHoriz = getConfigDataInt("radius-horiz", 1);
		replaceDuration = getConfigDataInt("duration", 0);

		pointBlank = getConfigDataBoolean("point-blank", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		ConfigData<Boolean> replaceRandom = getConfigDataBoolean("replace-random", true);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", false);
		resolveDurationPerBlock = getConfigDataBoolean("resolve-duration-per-block", false);

		boolean replaceAll = false;
		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			for (String block : list) {
				if (block.equals("all")) {
					replaceAll = true;
					// Just a filler.
					replace.add(null);
					break;
				}

				try {
					BlockData data = Bukkit.createBlockData(block.trim().toLowerCase());
					replace.add(data);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ReplaceSpell '" + internalName + "' has an invalid 'replace-blocks' item: " + block);
				}
			}
		}
		this.replaceAll = replaceAll;

		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (String s : list) {
				try {
					BlockData data = Bukkit.createBlockData(s.trim().toLowerCase());
					replaceWith.add(data);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ReplaceSpell '" + internalName + "' has an invalid 'replace-with' item: " + s);
				}
			}
		}

		list = getConfigStringList("replace-blacklist", null);
		if (list != null) {
			for (String s : list) {
				try {
					BlockData data = Bukkit.createBlockData(s.trim().toLowerCase());
					replaceBlacklist.add(data);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ReplaceSpell '" + internalName + "' has an invalid 'replace-blacklist' item: " + s);
				}
			}
		}

		if (replace.size() != replaceWith.size() && (!replaceRandom.isConstant() || !replaceRandom.get())) {
			replaceRandom = data -> true;
			if (replaceRandom.isConstant())
				MagicSpells.error("ReplaceSpell '" + internalName + "' had 'replace-random' as false, but replace-blocks and replace-with have different sizes!");
			else
				MagicSpells.error("ReplaceSpell '" + internalName + "' has a 'replace-random' that can be false, but replace-blocks and replace-with have different sizes!");
		}
		this.replaceRandom = replaceRandom;

		if (replace.isEmpty()) MagicSpells.error("ReplaceSpell '" + internalName + "' has empty 'replace-blocks' list!");
		if (replaceWith.isEmpty()) MagicSpells.error("ReplaceSpell '" + internalName + "' has empty 'replace-with' list!");
	}

	@Override
	public void turnOff() {
		for (Block b : blocks.keySet()) b.setBlockData(blocks.get(b));
		blocks.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (pointBlank.get(data)) {
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!event.callEvent()) return noTarget(event);
			data = event.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		boolean replaced = false;
		Block block;

		int d = radiusDown.get(data);
		int u = radiusUp.get(data);
		int h = radiusHoriz.get(data);
		if (powerAffectsRadius.get(data)) {
			d = Math.round(d * data.power());
			u = Math.round(u * data.power());
			h = Math.round(h * data.power());
		}

		boolean checkPlugins = this.checkPlugins.get(data);
		boolean replaceRandom = this.replaceRandom.get(data);
		boolean resolveDurationPerBlock = this.resolveDurationPerBlock.get(data);

		int yOffset = this.yOffset.get(data);
		int replaceDuration = resolveDurationPerBlock ? 0 : this.replaceDuration.get(data);

		Location target = data.location();
		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						BlockData blockData = block.getBlockData();

						// If specific blocks are being replaced, skip if the block isn't replaceable.
						if (!replaceAll && !blockData.matches(replace.get(i))) continue;
						// If all blocks are being replaced, skip if the block is already replaced.
						if (replaceAll && blockData.matches(replaceWith.get(i))) continue;

						if (replaceBlacklisted(blockData)) continue;

						Block finalBlock = block;
						BlockState previousState = block.getState();

						// Place block.
						if (replaceRandom) block.setBlockData(replaceWith.get(ThreadLocalRandom.current().nextInt(replaceWith.size())));
						else block.setBlockData(replaceWith.get(i));

						if (checkPlugins && data.caster() instanceof Player player) {
							Block against = target.clone().add(target.getDirection()).getBlock();
							if (block.equals(against)) against = block.getRelative(BlockFace.DOWN);
							MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, previousState, against, player.getInventory().getItemInMainHand(), player, true);
							EventUtil.call(event);
							if (event.isCancelled()) {
								previousState.update(true);
								continue;
							}
						}

						SpellData subData = data.location(finalBlock.getLocation());
						playSpellEffects(EffectPosition.SPECIAL, finalBlock.getLocation(), subData);

						// Break block.
						if (resolveDurationPerBlock) replaceDuration = this.replaceDuration.get(subData);
						if (replaceDuration > 0) {
							blocks.put(block, blockData);

							MagicSpells.scheduleDelayedTask(() -> {
								BlockData previous = blocks.remove(finalBlock);
								if (previous == null) return;
								if (checkPlugins && data.caster() instanceof Player player) {
									MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(finalBlock, player);
									EventUtil.call(event);
									if (event.isCancelled()) return;
								}
								finalBlock.setBlockData(previous);
								playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, finalBlock.getLocation(), subData);
							}, replaceDuration, target);
						}

						replaced = true;
						break;
					}
				}
			}
		}

		playSpellEffects(data);
		return new CastResult(replaced ?PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED, data);
	}

	private boolean replaceBlacklisted(BlockData data) {
		for (BlockData blockData : replaceBlacklist)
			if (data.matches(blockData))
				return true;

		return false;
	}

}
