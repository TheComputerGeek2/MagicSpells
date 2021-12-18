package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PhaseSpell extends InstantSpell {

	private final List<Material> phasableBlocks;
	private final List<Material> nonPhasableBlocks;

	private ConfigData<Integer> maxDistance;
	private boolean powerAffectsMaxDistance;
	private String strCantPhase;

	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxDistance = getConfigDataInt("max-distance", 15);
		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");
		powerAffectsMaxDistance = getConfigBoolean("power-affects-max-distance", true);

		phasableBlocks = new ArrayList<>();
		nonPhasableBlocks = new ArrayList<>();

		processMaterials(phasableBlocks,"phasable-blocks");
		processMaterials(nonPhasableBlocks,"non-phasable-blocks");
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int r = getRange(caster, power, args);

			int distance = maxDistance.get(caster, null, power, args);
			if (powerAffectsMaxDistance) distance = Math.round(distance * power);

			BlockIterator iter;
			try {
				iter = new BlockIterator(caster, distance << 1);
			} catch (IllegalStateException e) {
				sendMessage(strCantPhase, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			int i = 0;
			Block start = null;
			Location location = null;

			while (i++ < r << 1 && iter.hasNext()) {
				Block b = iter.next();
				if (BlockUtils.isAir(b.getType())) continue;
				if (caster.getLocation().distanceSquared(b.getLocation()) >= r * r) continue;
				start = b;
				break;
			}

			if (start != null) {
				if (canPassThrough(start)) {
					while (i++ < distance << 1 && iter.hasNext()) {
						Block block = iter.next();
						if (BlockUtils.isAir(block.getType()) && BlockUtils.isAir(block.getRelative(0, 1, 0).getType()) && caster.getLocation().distanceSquared(block.getLocation()) < distance * distance) {
							location = block.getLocation();
							break;
						}
						if (!canPassThrough(block)) break;
					}
				}
			}

			if (location == null) {
				sendMessage(strCantPhase, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + 0.5);
			location.setPitch(caster.getLocation().getPitch());
			location.setYaw(caster.getLocation().getYaw());
			playSpellEffects(EffectPosition.CASTER, caster.getLocation());
			playSpellEffects(EffectPosition.TARGET, location);
			caster.teleport(location);
		}
		return PostCastAction.HANDLE_NORMALLY;
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