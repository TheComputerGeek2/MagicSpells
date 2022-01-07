package com.nisovin.magicspells.spells;

import java.util.regex.Pattern;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ValidTargetChecker;

public abstract class TargetedSpell extends InstantSpell {

	private static final Pattern chatVarCasterMatchPattern = Pattern.compile("%castervar:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern chatVarTargetMatchPattern = chatVarCasterMatchPattern;

	protected boolean targetSelf;
	protected boolean alwaysActivate;
	protected boolean playFizzleSound;
	
	protected String spellNameOnFail;
	protected Subspell spellOnFail;

	protected String strNoTarget;
	protected String strCastTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetSelf = getConfigBoolean("target-self", false);
		alwaysActivate = getConfigBoolean("always-activate", false);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);

		spellNameOnFail = getConfigString("spell-on-fail", "");

		strNoTarget = getConfigString("str-no-target", "");
		strCastTarget = getConfigString("str-cast-target", "");

	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellNameOnFail.isEmpty()) return;

		spellOnFail = new Subspell(spellNameOnFail);
		if (!spellOnFail.process()) {
			spellOnFail = null;
			MagicSpells.error("Spell '" + internalName + "' has an invalid spell-on-fail defined!");
		}
	}

	public void sendMessages(LivingEntity caster, LivingEntity target) {
		sendMessages(caster, target, null);
	}
	
	public void sendMessages(LivingEntity caster, LivingEntity target, String[] args) {
		String casterName = getTargetName(caster);
		Player playerCaster = null;
		if (caster instanceof Player player) playerCaster = player;

		String targetName = getTargetName(target);
		Player playerTarget = null;
		if (target instanceof Player player) playerTarget = player;

		if (playerCaster != null)
			sendMessage(prepareMessage(strCastSelf, playerCaster, playerTarget), caster, args,
				"%a", casterName, "%t", targetName);

		if (playerTarget != null)
			sendMessage(prepareMessage(strCastTarget, playerCaster, playerTarget), target, args,
				"%a", casterName, "%t", targetName);

		sendMessageNear(caster, playerTarget, prepareMessage(strCastOthers, playerCaster, playerTarget), broadcastRange, args,
			"%a", casterName, "%t", targetName);
	}
	
	private String prepareMessage(String message, Player caster, Player playerTarget) {
		if (message == null || message.isEmpty()) return message;

		message = MagicSpells.doTargetedVariableReplacements(caster, playerTarget, message);

		return message;
	}
	
	protected String getTargetName(LivingEntity target) {
		if (target instanceof Player) return target.getName();
		String name = MagicSpells.getEntityNames().get(target.getType());
		if (name != null) return name;
		return "unknown";
	}
	
	/**
	 * Checks whether two locations are within a certain distance from each other.
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @param range The maximum distance
	 * @return true if the distance is less than the range, false otherwise
	 */
	protected boolean inRange(Location loc1, Location loc2, int range) {
		return loc1.distanceSquared(loc2) < range * range;
	}
	
	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(LivingEntity livingEntity) {
		if (!playFizzleSound || !(livingEntity instanceof Player player)) return;
		player.playEffect(livingEntity.getLocation(), Effect.EXTINGUISH, null);
	}
	
	@Override
	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		if (targetSelf || validTargetList.canTargetSelf()) return new TargetInfo<>(caster, power);
		return super.getTargetedEntity(caster, power, forceTargetPlayers, checker);
	}

	@Override
	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker, String[] args) {
		if (targetSelf || validTargetList.canTargetSelf()) return new TargetInfo<>(caster, power);
		return super.getTargetedEntity(caster, power, forceTargetPlayers, checker, args);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 * @param livingEntity the casting living entity
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(LivingEntity livingEntity) {
		return noTarget(livingEntity, strNoTarget);
	}
	
	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param livingEntity the casting living entity
	 * @param message the message to send
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(LivingEntity livingEntity, String message) {
		fizzle(livingEntity);
		sendMessage(message, livingEntity, MagicSpells.NULL_ARGS);
		if (spellOnFail != null) spellOnFail.cast(livingEntity, 1.0F);
		return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
	}
	
}
