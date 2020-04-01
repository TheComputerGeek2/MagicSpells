package com.nisovin.magicjutsus.jutsus;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.BuffManager;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.util.LocationUtil;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectTracker;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public abstract class BuffJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	protected BuffJutsu thisJutsu;

	protected Map<UUID, Integer> useCounter;
	protected Map<UUID, Long> durationEndTime;

	protected ValidTargetList targetList;

	protected float duration;

	protected int numUses;
	protected int useCostInterval;

	protected JutsuReagents reagents;

	protected boolean toggle;
	protected boolean targeted;
	protected boolean castWithItem;
	protected boolean castByCommand;
	protected boolean cancelOnJoin;
	protected boolean cancelOnDeath;
	protected boolean cancelOnLogout;
	protected boolean cancelOnTeleport;
	protected boolean cancelOnJutsuCast;
	protected boolean cancelOnTakeDamage;
	protected boolean cancelOnGiveDamage;
	protected boolean cancelOnChangeWorld;
	protected boolean powerAffectsDuration;

	protected String strFade;
	protected String jutsuOnEndName;
	protected String jutsuOnCostName;
	protected String jutsuOnUseIncrementName;

	protected JutsuFilter filter;

	protected Ninjutsu jutsuOnEnd;
	protected Ninjutsu jutsuOnCost;
	protected Ninjutsu jutsuOnUseIncrement;

	public BuffJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		thisJutsu = this;

		if (config.isList("jutsus." + internalName + '.' + "can-target")) {
			List<String> defaultTargets = getConfigStringList("can-target", null);
			if (defaultTargets.isEmpty()) defaultTargets.add("players");
			targetList = new ValidTargetList(this, defaultTargets);
		} else targetList = new ValidTargetList(this, getConfigString("can-target", "players"));

		duration = getConfigFloat("duration", 0);

		numUses = getConfigInt("num-uses", 0);
		useCostInterval = getConfigInt("use-cost-interval", 0);

		reagents = getConfigReagents("use-cost");

		toggle = getConfigBoolean("toggle", true);
		targeted = getConfigBoolean("targeted", false);
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		cancelOnJoin = getConfigBoolean("cancel-on-join", false);
		cancelOnDeath = getConfigBoolean("cancel-on-death", false);
		cancelOnLogout = getConfigBoolean("cancel-on-logout", false);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", false);
		cancelOnJutsuCast = getConfigBoolean("cancel-on-jutsu-cast", false);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", false);
		cancelOnGiveDamage = getConfigBoolean("cancel-on-give-damage", false);
		cancelOnChangeWorld = getConfigBoolean("cancel-on-change-world", false);
		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);

		strFade = getConfigString("str-fade", "");
		jutsuOnEndName = getConfigString("jutsu-on-end", "");
		jutsuOnCostName = getConfigString("jutsu-on-cost", "");
		jutsuOnUseIncrementName = getConfigString("jutsu-on-use-increment", "");

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		if (cancelOnGiveDamage || cancelOnTakeDamage) registerEvents(new DamageListener());
		if (cancelOnDeath) registerEvents(new DeathListener());
		if (cancelOnTeleport) registerEvents(new TeleportListener());
		if (cancelOnChangeWorld) registerEvents(new ChangeWorldListener());
		if (cancelOnJutsuCast) registerEvents(new JutsuCastListener());
		if (cancelOnLogout) registerEvents(new QuitListener());
		if (cancelOnJoin) registerEvents(new JoinListener());
		registerEvents(new EntityListener());

		if (numUses > 0 || (reagents != null && useCostInterval > 0)) useCounter = new HashMap<>();
		if (duration > 0) durationEndTime = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		// Check jutsu on use increment
		jutsuOnUseIncrement = new Ninjutsu(jutsuOnUseIncrementName);
		if (!jutsuOnUseIncrement.process()) {
			if (!jutsuOnUseIncrementName.isEmpty()) MagicJutsus.error("BuffJutsu '" + internalName + "' has an invalid jutsu-on-use-increment defined!");
			jutsuOnUseIncrement = null;
		}

		// Check jutsu on cost
		jutsuOnCost = new Ninjutsu(jutsuOnCostName);
		if (!jutsuOnCost.process()) {
			if (!jutsuOnCostName.isEmpty()) MagicJutsus.error("BuffJutsu '" + internalName + "' has an invalid jutsu-on-cost defined!");
			jutsuOnCost = null;
		}

		// Check jutsu on end
		jutsuOnEnd = new Ninjutsu(jutsuOnEndName);
		if (!jutsuOnEnd.process()) {
			if (!jutsuOnEndName.isEmpty()) MagicJutsus.error("BuffJutsu '" + internalName + "' has an invalid jutsu-on-end defined!");
			jutsuOnEnd = null;
		}

	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	@Override
	public final PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		LivingEntity target;

		if (targeted) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			if (!targetList.canTarget(targetInfo.getTarget())) return noTarget(livingEntity);

			target = targetInfo.getTarget();
			power = targetInfo.getPower();
		} else {
			target = livingEntity;
		}

		PostCastAction action = activate(livingEntity, target, power, args, state == JutsuCastState.NORMAL);
		if (targeted && action == PostCastAction.HANDLE_NORMALLY) {
			sendMessages(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}

		return action;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return activate(caster, target, power, MagicJutsus.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return activate(null, target, power, MagicJutsus.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
	}

	private PostCastAction activate(LivingEntity caster, LivingEntity target, float power, String[] args, boolean normal) {
		if (isActive(target) && toggle) {
			turnOff(target);
			return PostCastAction.ALREADY_HANDLED;
		}

		if (!normal) return PostCastAction.HANDLE_NORMALLY;

		boolean ok;
		if (isActive(target)) ok = recastBuff(target, power, args);
		else ok = castBuff(target, power, args);
		if (!ok) return PostCastAction.HANDLE_NORMALLY;

		startJutsuDuration(target, power);
		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);

		return PostCastAction.HANDLE_NORMALLY;
	}

	public abstract boolean castBuff(LivingEntity entity, float power, String[] args);

	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		stopEffects();
		return true;
	}

	public void setAsEverlasting() {
		duration = 0;
		numUses = 0;
		useCostInterval = 0;
	}

	/**
	 * Begins counting the jutsu duration for a livingEntity
	 * @param livingEntity the livingEntity to begin counting duration
	 */
	private void startJutsuDuration(final LivingEntity livingEntity, float power) {
		if (duration > 0 && durationEndTime != null) {

			float dur = duration;
			if (powerAffectsDuration) dur *= power;
			durationEndTime.put(livingEntity.getUniqueId(), System.currentTimeMillis() + Math.round(dur * TimeUtil.MILLISECONDS_PER_SECOND));

			MagicJutsus.scheduleDelayedTask(() -> {
				if (isExpired(livingEntity)) turnOff(livingEntity);
			}, Math.round(dur * TimeUtil.TICKS_PER_SECOND) + 1); // overestimate ticks, since the duration is real-time ms based
		}

		playJutsuEffectsBuff(livingEntity, entity -> thisJutsu.isActiveAndNotExpired((LivingEntity) entity));

		BuffManager manager = MagicJutsus.getBuffManager();
		if (manager != null) manager.addBuff(livingEntity, this);
	}

	public float getDuration(LivingEntity livingEntity) {
		if (durationEndTime == null) return 0;
		if (!durationEndTime.containsKey(livingEntity.getUniqueId())) return 0;
		return (durationEndTime.get(livingEntity.getUniqueId()) - System.currentTimeMillis()) / 1000F;
	}

	public float getDuration() {
		return duration;
	}

	/**
	 * Checks whether the jutsu's duration has expired for a livingEntity
	 * @param entity the livingEntity to check
	 * @return true if the jutsu has expired, false otherwise
	 */
	public boolean isExpired(LivingEntity entity) {
		if (duration <= 0 || durationEndTime == null) return false;
		if (entity == null) return false;
		Long endTime = durationEndTime.get(entity.getUniqueId());
		if (endTime == null) return false;
		return endTime <= System.currentTimeMillis();
	}

	public boolean isActiveAndNotExpired(LivingEntity entity) {
		if (duration > 0 && isExpired(entity)) return false;
		return isActive(entity);
	}

	/**
	 * Checks if this buff jutsu is active for the specified livingEntity
	 * @param entity the livingEntity to check
	 * @return true if the jutsu is active, false otherwise
	 */
	public abstract boolean isActive(LivingEntity entity);

	/**
	 * Adds a use to the jutsu for the livingEntity. If the number of uses exceeds the amount allowed, the jutsu will immediately expire.
	 * This does not automatically charge the use cost.
	 * @param entity the livingEntity to add the use for
	 * @return the livingEntity's current number of uses (returns 0 if the use counting feature is disabled)
	 */
	protected int addUse(LivingEntity entity) {
		// Run jutsu on use increment first thing in case we want to intervene
		if (jutsuOnUseIncrement != null) jutsuOnUseIncrement.cast(entity, 1f);

		if (numUses > 0 || (reagents != null && useCostInterval > 0)) {

			Integer uses = useCounter.get(entity.getUniqueId());

			if (uses == null) uses = 1;
			else uses++;

			if (numUses > 0 && uses >= numUses) turnOff(entity);
			else useCounter.put(entity.getUniqueId(), uses);

			return uses;
		}

		return 0;

	}

	/**
	 * Removes this jutsu's use cost from the livingEntity's inventory. If the reagents aren't available, the jutsu will expire.
	 * @param entity the livingEntity to remove the cost from
	 * @return true if the reagents were removed, or if the use cost is disabled, false otherwise
	 */
	protected boolean chargeUseCost(LivingEntity entity) {
		// Run jutsu on cost first thing to dodge the early returns and allow intervention
		if (jutsuOnCost != null) jutsuOnCost.cast(entity, 1f);

		if (reagents == null) return true;
		if (useCostInterval <= 0) return true;
		if (useCounter == null) return true;

		Integer uses = useCounter.get(entity.getUniqueId());
		if (uses == null) return true;
		if (uses % useCostInterval != 0) return true;

		if (hasReagents(entity, reagents)) {
			removeReagents(entity, reagents);
			return true;
		}

		if (!hasReagents(entity, reagents)) {
			turnOff(entity);
			return false;
		}

		return true;
	}

	/**
	 * Adds a use to the jutsu for the livingEntity. If the number of uses exceeds the amount allowed, the jutsu will immediately expire.
	 * Removes this jutsu's use cost from the livingEntity's inventory. This does not return anything, to get useful return values, use
	 * addUse() and chargeUseCost().
	 * @param entity the livingEntity to add a use and charge cost to
	 */
	protected void addUseAndChargeCost(LivingEntity entity) {
		addUse(entity);
		chargeUseCost(entity);
	}

	/**
	 * Turns off this jutsu for the specified livingEntity. This can be called from many situations, including when the jutsu expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(livingEntity).
	 * @param entity
	 */
	public final void turnOff(LivingEntity entity) {
		if (!isActive(entity)) return;

		if (useCounter != null) useCounter.remove(entity.getUniqueId());
		if (durationEndTime != null) durationEndTime.remove(entity.getUniqueId());

		BuffManager manager = MagicJutsus.getBuffManager();
		if (manager != null) manager.removeBuff(entity, this);

		turnOffBuff(entity);
		playJutsuEffects(EffectPosition.DISABLED, entity);
		cancelEffects(EffectPosition.CASTER, entity.getUniqueId().toString());
		stopEffects();

		if (jutsuOnEnd != null) jutsuOnEnd.cast(entity, 1f);
		sendMessage(strFade, entity, null);
	}

	public void stopEffects() {
		Set<EffectTracker> trackers = new HashSet<>(getEffectTrackers());
		getEffectTrackers().clear();
		for (EffectTracker effectTracker : trackers) {
			effectTracker.stop();
		}
		trackers.clear();
	}

	protected abstract void turnOffBuff(LivingEntity entity);

	@Override
	protected abstract void turnOff();

	@Override
	public boolean isBeneficialDefault() {
		return true;
	}

	public boolean isTargeted() {
		return targeted;
	}

	public class DamageListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onEntityDamage(EntityDamageEvent e) {
			Entity entity = e.getEntity();
			if (cancelOnTakeDamage && entity instanceof LivingEntity && isActiveAndNotExpired((LivingEntity) entity)) {
				turnOff((LivingEntity) entity);
			} else if (cancelOnGiveDamage && e instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) e;
				Entity damager = evt.getDamager();
				if (damager instanceof LivingEntity && isActiveAndNotExpired((LivingEntity) damager)) {
					turnOff((LivingEntity) damager);
				} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity) {
					LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
					if (isActiveAndNotExpired(shooter)) turnOff(shooter);
				}
			}
		}

	}

	public class EntityListener implements Listener {

		@EventHandler
		public void onEntityDeath(EntityDeathEvent event) {
			LivingEntity entity = event.getEntity();
			if (entity instanceof Player) return;
			if (!isActiveAndNotExpired(entity)) return;
			turnOff(entity);
		}

	}

	public class DeathListener implements Listener {

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent event) {
			Player pl = event.getEntity();
			if (!isActiveAndNotExpired(pl)) return;
			turnOff(pl);
		}

	}

	public class TeleportListener implements Listener {

		@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
		public void onTeleport(EntityTeleportEvent e) {
			if (!(e.getEntity() instanceof LivingEntity)) return;
			LivingEntity entity = (LivingEntity) e.getEntity();
			if (!isActiveAndNotExpired(entity)) return;

			Location locationFrom = e.getFrom();
			Location locationTo = e.getTo();

			if (LocationUtil.differentWorldDistanceGreaterThan(locationFrom, locationTo, 5)) {
				turnOff(entity);
			}

		}

	}

	public class ChangeWorldListener implements Listener {

		@EventHandler(priority=EventPriority.LOWEST)
		public void onChangeWorld(PlayerChangedWorldEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

	public class JutsuCastListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onJutsuCast(JutsuCastEvent e) {
			if (thisJutsu != e.getJutsu() && e.getJutsuCastState() == JutsuCastState.NORMAL && isActiveAndNotExpired(e.getCaster())) {
				if (filter.check(e.getJutsu())) turnOff(e.getCaster());
			}
		}

	}

	public class QuitListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR)
		public void onQuit(PlayerQuitEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

	public class JoinListener implements Listener {

		@EventHandler
		public void onJoin(PlayerJoinEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

}
