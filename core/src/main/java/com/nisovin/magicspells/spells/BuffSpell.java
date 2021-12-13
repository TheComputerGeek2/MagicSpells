package com.nisovin.magicspells.spells;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.managers.BuffManager;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;

public abstract class BuffSpell extends TargetedSpell implements TargetedEntitySpell {

	protected BuffSpell thisSpell;

	protected Map<UUID, Integer> useCounter;
	protected Map<UUID, Long> durationEndTime;
	protected Map<UUID, LivingEntity> lastCaster;

	protected ValidTargetList targetList;

	protected float duration;

	protected int numUses;
	protected int useCostInterval;

	protected SpellReagents reagents;

	protected boolean toggle;
	protected boolean targeted;
	protected boolean castWithItem;
	protected boolean castByCommand;
	protected boolean cancelOnJoin;
	protected boolean cancelOnMove;
	protected boolean cancelOnDeath;
	protected boolean cancelOnLogout;
	protected boolean cancelOnTeleport;
	protected boolean cancelOnSpellCast;
	protected boolean cancelOnTakeDamage;
	protected boolean cancelOnGiveDamage;
	protected boolean cancelOnChangeWorld;
	protected boolean cancelAffectsTarget;
	protected boolean powerAffectsDuration;

	private final boolean endSpellFromTarget;

	protected String strFade;
	protected String spellOnEndName;
	protected String spellOnCostName;
	protected String spellOnUseIncrementName;

	protected SpellFilter filter;

	protected Subspell spellOnEnd;
	protected Subspell spellOnCost;
	protected Subspell spellOnUseIncrement;

	public BuffSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		thisSpell = this;

