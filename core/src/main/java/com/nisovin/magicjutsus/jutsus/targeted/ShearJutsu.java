package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Sheep;
import org.bukkit.material.Wool;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

//This jutsu currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

public class ShearJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Random random;

	private DyeColor dye;

	private String requestedColor;

	private int minWool;
	private int maxWool;

	private double dropOffset;

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private boolean configuredCorrectly;

	public ShearJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		random = new Random();

		requestedColor = getConfigString("wool-color", "");

		minWool = getConfigInt("min-wool-drop", 1);
		maxWool = getConfigInt("max-wool-drop", 3);

		dropOffset = getConfigDouble("drop-offset", 1);

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseJutsu();
		if (!configuredCorrectly) MagicJutsus.error("ShearJutsu " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return PostCastAction.ALREADY_HANDLED;
			if (!(target.getTarget() instanceof Sheep)) return PostCastAction.ALREADY_HANDLED;

			boolean done = shear((Sheep) target.getTarget());
			if (!done) return noTarget(livingEntity);

			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return shear((Sheep) target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return shear((Sheep) target);
	}

	private boolean parseJutsu() {
		if (forceWoolColor && requestedColor != null) {
			try {
			  dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicJutsus.error("Invalid wool color defined. Will use sheep's color instead.");
				requestedColor = null;
				return false;
			}
		}
		return true;
	}

	private DyeColor randomizeDyeColor() {
		DyeColor[] allDyes = DyeColor.values();
		int dyePosition = random.nextInt(allDyes.length);
		return allDyes[dyePosition];
	}

	private boolean shear(Sheep sheep) {
		if (!configuredCorrectly) return false;
		if (sheep.isSheared()) return false;
		if (!sheep.isAdult()) return false;
		
		DyeColor color = sheep.getColor();

		Location location = sheep.getLocation();
		Wool wool;
		ItemStack woolBlock;
		int count;

		if (forceWoolColor && !randomWoolColor && dye != null) wool = new Wool(dye);
		else if (forceWoolColor && randomWoolColor) wool = new Wool(randomizeDyeColor());
		else wool = new Wool(color);

		woolBlock = wool.toItemStack(1);

		if (maxWool != 0) count = random.nextInt((maxWool - minWool) + 1) + minWool;
		else count = random.nextInt(minWool + 1);

		sheep.setSheared(true);
		location.add(0, dropOffset, 0);

		for (int i = 0; i < count; i++) {
			sheep.getWorld().dropItemNaturally(location, woolBlock);
		}

		return true;
	}

}
