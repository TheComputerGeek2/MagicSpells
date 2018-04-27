package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

//This spell currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

/*
#Example Configuration.
RegrowSpell:
	force-wool-color: true
	wool-color: white
*/

public class RegrowSpell extends TargetedSpell implements TargetedEntitySpell {

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private String requestedColor;
	private DyeColor dye;
	private Random random = new Random();

	private boolean configuredCorrectly;

	public RegrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);
		requestedColor = getConfigString("wool-color", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseSpell();
		if (!configuredCorrectly) MagicSpells.error("Regrow Spell " + internalName + " was configured incorrectly!");

	}

	public boolean parseSpell() {
		if (forceWoolColor && requestedColor != null) {
			try {
			  	dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid wool color defined. Will use sheep's color instead.");
				return false;
			}
		}
		return true;
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return PostCastAction.ALREADY_HANDLED;
			if (!(target.getTarget() instanceof Sheep)) return PostCastAction.ALREADY_HANDLED;

			boolean done = grow((Sheep)target.getTarget());
			if (!done) return noTarget(player);

			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private DyeColor randomizeDyeColor() {
		DyeColor[] allDyes = DyeColor.values();
		int dyePosition = random.nextInt(allDyes.length);
		return allDyes[dyePosition];
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

	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return grow((Sheep)target);
	}

	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return grow((Sheep)target);
	}
}
