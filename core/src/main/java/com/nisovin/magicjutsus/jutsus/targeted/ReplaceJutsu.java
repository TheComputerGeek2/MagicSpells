package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ReplaceJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Map<Block, Material> blocks;

	private List<Material> replace;
	private List<Material> replaceWith;

	private Random random;

	private int yOffset;
	private int radiusUp;
	private int radiusDown;
	private int radiusHoriz;
	private int replaceDuration;

	private boolean pointBlank;
	private boolean replaceRandom;
	private boolean powerAffectsRadius;
	
	public ReplaceJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		blocks = new HashMap<>();
		replace = new ArrayList<>();
		replaceWith = new ArrayList<>();

		random = new Random();
				
		yOffset = getConfigInt("y-offset", 0);
		radiusUp = getConfigInt("radius-up", 1);
		radiusDown = getConfigInt("radius-down", 1);
		radiusHoriz = getConfigInt("radius-horiz", 1);
		replaceDuration = getConfigInt("duration", 0);
		if (replaceDuration < 0) replaceDuration = 0;

		pointBlank = getConfigBoolean("point-blank", false);
		replaceRandom = getConfigBoolean("replace-random", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);

		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) {
					MagicJutsus.error("ReplaceJutsu " + internalName + " has an invalid replace-blocks item: " + s);
					continue;
				}

				replace.add(material);
			}
		}

		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) {
					MagicJutsus.error("ReplaceJutsu " + internalName + " has an invalid replace-with item: " + s);
					continue;
				}

				replaceWith.add(material);
			}
		}
		
		if (!replaceRandom && replace.size() != replaceWith.size()) {
			replaceRandom = true;
			MagicJutsus.error("ReplaceJutsu " + internalName + " replace-random false, but replace-blocks and replace-with have different sizes!");
		}
		
		if (replace.isEmpty()) MagicJutsus.error("ReplaceJutsu " + internalName + " has empty replace-blocks list!");
		if (replaceWith.isEmpty()) MagicJutsus.error("ReplaceJutsu " + internalName + " has empty replace-with list!");
	}

	@Override
	public void turnOff() {
		for (Block b : blocks.keySet()) {
			b.setType(blocks.get(b));
		}

		blocks.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Block target = pointBlank ? livingEntity.getLocation().getBlock() : getTargetedBlock(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			replace(livingEntity, target.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return replace(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return replace(null, target, power);
	}

	private boolean replace(LivingEntity caster, Location target, float power) {
		boolean replaced = false;
		Block block;

		int d = powerAffectsRadius ? Math.round(radiusDown * power) : radiusDown;
		int u = powerAffectsRadius ? Math.round(radiusUp * power) : radiusUp;
		int h = powerAffectsRadius ? Math.round(radiusHoriz * power) : radiusHoriz;

		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						if (!replace.get(i).equals(block.getType())) continue;

						blocks.put(block, block.getType());
						Block finalBlock = block;
						if (replaceDuration > 0) MagicJutsus.scheduleDelayedTask(() -> {
							finalBlock.setType(blocks.get(finalBlock));
							blocks.remove(finalBlock);
						}, replaceDuration);

						if (replaceRandom) block.setType(replaceWith.get(random.nextInt(replaceWith.size())));
						else block.setType(replaceWith.get(i));

						replaced = true;
						break;
					}
				}
			}
		}

		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);

		return replaced;
	}

}
