package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.TemporaryBlockSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class WallSpell extends InstantSpell implements TargetedLocationSpell {

	private final Map<UUID, TemporaryBlockSet> blockSets;

	private final List<Material> materials;

	private String strNoTarget;

	private final String spellOnBreakName;

	private Subspell spellOnBreak;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> distance;
	private ConfigData<Integer> wallWidth;
	private ConfigData<Integer> wallDepth;
	private ConfigData<Integer> wallHeight;
	private ConfigData<Integer> wallDuration;

	private boolean checkPlugins;
	private boolean preventDrops;
	private boolean alwaysOnGround;
	private boolean preventBreaking;
	private boolean checkPluginsPerBlock;
	private boolean powerAffectsWallWidth;
	private boolean powerAffectsWallHeight;
	private boolean powerAffectsWallDuration;

	public WallSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockSets = new HashMap<>();

		materials = new ArrayList<>();

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
		distance = getConfigDataInt("distance", 3);
		wallWidth = getConfigDataInt("wall-width", 5);
		wallDepth = getConfigDataInt("wall-depth", 1);
		wallHeight = getConfigDataInt("wall-height", 3);
		wallDuration = getConfigDataInt("wall-duration", 15);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventDrops = getConfigBoolean("prevent-drops", true);
		alwaysOnGround = getConfigBoolean("always-on-ground", false);
		preventBreaking = getConfigBoolean("prevent-breaking", false);
		checkPluginsPerBlock = getConfigBoolean("check-plugins-per-block", checkPlugins);
		powerAffectsWallWidth = getConfigBoolean("power-affects-wall-width", true);
		powerAffectsWallHeight = getConfigBoolean("power-affects-wall-height", true);
		powerAffectsWallDuration = getConfigBoolean("power-affects-wall-duration", true);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnBreak = new Subspell(spellOnBreakName);
		if (!spellOnBreak.process()) {
			if (!spellOnBreakName.isEmpty()) MagicSpells.error("WallSpell '" + internalName + "' has an invalid spell-on-break defined!");
			spellOnBreak = null;
		}

		if (preventBreaking || preventDrops || spellOnBreak != null) registerEvents(new BreakListener());
	}

	@Override
	public void turnOff() {
		for (TemporaryBlockSet set : blockSets.values()) {
			set.remove();
		}

		blockSets.clear();
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (materials == null || materials.isEmpty()) return PostCastAction.ALREADY_HANDLED;

			int distance = this.distance.get(caster, null, power, args);
			Block target = getTargetedBlock(caster, distance > 0 && distance < 15 ? distance : 3);
			if (target == null || !BlockUtils.isAir(target.getType())) {
				sendMessage(strNoTarget, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			makeWall(caster, target.getLocation(), caster.getLocation().getDirection(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		makeWall(caster, target, target.getDirection(), power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		makeWall(caster, target, target.getDirection(), power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void makeWall(LivingEntity caster, Location location, Vector direction, float power, String[] args) {
		if (blockSets.containsKey(caster.getUniqueId())) return;
		if (materials == null || materials.isEmpty()) return;
		if (location == null || direction == null) return;

		Block target = location.getBlock();

		if (checkPlugins && caster instanceof Player player) {
			BlockState eventBlockState = target.getState();
			target.setType(materials.get(0), false);

			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(target, eventBlockState, target, player.getInventory().getItemInMainHand(), player, true);
			EventUtil.call(event);

			if (event.isCancelled()) {
				sendMessage(caster, strNoTarget);
				return;
			}

			BlockUtils.setTypeAndData(target, Material.AIR, Material.AIR.createBlockData(), false);
		}

		int yOffset = this.yOffset.get(caster, null, power, args);
		if (alwaysOnGround) {
			yOffset = 0;

			Block b = target.getRelative(0, -1, 0);
			while (BlockUtils.isAir(b.getType()) && yOffset > -5) {
				yOffset--;
				b = b.getRelative(0, -1, 0);
			}
		}

		TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, materials, checkPluginsPerBlock, caster);
		Location loc = target.getLocation();
		Vector dir = direction.clone();

		int wallWidth = this.wallWidth.get(caster, null, power, args);
		if (powerAffectsWallWidth) wallWidth = Math.round(wallWidth * power);

		int wallHeight = this.wallHeight.get(caster, null, power, args);
		if (powerAffectsWallHeight) wallHeight = Math.round(wallHeight * power);

		int wallDepth = this.wallDepth.get(caster, null, power, args);

		if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
			int depthDir = dir.getX() > 0 ? 1 : -1;
			for (int z = loc.getBlockZ() - (wallWidth / 2); z <= loc.getBlockZ() + (wallWidth / 2); z++) {
				for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
					for (int x = target.getX(); x < target.getX() + wallDepth && x > target.getX() - wallDepth; x += depthDir) {
						blockSet.add(caster.getWorld().getBlockAt(x, y, z));
					}
				}
			}
		} else {
			int depthDir = dir.getZ() > 0 ? 1 : -1;
			for (int x = loc.getBlockX() - (wallWidth / 2); x <= loc.getBlockX() + (wallWidth / 2); x++) {
				for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
					for (int z = target.getZ(); z < target.getZ() + wallDepth && z > target.getZ() - wallDepth; z += depthDir) {
						blockSet.add(caster.getWorld().getBlockAt(x, y, z));
					}
				}
			}
		}

		int wallDuration = this.wallDuration.get(caster, null, power, args);
		if (powerAffectsWallDuration) wallDuration = Math.round(wallDuration * power);

		if (wallDuration > 0) {
			blockSets.put(caster.getUniqueId(), blockSet);
			blockSet.removeAfter(wallDuration, (TemporaryBlockSet set) -> blockSets.remove(caster.getUniqueId()));
		}

		playSpellEffects(EffectPosition.CASTER, caster);
	}

	private class BreakListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		private void onBlockBreak(BlockBreakEvent event) {
			if (blockSets.isEmpty()) return;
			Block block = event.getBlock();
			Player player = event.getPlayer();

			Player caster = null;
			TemporaryBlockSet set = null;
			for (TemporaryBlockSet blockSet : blockSets.values()) {
				if (!blockSet.contains(block)) continue;
				set = blockSet;
				event.setCancelled(true);
				if (!preventBreaking) block.setType(Material.AIR);
			}

			for (UUID id : blockSets.keySet()) {
				if (!blockSets.get(id).equals(set)) continue;
				caster = Bukkit.getPlayer(id);
				if (caster == null) return;
				if (!caster.isOnline()) return;
			}

			if (spellOnBreak == null) return;
			if (spellOnBreak.isTargetedEntityFromLocationSpell()) {
				spellOnBreak.castAtEntityFromLocation(caster, block.getLocation().add(0.5, 0, 0.5), player, 1F);
			} else if (spellOnBreak.isTargetedEntitySpell()) {
				spellOnBreak.castAtEntity(caster, player, 1F);
			} else if (spellOnBreak.isTargetedLocationSpell()) {
				spellOnBreak.castAtLocation(caster, block.getLocation().add(0.5, 0, 0.5), 1F);
			} else {
				spellOnBreak.cast(caster, 1F);
			}
		}
		
	}

	public Map<UUID, TemporaryBlockSet> getBlockSets() {
		return blockSets;
	}

	public List<Material> getMaterials() {
		return materials;
	}

	public String getStrNoTarget() {
		return strNoTarget;
	}

	public void setStrNoTarget(String strNoTarget) {
		this.strNoTarget = strNoTarget;
	}

	public Subspell getSpellOnBreak() {
		return spellOnBreak;
	}

	public void setSpellOnBreak(Subspell spellOnBreak) {
		this.spellOnBreak = spellOnBreak;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

	public boolean shouldPreventDrop() {
		return preventDrops;
	}

	public void setPreventDrops(boolean preventDrops) {
		this.preventDrops = preventDrops;
	}

	public boolean isAlwaysOnGround() {
		return alwaysOnGround;
	}

	public void setAlwaysOnGround(boolean alwaysOnGround) {
		this.alwaysOnGround = alwaysOnGround;
	}

	public boolean shouldPreventBreaking() {
		return preventBreaking;
	}

	public void setPreventBreaking(boolean preventBreaking) {
		this.preventBreaking = preventBreaking;
	}

	public boolean shouldCheckPluginsPerBlock() {
		return checkPluginsPerBlock;
	}

	public void setCheckPluginsPerBlock(boolean checkPluginsPerBlock) {
		this.checkPluginsPerBlock = checkPluginsPerBlock;
	}

}
