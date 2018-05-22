package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public class RotateSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	boolean randomAngle;
	boolean faceTarget;
	boolean faceCaster;
	boolean affectPitch;
	boolean mimicDirection;
	int rotationYaw;
	int rotationPitch;

	Random randomizer = new Random();

	public RotateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		randomAngle = getConfigBoolean("random", false);
		faceTarget = getConfigBoolean("face-target", false);
		faceCaster = getConfigBoolean("face-caster", false);
		mimicDirection = getConfigBoolean("mimic-direction", false);
		//If this is true, this also applies for random rotations
		affectPitch = getConfigBoolean("affect-pitch", false);
		rotationYaw = getConfigInt("rotation", 10);
		//affect pitch must be true for this to have any use
		rotationPitch = getConfigInt("rotation-pitch", 0);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			spin(player, target.getTarget());
			playSpellEffects(player, target.getTarget());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	//Basic spin for a single target. This was there to begin with.
	private void spin(LivingEntity target) {
		Location loc = target.getLocation();
		//Affect Pitch must be added to affect the pitch of a target.
		if (randomAngle) {
			loc.setYaw(Util.getRandomInt(360));
			if (affectPitch) loc.setPitch(randomizer.nextInt(181) - 90);
		} else {
			loc.setYaw(loc.getYaw() + rotationYaw);
			if (affectPitch) loc.setPitch(loc.getPitch() + rotationPitch);
		}
		//Finally teleport the target so they have the new Pitch and Yaw
		target.teleport(loc);
	}

	//If I'm spinning an entity relative to another entity.
	private void spin(LivingEntity caster, LivingEntity target) {
		//Get the directions of the caster and target.
		Location targetLoc = target.getLocation();
		Location casterLoc = caster.getLocation();

		//If they want to make the caster face the target they targeted.
		if (faceTarget) caster.teleport(changeDirection(casterLoc, targetLoc));
		//If they want to make the target face the caster, lets teleport the target.
		else if (faceCaster) target.teleport(changeDirection(targetLoc, casterLoc));
		//If there are no face options, we'll just spin the target normally.
		else spin(target);
	}

	//If I'm spinning an entity relative to a location and nothing else.
	private void spin(LivingEntity entity, Location target) {
		entity.teleport(changeDirection(entity.getLocation(), target));
	}

	/*The main function that tries to process the directions sent through.
	It will always process the caster location relative to the target*/
	private Location changeDirection(Location caster, Location target) {
		//A cloned location so that I don't tamper with the caster's.
		Location loc = caster.clone();
		//Mimicing the direction of the target.
		if (mimicDirection) {
			//Set the Yaw and Pitch of the target to be the same as the caster's
			if (affectPitch) loc.setPitch(target.getPitch());
			loc.setYaw(target.getYaw());

		} else {
			//If I'm not mimicing anything, I'm going to face the target location
			loc.setDirection(getVectorDir(caster, target));
			//I'll face in the direction of the target but still maintain the pitch if it should be unaffected
			if (!affectPitch) loc.setPitch(caster.getPitch());
		}
		return loc;
	}

	//A function that allows me to properly get the direction of a target relative to a caster.
	private Vector getVectorDir(Location caster, Location target) {
		return target.clone().subtract(caster.toVector()).toVector();
	}

	//Cast Methods
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		spin(caster, target);
		playSpellEffects(caster, target);
		return true;
	}

	public boolean castAtEntity(LivingEntity target, float power) {
		spin(target);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	public boolean castAtLocation(Player caster, Location target, float power) {
		spin(caster, target);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	public boolean castAtLocation(Location target, float power) {
		return false;
	}

}
