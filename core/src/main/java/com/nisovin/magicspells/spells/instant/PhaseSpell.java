package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class PhaseSpell extends InstantSpell {

	private final ConfigData<Integer> maxDistance;
	private final ConfigData<Integer> requireGroundDistance;

	private final ConfigData<Boolean> airOnlyExit;
	private final ConfigData<Boolean> snapToGround;
	private final ConfigData<Boolean> ignoreTransparentBlocks;
	private final ConfigData<Boolean> powerAffectsMaxDistance;

	private final String strCantPhase;

	private final List<Material> enterBlocks = new ArrayList<>();
	private final List<Material> nonExitBlocks = new ArrayList<>();
	private final List<Material> phasableBlocks = new ArrayList<>();
	private final List<Material> nonPhasableBlocks = new ArrayList<>();

	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxDistance = getConfigDataInt("max-distance", 15);
		requireGroundDistance = getConfigDataInt("require-ground-distance", 0);

		airOnlyExit = getConfigDataBoolean("air-only-exit", true);
		snapToGround = getConfigDataBoolean("snap-to-ground", false);
		ignoreTransparentBlocks = getConfigDataBoolean("ignore-transparent-blocks", false);
		powerAffectsMaxDistance = getConfigDataBoolean("power-affects-max-distance", true);

		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");

		processMaterials(enterBlocks, "enter-blocks");
		processMaterials(nonExitBlocks, "non-exit-blocks");
		processMaterials(phasableBlocks, "phasable-blocks");
		processMaterials(nonPhasableBlocks, "non-phasable-blocks");
	}

	private void processMaterials(List<Material> materials, String path) {
		for (String mat : getConfigStringList(path, List.of())) {
			Material material = Util.getMaterial(mat);
			if (material == null) {
				MagicSpells.error("PhaseSpell '" + internalName + "' has an invalid material specified on '" + path + "': " + mat);
				continue;
			}
			materials.add(material);
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		int range = getRange(data);
		int rangeSquared = range * range;
		Location casterLoc = data.caster().getLocation();

		int distance = maxDistance.get(data);
		if (powerAffectsMaxDistance.get(data)) distance = Math.round(distance * data.power());
		int distanceSquared = distance * distance;

		BlockIterator iter;
		try {
			iter = new BlockIterator(data.caster(), distance);
		} catch (IllegalStateException e) {
			sendMessage(strCantPhase, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Predicate<Location> transparent = ignoreTransparentBlocks.get(data) ? isTransparent(data) : l -> l.getBlock().isEmpty();

		while (iter.hasNext()) {
			Block b = iter.next();
			Location loc = b.getLocation();

			if (enterBlocks.contains(b.getType())) break;
			if (transparent.test(loc)) continue;

			if (casterLoc.distanceSquared(loc) < rangeSquared && canPassThrough(b)) break;

			sendMessage(strCantPhase, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Location location = null;
		boolean airOnlyExit = this.airOnlyExit.get(data);
		boolean snapToGround = this.snapToGround.get(data);
		int requireGroundDistance = this.requireGroundDistance.get(data);

		while (iter.hasNext()) {
			Block block = iter.next();
			Location loc = block.getLocation().add(0.5, 0, 0.5);

			if (casterLoc.distanceSquared(loc) >= distanceSquared) break;

			Location adjusted = BlockUtils.adjustToSafeLocation(data.caster(), loc, requireGroundDistance, snapToGround);
			if (adjusted == null) continue;

			BoundingBox box = data.caster().getBoundingBox();
			box.shift(adjusted.clone().subtract(casterLoc));
			if (!Util.hasCollisionsIn(data.caster().getWorld(), box, false, FluidCollisionMode.NEVER,
				b -> (airOnlyExit && !b.isEmpty()) || nonExitBlocks.contains(b.getType())
			)) {
				location = adjusted;
				break;
			}

			if (!canPassThrough(adjusted.getBlock())) break;
		}

		if (location == null) {
			sendMessage(strCantPhase, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		location.setPitch(casterLoc.getPitch());
		location.setYaw(casterLoc.getYaw());
		data = data.location(location);

		data.caster().teleportAsync(location);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private boolean canPassThrough(Block block) {
		Material type = block.getType();
		return !nonPhasableBlocks.contains(type) && (phasableBlocks.isEmpty() || phasableBlocks.contains(type));
	}

}
