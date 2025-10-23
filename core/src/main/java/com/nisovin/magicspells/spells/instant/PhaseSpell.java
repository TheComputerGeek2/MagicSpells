package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class PhaseSpell extends InstantSpell {

	private final List<Material> phasableBlocks;
	private final List<Material> nonPhasableBlocks;

	private final ConfigData<Integer> maxDistance;
	private final ConfigData<Boolean> powerAffectsMaxDistance;
	private String strCantPhase;

	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxDistance = getConfigDataInt("max-distance", 15);
		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");
		powerAffectsMaxDistance = getConfigDataBoolean("power-affects-max-distance", true);

		phasableBlocks = new ArrayList<>();
		nonPhasableBlocks = new ArrayList<>();

		processMaterials(phasableBlocks, "phasable-blocks");
		processMaterials(nonPhasableBlocks, "non-phasable-blocks");
	}

	private void processMaterials(List<Material> materials, String path) {
		List<String> matList = getConfigStringList(path, null);
		if (matList == null || matList.isEmpty()) return;
		for (String mat : matList) {
			Material material = Util.getMaterial(mat);
			if (material == null) {
				MagicSpells.error("PhaseSpell has an invalid material specified on '" + path + "': " + mat);
				continue;
			}
			materials.add(material);
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		int r = getRange(data);

		int distance = maxDistance.get(data);
		if (powerAffectsMaxDistance.get(data)) distance = Math.round(distance * data.power());

		BlockIterator iter;
		try {
			iter = new BlockIterator(data.caster(), distance << 1);
		} catch (IllegalStateException e) {
			sendMessage(strCantPhase, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		int i = 0;
		Block start = null;
		Location location = null, casterLoc = data.caster().getLocation();

		while (i++ < r << 1 && iter.hasNext()) {
			Block b = iter.next();
			if (b.getType().isAir()) continue;
			if (casterLoc.distanceSquared(b.getLocation()) >= r * r) continue;
			start = b;
			break;
		}

		if (start != null) {
			if (canPassThrough(start)) {
				while (i++ < distance << 1 && iter.hasNext()) {
					Block block = iter.next();
					if (block.getType().isAir() && block.getRelative(0, 1, 0).getType().isAir() && casterLoc.distanceSquared(block.getLocation()) < distance * distance) {
						location = block.getLocation();
						break;
					}
					if (!canPassThrough(block)) break;
				}
			}
		}

		if (location == null) {
			sendMessage(strCantPhase, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		location.setX(location.getX() + 0.5);
		location.setZ(location.getZ() + 0.5);
		location.setPitch(casterLoc.getPitch());
		location.setYaw(casterLoc.getYaw());
		data = data.location(location);

		data.caster().teleportAsync(location);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private boolean canPassThrough(Block block) {
		// Check only blacklist.
		if (phasableBlocks.isEmpty()) return !nonPhasableBlocks.contains(block.getType());
		return phasableBlocks.contains(block.getType()) && !nonPhasableBlocks.contains(block.getType());
	}

	public List<Material> getPhasableBlocks() {
		return phasableBlocks;
	}

	public List<Material> getNonPhasableBlocks() {
		return nonPhasableBlocks;
	}

	public String getStrCantPhase() {
		return strCantPhase;
	}

	public void setStrCantPhase(String strCantPhase) {
		this.strCantPhase = strCantPhase;
	}

}
