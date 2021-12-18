package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class ReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	private Map<Block, BlockData> blocks;

	private boolean replaceAll;
	private List<BlockData> replace;
	private List<BlockData> replaceWith;
	private List<BlockData> replaceBlacklist;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> radiusUp;
	private ConfigData<Integer> radiusDown;
	private ConfigData<Integer> radiusHoriz;
	private ConfigData<Integer> replaceDuration;

	private boolean pointBlank;
	private boolean replaceRandom;
	private boolean powerAffectsRadius;
	private final boolean checkPlugins;

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

		pointBlank = getConfigBoolean("point-blank", false);
		replaceRandom = getConfigBoolean("replace-random", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);
		checkPlugins = getConfigBoolean("check-plugins", true);

		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			replaceAll = false;
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
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-blocks item: " + block);
				}
			}
		}

		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (String s : list) {
				try {
					BlockData data = Bukkit.createBlockData(s.trim().toLowerCase());
					replaceWith.add(data);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-with item: " + s);
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
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-blacklist item: " + s);
				}
			}
		}

		if (!replaceRandom && replace.size() != replaceWith.size()) {
			replaceRandom = true;
			MagicSpells.error("ReplaceSpell " + internalName + " replace-random false, but replace-blocks and replace-with have different sizes!");
		}

		if (replace.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-blocks list!");
		if (replaceWith.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-with list!");
	}

	@Override
	public void turnOff() {
		for (Block b : blocks.keySet()) b.setBlockData(blocks.get(b));
		blocks.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pointBlank ? caster.getLocation().getBlock() : getTargetedBlock(caster, power);
			if (target == null) return noTarget(caster);
			replace(caster, target.getLocation(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return replace(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return replace(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return replace(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return replace(null, target, power, null);
	}

	private boolean replace(LivingEntity caster, Location target, float power, String[] args) {
		boolean replaced = false;
		Block block;

		int d = radiusDown.get(caster, null, power, args);
		int u = radiusUp.get(caster, null, power, args);
		int h = radiusHoriz.get(caster, null, power, args);
		if (powerAffectsRadius) {
			d *= power;
			u *= power;
			h *= power;
		}

		int yOffset = this.yOffset.get(caster, null, power, args);

		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						BlockData data = block.getBlockData();

						// If specific blocks are being replaced, skip if the block isn't replaceable.
						if (!replaceAll && !data.matches(replace.get(i))) continue;
						// If all blocks are being replaced, skip if the block is already replaced.
						if (replaceAll && data.matches(replaceWith.get(i))) continue;

						if (replaceBlacklisted(data)) continue;

						Block finalBlock = block;
						BlockState previousState = block.getState();

						// Place block.
						if (replaceRandom) block.setBlockData(replaceWith.get(Util.getRandomInt(replaceWith.size())));
						else block.setBlockData(replaceWith.get(i));

						if (checkPlugins && caster instanceof Player player) {
							Block against = target.clone().add(target.getDirection()).getBlock();
							if (block.equals(against)) against = block.getRelative(BlockFace.DOWN);
							MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, previousState, against, player.getInventory().getItemInMainHand(), player, true);
							EventUtil.call(event);
							if (event.isCancelled()) {
								previousState.update(true);
								return false;
							}
						}
						playSpellEffects(EffectPosition.SPECIAL, finalBlock.getLocation());

						// Break block.
						int replaceDuration = this.replaceDuration.get(caster, null, power, args);
						if (replaceDuration > 0) {
							blocks.put(block, data);

							MagicSpells.scheduleDelayedTask(() -> {
								BlockData previous = blocks.remove(finalBlock);
								if (previous == null) return;
								if (checkPlugins && caster instanceof Player) {
									MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(finalBlock, (Player) caster);
									EventUtil.call(event);
									if (event.isCancelled()) return;
								}
								finalBlock.setBlockData(previous);
								playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, finalBlock.getLocation());
							}, replaceDuration);
						}

						replaced = true;
						break;
					}
				}
			}
		}

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		return replaced;
	}

	private boolean replaceBlacklisted(BlockData data) {
		for (BlockData blockData : replaceBlacklist)
			if (data.matches(blockData))
				return true;

		return false;
	}

}
