package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.ExperienceUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.util.TargetInfo;

/**
 * public class DrainlifeSpell extends {@link TargetedSpell} implements {@link TargetedEntitySpell}, {@link SpellDamageSpell}
 * Configuration fields:
 * <ul>
 * <li>take-type: "health"</li>
 * <li>take-amt: 2<br>Affected by spell power</li>
 * <li>give-type: "health"</li>
 * <li>give-amt: 2<br>Affected by spell power</li>
 * <li>spell-damage-type: ""</li>
 * <li>show-spell-effect: true</li>
 * <li>animation-speed: 2</li>
 * <li>instant: true</li>
 * <li>ignore-armor: false</li>
 * <li>check-plugins: true</li>
 * </ul>
 */
public class DrainlifeSpell extends TargetedSpell implements TargetedEntitySpell, SpellDamageSpell {
	
	public static final int MAX_FOOD_LEVEL = 20;
	public static final int MIN_FOOD_LEVEL = 0;
	
	public static final double MIN_HEALTH = 0D;
	
	private String takeType;
	private double takeAmt;
	private String giveType;
	private double giveAmt;
	private String spellDamageType;
	private boolean showSpellEffect;
	int animationSpeed;
	boolean instant;
	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;
	private boolean useSmoke;
	
	private static final String STR_GIVE_TAKE_TYPE_HEALTH = "health";
	private static final String STR_GIVE_TAKE_TYPE_MANA = "mana";
	private static final String STR_GIVE_TAKE_TYPE_HUNGER = "hunger";
	private static final String STR_GIVE_TAKE_TYPE_EXPERIENCE = "experience";
	
	public DrainlifeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		takeType = getConfigString("take-type", STR_GIVE_TAKE_TYPE_HEALTH);
		takeAmt = getConfigFloat("take-amt", 2);
		giveType = getConfigString("give-type", STR_GIVE_TAKE_TYPE_HEALTH);
		giveAmt = getConfigFloat("give-amt", 2);
		spellDamageType = getConfigString("spell-damage-type", "");
		showSpellEffect = getConfigBoolean("show-spell-effect", true);
		animationSpeed = getConfigInt("animation-speed", 2);
		instant = getConfigBoolean("instant", true);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
		useSmoke = getConfigBoolean("smoke", true);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// Fail: no target
				return noTarget(player);
			}
			boolean drained = drain(player, target.getTarget(), target.getPower());
			if (!drained) return noTarget(player);
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}
	
	private boolean drain(Player player, LivingEntity target, float power) {
		double take = takeAmt * power;
		double give = giveAmt * power;
		
		// Drain from target
		if (takeType.equals(STR_GIVE_TAKE_TYPE_HEALTH)) {
			if (target instanceof Player && checkPlugins) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, take);
				EventUtil.call(event);
				if (event.isCancelled()) return false;
				if (!avoidDamageModification) take = event.getDamage();
				player.setLastDamageCause(event);
			}
			SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, player, target, take, DamageCause.MAGIC, spellDamageType);
			EventUtil.call(event);
			take = event.getFinalDamage();
			if (ignoreArmor) {
				double health = target.getHealth();
				if (health > target.getMaxHealth()) health = target.getMaxHealth();
				health -= take;
				if (health < MIN_HEALTH) health = MIN_HEALTH;
				if (health > target.getMaxHealth()) health = target.getMaxHealth();
				if (health == MIN_HEALTH && player != null) MagicSpells.getVolatileCodeHandler().setKiller(target, player);
				target.setHealth(health);
				target.playEffect(EntityEffect.HURT);
			} else {
				target.damage(take, player);
			}
		} else if (takeType.equals(STR_GIVE_TAKE_TYPE_MANA)) {
			if (target instanceof Player) {
				boolean removed = MagicSpells.getManaHandler().removeMana((Player)target, (int)Math.round(take), ManaChangeReason.OTHER);
				if (!removed) give = 0;
			}
		} else if (takeType.equals(STR_GIVE_TAKE_TYPE_HUNGER)) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int food = p.getFoodLevel();
				if (give > food) give = food;
				food -= take;
				if (food < MIN_FOOD_LEVEL) food = MIN_FOOD_LEVEL;
				p.setFoodLevel(food);
			}
		} else if (takeType.equals(STR_GIVE_TAKE_TYPE_EXPERIENCE)) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int exp = ExperienceUtils.getCurrentExp(p);
				if (give > exp) give = exp;
				ExperienceUtils.changeExp(p, (int)Math.round(-take));
			}
		}
		
		// Give to caster
		if (instant) {
			giveToCaster(player, give);
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}		
		
		// Show animation
		if (showSpellEffect) new DrainlifeAnim(target.getLocation(), player, give, power);
		
		return true;
	}
	
	void giveToCaster(Player player, double give) {
		if (giveType.equals(STR_GIVE_TAKE_TYPE_HEALTH)) {
			double h = player.getHealth() + give;
			if (h > player.getMaxHealth()) h = player.getMaxHealth();
			player.setHealth(h);
		} else if (giveType.equals(STR_GIVE_TAKE_TYPE_MANA)) {
			MagicSpells.getManaHandler().addMana(player, (int)give, ManaChangeReason.OTHER);
		} else if (giveType.equals(STR_GIVE_TAKE_TYPE_HUNGER)) {
			int food = player.getFoodLevel();
			food += give;
			if (food > MAX_FOOD_LEVEL) food = MAX_FOOD_LEVEL;
			player.setFoodLevel(food);
		} else if (giveType.equals(STR_GIVE_TAKE_TYPE_EXPERIENCE)) {
			ExperienceUtils.changeExp(player, (int)give);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return drain(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	// TODO refactor this so that the animation format can be used in a more generic manner
	// TODO it should have abstract methods for what to do at the points specific to this spell
	private class DrainlifeAnim extends SpellAnimation {
		
		Vector current;
		Player caster;
		World world;
		double giveAmtAnimator;
		int range;
		
		public DrainlifeAnim(Location start, Player caster, double giveAmt, float power) {
			super(animationSpeed, true);
			
			this.current = start.toVector();
			this.caster = caster;
			this.world = caster.getWorld();
			this.giveAmtAnimator = giveAmt;
			this.range = getRange(power);
		}

		@Override
		protected void onTick(int tick) {
			Location casterLocation = caster.getLocation();
			Vector targetVector = casterLocation.toVector();
			Vector tempVector = current.clone();
			tempVector.subtract(casterLocation.toVector()).normalize();
			current.subtract(tempVector);
			Location playAt = current.toLocation(world);
			playAt.setDirection(tempVector);
			if (useSmoke) world.playEffect(playAt, Effect.SMOKE, 4);
			playSpellEffects(EffectPosition.SPECIAL, playAt);
			if (current.distanceSquared(targetVector) < 4 || tick > range * 1.5) {
				stop();
				if (!instant) {
					giveToCaster(caster, giveAmtAnimator);
					playSpellEffects(EffectPosition.CASTER, caster);
				}
			}
		}
	}

}
