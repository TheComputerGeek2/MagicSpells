package com.nisovin.magicspells.spells.targeted;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

//This spell currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

public class RegrowSpell extends TargetedSpell implements TargetedEntitySpell {

	private DyeColor dye;

	private Random random;

	private String requestedColor;

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private boolean configuredCorrectly;

	public RegrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requestedColor = getConfigString("wool-color", "");

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);

		random = ThreadLocalRandom.current();
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseSpell();
		if (!configuredCorrectly) MagicSpells.error("RegrowSpell " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target == null) return PostCastAction.ALREADY_HANDLED;
			if (!(target.getTarget() instanceof Sheep sheep)) return PostCastAction.ALREADY_HANDLED;

			boolean done = grow(sheep);
			if (!done) return noTarget(caster);

			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Sheep sheep)) return false;
		return grow(sheep);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Sheep sheep)) return false;
		return grow(sheep);
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

	private boolean parseSpell() {
		if (forceWoolColor && !requestedColor.isEmpty()) {
			try {
				dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid wool color defined. Will use sheep's color instead.");
				return false;
			}
		}
		return true;
	}

}
