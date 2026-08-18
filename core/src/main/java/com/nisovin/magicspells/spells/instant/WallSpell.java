package com.nisovin.magicspells.spells.instant;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.RayTraceResult;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class WallSpell extends TargetedSpell implements TargetedLocationSpell {

	private static final Multimap<UUID, WallData> blockSets = MultimapBuilder.hashKeys().arrayListValues().build();
	private static BreakListener breakListener;

	private final List<Material> materials;

	private final String strAtCap;

	private final String spellOnBreakName;

	private Subspell spellOnBreak;

	private final int capPerEntity;
	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> wallWidth;
	private final ConfigData<Integer> wallDepth;
	private final ConfigData<Integer> wallHeight;
	private final ConfigData<Integer> wallDuration;

	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> preventDrops;
	private final ConfigData<Boolean> alwaysOnGround;
	private final ConfigData<Boolean> preventBreaking;
	private final ConfigData<Boolean> checkPluginsPerBlock;
	private final ConfigData<Boolean> powerAffectsWallWidth;
	private final ConfigData<Boolean> powerAffectsWallHeight;
	private final ConfigData<Boolean> powerAffectsWallDuration;

	public WallSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materials = new ArrayList<>();

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");
		strNoTarget = getConfigString("str-no-target", "Unable to create a wall.");

		List<String> blocks = getConfigStringList("wall-types", null);
		if (blocks != null && !blocks.isEmpty()) {
			for (String s : blocks) {
				Material material = Util.getMaterial(s);
				if (material == null) continue;
				if (!material.isBlock()) continue;
				materials.add(material);
			}
		}

		spellOnBreakName = getConfigString("spell-on-break", "");

		yOffset = getConfigDataInt("y-offset", -1);
		wallWidth = getConfigDataInt("wall-width", 5);
		wallDepth = getConfigDataInt("wall-depth", 1);
		wallHeight = getConfigDataInt("wall-height", 3);
		wallDuration = getConfigDataInt("wall-duration", 15);
		capPerEntity = getConfigInt("cap-per-entity", 1);

		checkPlugins = getConfigDataBoolean("check-plugins", true);
		preventDrops = getConfigDataBoolean("prevent-drops", true);
		alwaysOnGround = getConfigDataBoolean("always-on-ground", false);
		preventBreaking = getConfigDataBoolean("prevent-breaking", false);
		checkPluginsPerBlock = getConfigDataBoolean("check-plugins-per-block", checkPlugins);
		powerAffectsWallWidth = getConfigDataBoolean("power-affects-wall-width", true);
		powerAffectsWallHeight = getConfigDataBoolean("power-affects-wall-height", true);
		powerAffectsWallDuration = getConfigDataBoolean("power-affects-wall-duration", true);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnBreak = initSubspell(spellOnBreakName,
				"WallSpell '" + internalName + "' has an invalid spell-on-break defined!",
				true);

		if (breakListener == null) {
			breakListener = new BreakListener();
			registerEvents(breakListener);
		}
	}

	@Override
	public void turnOff() {
		breakListener = null;
		for (WallData wallData : blockSets.values()) wallData.blockSet.remove();
		blockSets.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (checkAtCap(data)) return noTarget(strAtCap, data);

		int range = getRange(data);

		RayTraceResult result = rayTraceBlocks(data, range);
		if (result != null) return noTarget(data);

		Location location = data.caster().getEyeLocation();
		Vector start = location.toVector();

		location.add(location.getDirection().multiply(range));
		location.setDirection(location.toVector().subtract(start));

		SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, location);
		if (!targetEvent.callEvent()) return noTarget(targetEvent);

		return makeWall(targetEvent.getSpellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		if (checkAtCap(data)) return noTarget(strAtCap, data);
		return makeWall(data);
	}

	private boolean checkAtCap(SpellData data) {
		if (capPerEntity > 0) {
			int count = 0;

			Collection<WallData> wallData = blockSets.get(data.caster().getUniqueId());
			for (WallData wd : wallData) {
				if (wd.wallSpell == this) {
					count++;
					if (count >= capPerEntity) return true;
				}
			}
		}

		return false;
	}

	private CastResult makeWall(SpellData data) {
		if (materials == null || materials.isEmpty())
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location loc = data.location();
		Block target = loc.getBlock();

		if (checkPlugins.get(data) && data.caster() instanceof Player caster) {
			BlockState eventBlockState = target.getState();
			target.setType(materials.get(0), false);

			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(target, eventBlockState, target, caster.getInventory().getItemInMainHand(), caster, true);
			if (!event.callEvent()) return noTarget(data);

			target.setType(Material.AIR, false);
		}

		int yOffset = this.yOffset.get(data);
		if (alwaysOnGround.get(data)) {
			yOffset = 0;

			Block b = target.getRelative(0, -1, 0);
			while (b.getType().isAir() && yOffset > -5) {
				yOffset--;
				b = b.getRelative(0, -1, 0);
			}
		}

		TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, materials, checkPluginsPerBlock.get(data), data.caster());
		Vector dir = loc.getDirection();

		int wallWidth = this.wallWidth.get(data);
		if (powerAffectsWallWidth.get(data)) wallWidth = Math.round(wallWidth * data.power());

		int wallHeight = this.wallHeight.get(data);
		if (powerAffectsWallHeight.get(data)) wallHeight = Math.round(wallHeight * data.power());

		int wallDepth = this.wallDepth.get(data);

		if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
			int depthDir = dir.getX() > 0 ? 1 : -1;
			for (int z = loc.getBlockZ() - (wallWidth / 2); z <= loc.getBlockZ() + (wallWidth / 2); z++) {
				for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
					for (int x = target.getX(); x < target.getX() + wallDepth && x > target.getX() - wallDepth; x += depthDir) {
						blockSet.add(loc.getWorld().getBlockAt(x, y, z));
					}
				}
			}
		} else {
			int depthDir = dir.getZ() > 0 ? 1 : -1;
			for (int x = loc.getBlockX() - (wallWidth / 2); x <= loc.getBlockX() + (wallWidth / 2); x++) {
				for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
					for (int z = target.getZ(); z < target.getZ() + wallDepth && z > target.getZ() - wallDepth; z += depthDir) {
						blockSet.add(loc.getWorld().getBlockAt(x, y, z));
					}
				}
			}
		}

		int wallDuration = this.wallDuration.get(data);
		if (powerAffectsWallDuration.get(data)) wallDuration = Math.round(wallDuration * data.power());

		if (wallDuration > 0) {
			WallData wallData = new WallData(this, blockSet, data.noLocation(), preventDrops.get(data), preventBreaking.get(data));

			blockSets.put(data.caster().getUniqueId(), wallData);
			blockSet.removeAfter(wallDuration, set -> blockSets.remove(data.caster().getUniqueId(), wallData));
		}

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public List<Material> getMaterials() {
		return materials;
	}

	public Subspell getSpellOnBreak() {
		return spellOnBreak;
	}

	public void setSpellOnBreak(Subspell spellOnBreak) {
		this.spellOnBreak = spellOnBreak;
	}

	private static class BreakListener implements Listener {

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		private void onBlockBreak(BlockBreakEvent event) {
			if (blockSets.isEmpty()) return;

			Block block = event.getBlock();

			WallData wallData = null;
			for (WallData data : blockSets.values()) {
				if (!data.blockSet.contains(block)) continue;

				wallData = data;
				break;
			}
			if (wallData == null) return;

			if (wallData.preventBreaking || wallData.preventDrops) {
				event.setCancelled(true);
				if (!wallData.preventBreaking) {
					block.setType(Material.AIR);
					wallData.blockSet.untrack(block);
				}
			} else wallData.blockSet.untrack(block);

			SpellData data = wallData.data;
			if (!data.caster().isValid()) return;

			if (wallData.wallSpell.spellOnBreak != null)
				wallData.wallSpell.spellOnBreak.subcast(data.location(block.getLocation().add(0.5, 0, 0.5)), false, false);
		}

	}

	private record WallData(WallSpell wallSpell, TemporaryBlockSet blockSet, SpellData data, boolean preventDrops, boolean preventBreaking) {
	}

}
