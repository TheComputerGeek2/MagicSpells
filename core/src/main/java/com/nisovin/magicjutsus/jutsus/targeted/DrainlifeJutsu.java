package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuAnimation;
import com.nisovin.magicjutsus.util.ExperienceUtils;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.jutsus.JutsuDamageJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class DrainlifeJutsu extends TargetedJutsu implements TargetedEntityJutsu, JutsuDamageJutsu {

	private static final String STR_MANA = "mana";
	private static final String STR_HEALTH = "health";
	private static final String STR_HUNGER = "hunger";
	private static final String STR_EXPERIENCE = "experience";

	private static final int MAX_FOOD_LEVEL = 20;
	private static final int MIN_FOOD_LEVEL = 0;
	private static final double MIN_HEALTH = 0D;

	private String takeType;
	private String giveType;
	private String jutsuDamageType;

	private double takeAmt;
	private double giveAmt;

	private int animationSpeed;

	private boolean instant;
	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean showJutsuEffect;
	private boolean avoidDamageModification;

	private String jutsuOnAnimationName;
	private Ninjutsu jutsuOnAnimation;

	public DrainlifeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		takeType = getConfigString("take-type", "health");
		giveType = getConfigString("give-type", "health");
		jutsuDamageType = getConfigString("jutsu-damage-type", "");

		takeAmt = getConfigFloat("take-amt", 2);
		giveAmt = getConfigFloat("give-amt", 2);

		animationSpeed = getConfigInt("animation-speed", 2);

		instant = getConfigBoolean("instant", true);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		showJutsuEffect = getConfigBoolean("show-jutsu-effect", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);

		jutsuOnAnimationName = getConfigString("jutsu-on-animation", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		jutsuOnAnimation = new Ninjutsu(jutsuOnAnimationName);
		if (!jutsuOnAnimation.process()) {
			jutsuOnAnimation = null;
			if (!jutsuOnAnimationName.isEmpty()) MagicJutsus.error("DrainlifeJutsu '" + internalName + "' has an invalid jutsu-on-animation defined!");
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean drained = drain(livingEntity, target.getTarget(), target.getPower());
			if (!drained) return noTarget(livingEntity);
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return drain(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	@Override
	public String getJutsuDamageType() {
		return jutsuDamageType;
	}
	
	private boolean drain(LivingEntity livingEntity, LivingEntity target, float power) {
		if (livingEntity == null) return false;
		if (target == null) return false;

		double take = takeAmt * power;
		double give = giveAmt * power;

		Player pl = null;
		if (target instanceof Player) pl = (Player) target;

		switch (takeType) {
			case STR_HEALTH:
				if (pl != null && checkPlugins) {
					MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, pl, DamageCause.ENTITY_ATTACK, take);
					EventUtil.call(event);
					if (event.isCancelled()) return false;
					if (!avoidDamageModification) take = event.getDamage();
					livingEntity.setLastDamageCause(event);
				}

				JutsuApplyDamageEvent event = new JutsuApplyDamageEvent(this, livingEntity, target, take, DamageCause.MAGIC, jutsuDamageType);
				EventUtil.call(event);
				take = event.getFinalDamage();

				if (ignoreArmor) {
					double health = target.getHealth();
					if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
					health -= take;
					if (health < MIN_HEALTH) health = MIN_HEALTH;
					if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
					if (health == MIN_HEALTH && livingEntity instanceof Player) MagicJutsus.getVolatileCodeHandler().setKiller(target, (Player) livingEntity);
					target.setHealth(health);
					target.playEffect(EntityEffect.HURT);
				} else target.damage(take, livingEntity);

				break;
			case STR_MANA:
				if (pl == null) break;
				boolean removed = MagicJutsus.getManaHandler().removeMana(pl, (int) Math.round(take), ManaChangeReason.OTHER);
				if (!removed) give = 0;
				break;
			case STR_HUNGER:
				if (pl == null) break;
				int food = pl.getFoodLevel();
				if (give > food) give = food;
				food -= take;
				if (food < MIN_FOOD_LEVEL) food = MIN_FOOD_LEVEL;
				pl.setFoodLevel(food);
				break;
			case STR_EXPERIENCE:
				if (pl == null) break;
				int exp = ExperienceUtils.getCurrentExp(pl);
				if (give > exp) give = exp;
				ExperienceUtils.changeExp(pl, (int) Math.round(-take));
				break;
		}
		
		if (instant) {
			giveToCaster(livingEntity, give);
			playJutsuEffects(livingEntity, target);
		} else playJutsuEffects(EffectPosition.TARGET, target);
		
		if (showJutsuEffect) new DrainAnimation(target.getLocation(), livingEntity, give, power);
		
		return true;
	}
	
	private void giveToCaster(LivingEntity caster, double give) {
		switch (giveType) {
			case STR_HEALTH:
				double h = caster.getHealth() + give;
				if (h > Util.getMaxHealth(caster)) h = Util.getMaxHealth(caster);
				caster.setHealth(h);
				break;
			case STR_MANA:
				if (caster instanceof Player) MagicJutsus.getManaHandler().addMana((Player) caster, (int) give, ManaChangeReason.OTHER);
				break;
			case STR_HUNGER:
				if (caster instanceof Player) {
					int food = ((Player) caster).getFoodLevel();
					food += give;
					if (food > MAX_FOOD_LEVEL) food = MAX_FOOD_LEVEL;
					((Player) caster).setFoodLevel(food);
				}
				break;
			case STR_EXPERIENCE:
				if (caster instanceof Player) ExperienceUtils.changeExp((Player) caster, (int) give);
				break;
		}
	}

	private class DrainAnimation extends JutsuAnimation {

		private World world;
		private LivingEntity caster;
		private Vector current;

		private int range;
		private double giveAmtAnimator;

		DrainAnimation(Location start, LivingEntity caster, double giveAmt, float power) {
			super(animationSpeed, true);
			
			this.current = start.toVector();
			this.caster = caster;
			this.world = caster.getWorld();
			this.giveAmtAnimator = giveAmt;
			this.range = getRange(power);
		}

		@Override
		protected void onTick(int tick) {
			Vector v = current.clone();
			v.subtract(caster.getLocation().toVector()).normalize();
			current.subtract(v);

			Location playAt = current.toLocation(world).setDirection(v);
			playJutsuEffects(EffectPosition.SPECIAL, playAt);
			if (current.distanceSquared(caster.getLocation().toVector()) < 4 || tick > range * 1.5) {
				stop();
				playJutsuEffects(EffectPosition.DELAYED, caster);
				if (jutsuOnAnimation != null) jutsuOnAnimation.cast(caster, 1F);
				if (!instant) giveToCaster(caster, giveAmtAnimator);
			}
		}

	}

}