		if (config.isList("spells." + internalName + '.' + "can-target")) {
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
		cancelOnMove = getConfigBoolean("cancel-on-move", false);
		cancelOnDeath = getConfigBoolean("cancel-on-death", false);
		cancelOnLogout = getConfigBoolean("cancel-on-logout", false);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", false);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", false);
		cancelOnGiveDamage = getConfigBoolean("cancel-on-give-damage", false);
		cancelOnChangeWorld = getConfigBoolean("cancel-on-change-world", false);
		cancelAffectsTarget = getConfigBoolean("cancel-affects-target", true);
		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);

		endSpellFromTarget = getConfigBoolean("end-spell-from-target", true);

		strFade = getConfigString("str-fade", "");
		spellOnEndName = getConfigString("spell-on-end", "");
		spellOnCostName = getConfigString("spell-on-cost", "");
		spellOnUseIncrementName = getConfigString("spell-on-use-increment", "");

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		if (cancelOnGiveDamage || cancelOnTakeDamage) registerEvents(new DamageListener());
		if (cancelOnDeath) registerEvents(new PlayerDeathListener());
		if (cancelOnTeleport) registerEvents(new TeleportListener());
		if (cancelOnChangeWorld) registerEvents(new ChangeWorldListener());
		if (cancelOnSpellCast) registerEvents(new SpellCastListener());
		if (cancelOnLogout) registerEvents(new PlayerQuitListener());
		if (cancelOnJoin) registerEvents(new PlayerJoinListener());
		if (cancelOnMove) registerEvents(new PlayerMoveListener());
		registerEvents(new EntityDeathListener());

		if (numUses > 0 || (reagents != null && useCostInterval > 0)) useCounter = new HashMap<>();
		if (duration > 0) durationEndTime = new HashMap<>();
		lastCaster = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		// Check spell on use increment
		spellOnUseIncrement = new Subspell(spellOnUseIncrementName);
		if (!spellOnUseIncrement.process()) {
			if (!spellOnUseIncrementName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-use-increment defined!");
			spellOnUseIncrement = null;
		}

		// Check spell on cost
		spellOnCost = new Subspell(spellOnCostName);
		if (!spellOnCost.process()) {
			if (!spellOnCostName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-cost defined!");
			spellOnCost = null;
		}

		// Check spell on end
		spellOnEnd = new Subspell(spellOnEndName);
		if (!spellOnEnd.process()) {
			if (!spellOnEndName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-end defined!");
			spellOnEnd = null;
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
	public final PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		LivingEntity target;

		if (targeted) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);
			if (!targetList.canTarget(targetInfo.getTarget())) return noTarget(caster);

			target = targetInfo.getTarget();
			power = targetInfo.getPower();
		}
		else target = caster;

		PostCastAction action = activate(caster, target, power, args, state == SpellCastState.NORMAL);
		if (targeted && action == PostCastAction.HANDLE_NORMALLY) {
			sendMessages(caster, target, args);
			return PostCastAction.NO_MESSAGES;
		}

		return action;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		return activate(caster, target, power, args, true) == PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return activate(caster, target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return activate(null, target, power, args, true) == PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return activate(null, target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
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

		startSpellDuration(target, power);
		lastCaster.put(target.getUniqueId(), caster);
		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		return PostCastAction.HANDLE_NORMALLY;
	}

	public abstract boolean castBuff(LivingEntity entity, float power, String[] args);

	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		stopEffects(entity);
		return true;
	}

	public void setAsEverlasting() {
		duration = 0;
		numUses = 0;
		useCostInterval = 0;
	}

	/**
	 * Begins counting the spell duration for a livingEntity
	 * @param livingEntity the livingEntity to begin counting duration
	 */
	private void startSpellDuration(final LivingEntity livingEntity, float power) {
		if (duration > 0 && durationEndTime != null) {

			float dur = duration;
			if (powerAffectsDuration) dur *= power;
			setDuration(livingEntity, dur);

			MagicSpells.scheduleDelayedTask(() -> {
				if (isExpired(livingEntity)) turnOff(livingEntity);
			}, Math.round(dur * TimeUtil.TICKS_PER_SECOND) + 1); // overestimate ticks, since the duration is real-time ms based
		}

		playSpellEffectsBuff(livingEntity, entity -> thisSpell.isActiveAndNotExpired((LivingEntity) entity));

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null) manager.addBuff(livingEntity, this);
	}

	public void setDuration(LivingEntity livingEntity, float duration) {
		long endTime = System.currentTimeMillis() + Math.round(duration * TimeUtil.MILLISECONDS_PER_SECOND);
		durationEndTime.put(livingEntity.getUniqueId(), endTime);
	}

	public float getDuration(LivingEntity livingEntity) {
		if (durationEndTime == null) return 0;
		if (!durationEndTime.containsKey(livingEntity.getUniqueId())) return 0;
		return (durationEndTime.get(livingEntity.getUniqueId()) - System.currentTimeMillis()) / 1000F;
	}

	public float getDuration() {
		return duration;
	}

	public LivingEntity getLastCaster(LivingEntity target) {
		return lastCaster.get(target.getUniqueId());
	}

	/**
	 * Checks whether the spell's duration has expired for a livingEntity
	 * @param entity the livingEntity to check
	 * @return true if the spell has expired, false otherwise
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
	 * Checks if this buff spell is active for the specified livingEntity
	 * @param entity the livingEntity to check
	 * @return true if the spell is active, false otherwise
	 */
	public abstract boolean isActive(LivingEntity entity);

	/**
	 * Adds a use to the spell for the livingEntity. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * This does not automatically charge the use cost.
	 * @param entity the livingEntity to add the use for
	 * @return the livingEntity's current number of uses (returns 0 if the use counting feature is disabled)
	 */
	protected int addUse(LivingEntity entity) {
		// Run spell on use increment first thing in case we want to intervene
		if (spellOnUseIncrement != null) spellOnUseIncrement.cast(entity, 1f);

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
	 * Removes this spell's use cost from the livingEntity's inventory. If the reagents aren't available, the spell will expire.
	 * @param entity the livingEntity to remove the cost from
	 * @return true if the reagents were removed, or if the use cost is disabled, false otherwise
	 */
	protected boolean chargeUseCost(LivingEntity entity) {
		// Run spell on cost first thing to dodge the early returns and allow intervention
		if (spellOnCost != null) spellOnCost.cast(entity, 1f);

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
	 * Adds a use to the spell for the livingEntity. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * Removes this spell's use cost from the livingEntity's inventory. This does not return anything, to get useful return values, use
	 * addUse() and chargeUseCost().
	 * @param entity the livingEntity to add a use and charge cost to
	 */
	protected void addUseAndChargeCost(LivingEntity entity) {
		addUse(entity);
		chargeUseCost(entity);
	}

	/**
	 * Turns off this spell for the specified livingEntity. This can be called from many situations, including when the spell expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(livingEntity).
	 * @param entity
	 */
	public final void turnOff(LivingEntity entity) {
		if (!isActive(entity)) return;

		if (useCounter != null) useCounter.remove(entity.getUniqueId());
		if (durationEndTime != null) durationEndTime.remove(entity.getUniqueId());

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null) manager.removeBuff(entity, this);

		turnOffBuff(entity);
		playSpellEffects(EffectPosition.DISABLED, entity);
		cancelEffects(EffectPosition.CASTER, entity.getUniqueId().toString());
		stopEffects(entity);

		if (spellOnEnd != null) spellOnEnd.cast(endSpellFromTarget ? entity : getLastCaster(entity), 1f);
		sendMessage(strFade, entity, null);

		lastCaster.remove(entity.getUniqueId());
	}

	public void stopEffects(LivingEntity livingEntity) {
		Iterator<EffectTracker> trackerIterator = getEffectTrackers().iterator();
		EffectTracker tracker;
		while(trackerIterator.hasNext()) {
			tracker = trackerIterator.next();
			if (tracker.getEntity() != null && !tracker.getEntity().equals(livingEntity)) continue;
			tracker.stop();
			trackerIterator.remove();
		}

		Iterator<AsyncEffectTracker> asyncTrackerIterator = getAsyncEffectTrackers().iterator();
		AsyncEffectTracker asyncTracker;
		while(asyncTrackerIterator.hasNext()) {
			asyncTracker = asyncTrackerIterator.next();
			if (asyncTracker.getEntity() != null && !asyncTracker.getEntity().equals(livingEntity)) continue;
			asyncTracker.stop();
			asyncTrackerIterator.remove();
		}
	}

	public void stopAllEffects() {
		Iterator<EffectTracker> trackerIterator = getEffectTrackers().iterator();
		EffectTracker effectTracker;
		while(trackerIterator.hasNext()) {
			effectTracker = trackerIterator.next();
			effectTracker.stop();
			trackerIterator.remove();
		}

		Iterator<AsyncEffectTracker> asyncTrackerIterator = getAsyncEffectTrackers().iterator();
		AsyncEffectTracker asyncEffectTracker;
		while(asyncTrackerIterator.hasNext()) {
			asyncEffectTracker = asyncTrackerIterator.next();
			asyncEffectTracker.stop();
			asyncTrackerIterator.remove();
		}
	}

	protected abstract void turnOffBuff(LivingEntity entity);

	@Override
	protected abstract void turnOff();

	public boolean isTargeted() {
		return targeted;
	}

	private LivingEntity getWhoToCancel(LivingEntity livingEntity) {
		// If the target was affected by the event, cancel them.
		if (!targeted || cancelAffectsTarget) return isActiveAndNotExpired(livingEntity) ? livingEntity : null;

		// targeted && !cancelAffectsTarget
		// If the caster was affected by the event, cancel the target if they have the buff.

		for (Map.Entry<UUID, LivingEntity> entry : lastCaster.entrySet()) {
			if (entry == null) continue;

			// Check if the entity is the caster of this entry.
			if (!entry.getValue().getUniqueId().equals(livingEntity.getUniqueId())) continue;

			Entity entity = Bukkit.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target)) return null;
			return isActiveAndNotExpired(target) ? target : null;
		}

		return null;
	}

	public class DamageListener implements Listener {

		@EventHandler(ignoreCancelled=true)
		public void onEntityDamage(EntityDamageEvent e) {
			if (cancelOnTakeDamage) {
				Entity entity = e.getEntity();
				if (!(entity instanceof LivingEntity livingEntity)) return;
				LivingEntity target = getWhoToCancel(livingEntity);
				if (target == null) return;
				turnOff(target);
				return;
			}

			if (cancelOnGiveDamage) {
				if (!(e instanceof EntityDamageByEntityEvent evt)) return;

				Entity damager = evt.getDamager();
				if (!(damager instanceof LivingEntity livingEntity)) return;
				LivingEntity target = getWhoToCancel(livingEntity);
				if (target != null) {
					turnOff(target);
					return;
				}

				if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
					LivingEntity newTarget = getWhoToCancel(shooter);
					if (newTarget == null) return;
					turnOff(newTarget);
				}
			}
		}

	}

	public class EntityDeathListener implements Listener {

		// Entity only
		@EventHandler(ignoreCancelled = true)
		public void onEntityDeath(EntityDeathEvent event) {
			LivingEntity entity = getWhoToCancel(event.getEntity());
			if (entity == null) return;
			if (entity instanceof Player) return;
			turnOff(entity);
		}

	}

	public class PlayerDeathListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		public void onPlayerDeath(PlayerDeathEvent event) {
			LivingEntity player = getWhoToCancel(event.getEntity());
			if (player == null) return;
			turnOff(player);
		}

	}

	public class TeleportListener implements Listener {

		// player only
		@EventHandler(ignoreCancelled = true)
		public void onTeleport(PlayerTeleportEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;
			if (!LocationUtil.differentWorldDistanceGreaterThan(event.getFrom(), event.getTo(), 5)) return;
			turnOff(player);
		}

		// entity only
		@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
		public void onTeleport(EntityTeleportEvent event) {
			if (!(event.getEntity() instanceof LivingEntity)) return;
			LivingEntity entity = getWhoToCancel((LivingEntity) event.getEntity());
			if (entity == null) return;
			if (!LocationUtil.differentWorldDistanceGreaterThan(event.getFrom(), event.getTo(), 5)) return;
			turnOff(entity);
		}

	}

	public class ChangeWorldListener implements Listener {

		// player only
		@EventHandler(priority=EventPriority.LOWEST)
		public void onChangeWorld(PlayerChangedWorldEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;
			turnOff(player);
		}

		// entity only
		@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
		public void onChangeWorld(EntityTeleportEvent event) {
			if (!(event.getEntity() instanceof LivingEntity)) return;

			// Check if the world is the same.
			Location to = event.getTo();
			if (to == null) return;
			if (event.getFrom().getWorld().equals(to.getWorld())) return;

			LivingEntity entity = getWhoToCancel((LivingEntity) event.getEntity());
			if (entity == null) return;
			turnOff(entity);
		}

	}

	public class SpellCastListener implements Listener {

		@EventHandler(ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (thisSpell == event.getSpell()) return;
			if (event.getSpellCastState() != SpellCastState.NORMAL) return;
			LivingEntity entity = getWhoToCancel(event.getCaster());
			if (entity == null) return;
			if (filter.check(event.getSpell())) return;
			turnOff(entity);
		}

	}

	public class PlayerQuitListener implements Listener {

		@EventHandler
		public void onQuit(PlayerQuitEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;
			turnOff(player);
		}

	}

	public class PlayerJoinListener implements Listener {

		@EventHandler
		public void onJoin(PlayerJoinEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;
			turnOff(player);
		}

	}

	public class PlayerMoveListener implements Listener {

		private static final double MOTION_TOLERANCE = 0.1;

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onMove(PlayerMoveEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;

			if (LocationUtil.distanceLessThan(event.getFrom(), event.getTo(), MOTION_TOLERANCE)) return;

			turnOff(player);
		}

	}

}
