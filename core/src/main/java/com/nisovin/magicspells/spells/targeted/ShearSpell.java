package com.nisovin.magicspells.spells.targeted;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.entity.Shearable;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ShearSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker SHEARABLE = entity -> entity instanceof Shearable shearable && shearable.readyToBeSheared();

	private final ConfigData<DyeColor> woolColor;

	private final ConfigData<Integer> minWool;
	private final ConfigData<Integer> maxWool;

	private final ConfigData<Double> dropOffset;

	private final ConfigData<Boolean> forceWoolColor;
	private final ConfigData<Boolean> randomWoolColor;
	private final ConfigData<Boolean> vanillaSheepShearing;

	public ShearSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		woolColor = getConfigDataEnum("wool-color", DyeColor.class, null);

		minWool = getConfigDataInt("min-wool-drop", 1);
		maxWool = getConfigDataInt("max-wool-drop", 3);

		dropOffset = getConfigDataDouble("drop-offset", 1);

		forceWoolColor = getConfigDataBoolean("force-wool-color", false);
		randomWoolColor = getConfigDataBoolean("random-wool-color", false);
		vanillaSheepShearing = getConfigDataBoolean("vanilla-sheep-shearing", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data, SHEARABLE);
		if (info.noTarget()) return noTarget(info);

		return shear((Shearable) info.target(), info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!SHEARABLE.isValidTarget(data.target())) return noTarget(data);

		return shear((Shearable) data.target(), data);
	}

	private CastResult shear(Shearable shearable, SpellData data) {
		if (shearable instanceof Sheep sheep && !vanillaSheepShearing.get(data)) {
			DyeColor color;
			if (forceWoolColor.get(data)) {
				if (randomWoolColor.get(data)) {
					DyeColor[] colors = DyeColor.values();
					color = colors[random.nextInt(colors.length)];
				} else {
					color = woolColor.get(data);
					if (color == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
				}
			} else {
				color = sheep.getColor();
				if (color == null) color = DyeColor.WHITE;
			}

			Material wool = Material.getMaterial(color.name() + "_WOOL");
			if (wool == null) wool = Material.WHITE_WOOL;

			int minWool = this.minWool.get(data);
			int maxWool = this.maxWool.get(data);

			int count = 1;
			if (minWool >= 0) {
				if (maxWool >= 0) count = random.nextInt((maxWool - minWool) + 1) + minWool;
				else count = random.nextInt(minWool + 1);
			}

			sheep.setSheared(true);
			sheep.getWorld().dropItemNaturally(sheep.getLocation().add(0, dropOffset.get(data), 0), new ItemStack(wool, count));
		} else shearable.shear();

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return SHEARABLE;
	}

}
