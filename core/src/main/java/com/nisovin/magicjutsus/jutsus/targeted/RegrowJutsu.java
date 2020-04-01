package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

//This jutsu currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

public class RegrowJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private DyeColor dye;

	private Random random;

	private String requestedColor;

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private boolean configuredCorrectly;

	public RegrowJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		requestedColor = getConfigString("wool-color", "");

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);

		random = new Random();
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseJutsu();
		if (!configuredCorrectly) MagicJutsus.error("RegrowJutsu " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return PostCastAction.ALREADY_HANDLED;
			if (!(target.getTarget() instanceof Sheep)) return PostCastAction.ALREADY_HANDLED;

			boolean done = grow((Sheep) target.getTarget());
			if (!done) return noTarget(livingEntity);

			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return grow((Sheep) target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return grow((Sheep) target);
	}

	private boolean grow(Sheep sheep) {
		if (!configuredCorrectly) return false;
		if (!sheep.isSheared()) return false;
		if (!sheep.isAdult()) return false;

		//If we are forcing a specific random wool color, lets set its color to this.
		if (forceWoolColor && randomWoolColor) sheep.setColor(randomizeDyeColor());
		else if (forceWoolColor && dye != null) sheep.setColor(dye);

		sheep.setSheared(false);
		return true;
	}

	private DyeColor randomizeDyeColor() {
		DyeColor[] allDyes = DyeColor.values();
		int dyePosition = random.nextInt(allDyes.length);
		return allDyes[dyePosition];
	}

	private boolean parseJutsu() {
		if (forceWoolColor && !requestedColor.isEmpty()) {
			try {
				dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicJutsus.error("Invalid wool color defined. Will use sheep's color instead.");
				return false;
			}
		}
		return true;
	}

}
