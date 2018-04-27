package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.Wool;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

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
ShearSpell:
	force-wool-color: false
	wool-color: white
	wool-count: 1-3
	drop-offset: 1
*/

public class ShearSpell extends TargetedSpell implements TargetedEntitySpell {

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private String requestedColor;
	private DyeColor dye;
	private int minWool;
	private int maxWool;
	private double dropOffset;

	private Random random = new Random();

	private boolean configuredCorrectly;

	public ShearSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);
		requestedColor = getConfigString("wool-color", null);
		minWool = getConfigInt("min-wool-drop", 1);
		maxWool = getConfigInt("max-wool-drop", 3);
		dropOffset = getConfigDouble("drop-offset", 1.0D);
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseSpell();
		if (!configuredCorrectly) MagicSpells.error("ShearSpell " + internalName + " was configured incorrectly!");

	}

	public boolean parseSpell() {
		if (forceWoolColor && requestedColor != null) {
			try {
			  dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid wool color defined. Will use sheep's color instead.");
				requestedColor = null;
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

			boolean done = shear((Sheep)target.getTarget());
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

	private boolean shear(Sheep sheep) {
		if (!configuredCorrectly) return false;
		//tf, why are you trying to shear naked skeep?!?
		if (sheep.isSheared()) return false;
		//Animal cruelty much, you can barely get half a block of wool from baby sheep.
		if (!sheep.isAdult()) return false;
		
		//Make a variable for the sheep's color if we need it.
		DyeColor color = sheep.getColor();

		Location location = sheep.getLocation();
		Wool wool;
		ItemStack woolBlock;
		int count;

		if (forceWoolColor && !randomWoolColor && dye != null) wool = new Wool(dye);
		else if (forceWoolColor && randomWoolColor) wool = new Wool(randomizeDyeColor());
		else wool = new Wool(color);

		//Lets set the woolBlock's data to wool
		woolBlock = wool.toItemStack(1);

		//We need to choose a range between whateve	r they set.
		if (maxWool != 0) count = random.nextInt((maxWool - minWool) + 1) + minWool;
		//Didn't set a range? Just choose they minWoolimum. E.G wool-count: 3; minWool = 3;
		else count = random.nextInt(minWool + 1);

		//Shear dat boi
		sheep.setSheared(true);

		//Adjust for dropOffset
		location.add(0, dropOffset, 0);

		//Where the sheep is, lets spawn some items.
		for (int i = 0; i < count; i++) {
			sheep.getWorld().dropItemNaturally(location, woolBlock);
		}
		return true;
	}

	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return shear((Sheep)target);
	}

	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Sheep)) return false;
		return shear((Sheep)target);
	}
}
