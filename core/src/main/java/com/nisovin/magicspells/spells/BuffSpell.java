package com.nisovin.magicspells.spells;

import java.util.Map;
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

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.managers.BuffManager;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;

public abstract class BuffSpell extends TargetedSpell implements TargetedEntitySpell {

	protected Map<UUID, Integer> useCounter;
	protected Map<UUID, Long> durationEndTime;
	protected Map<UUID, LivingEntity> lastCaster;

	protected float duration;

	protected int numUses;
	protected int useCostInterval;

	protected SpellReagents reagents;

	protected boolean toggle;
	protected boolean targeted;
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

		duration = getConfigFloat("duration", 0);

		numUses = getConfigInt("num-uses", 0);
		useCostInterval = getConfigInt("use-cost-interval", 0);

		reagents = getConfigReagents("use-cost");

		toggle = getConfigBoolean("toggle", true);
		targeted = getConfigBoolean("targeted", false);
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

		String error = "BuffSpell '" + internalName + "' has an invalid '%s' defined!";
		spellOnUseIncrement = initSubspell(spellOnUseIncrementName,
				error.formatted("spell-on-use-increment"),
				true);
		spellOnCost = initSubspell(spellOnCostName,
				error.formatted("spell-on-cost"),
				true);
		spellOnEnd = initSubspell(spellOnEndName,
				error.formatted("spell-on-end"),
				true);

		filter = getConfigSpellFilter();
	}

	@Override
	public CastResult cast(SpellCastState state, SpellData data) {
		if (state != SpellCastState.NORMAL && !toggle) return new CastResult(PostCastAction.HANDLE_NORMALLY, data);

		if (targeted) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		} else data = data.target(data.caster());

		if (state != SpellCastState.NORMAL) {
			if (isActive(data.target())) turnOff(data.target());
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		return castAtEntity(data);
	}

	@Override
	public CastResult cast(SpellData data) {
		return cast(SpellCastState.NORMAL, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		boolean active = isActive(data.target());
		if (active && toggle) {
			turnOff(data.target());
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		SpellData recipientData = data.recipient(data.target());
		boolean casted = active ? recastBuff(recipientData) : castBuff(recipientData);
		if (!casted) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		startSpellDuration(data);
		lastCaster.put(data.target().getUniqueId(), data.caster());
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Deprecated
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		return false;
	}

	@Deprecated
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		stopEffects(entity);
		return true;
	}

	public boolean castBuff(SpellData data) {
		return castBuff(data.target(), data.power(), data.args());
	}

	public boolean recastBuff(SpellData data) {
		return recastBuff(data.target(), data.power(), data.args());
	}

	public void setAsEverlasting() {
		duration = 0;
		numUses = 0;
		useCostInterval = 0;
	}

	/**
	 * Begins counting the spell duration for a living entity
	 *
	 * @param data the spell data of the buff
	 */
	private void startSpellDuration(SpellData data) {
		if (duration > 0 && durationEndTime != null) {

			float dur = duration;
			if (powerAffectsDuration) dur *= data.power();
			setDuration(data.target(), dur);

			MagicSpells.scheduleDelayedTask(() -> {
				if (isExpired(data.target())) turnOff(data.target());
			}, Math.round(dur * TimeUtil.TICKS_PER_SECOND) + 1, data.target()); // overestimate ticks, since the duration is real-time ms based
		}

		playSpellEffectsBuff(data.target(), entity -> isActiveAndNotExpired((LivingEntity) entity), data);

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null) manager.startBuff(data.target(), this);
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
	 *
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
	 *
	 * @param entity the livingEntity to check
	 * @return true if the spell is active, false otherwise
	 */
	public abstract boolean isActive(LivingEntity entity);

	/**
	 * Adds a use to the spell for the livingEntity. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * It also removes this spell's use-cost from the livingEntity's inventory. If the reagents aren't available, the spell will expire.
	 * NOTE: Because the spell may expire, it's safest to call this method at end to avoid possible exceptions during turnOff logic.
	 *
	 * @param entity The livingEntity to add the use and remove the reagents from.
	 */
	protected void addUseAndChargeCost(LivingEntity entity) {
		if (!isActive(entity)) return;

		boolean hasNoCost = reagents == null && useCostInterval <= 0;
		if (numUses <= 0 && hasNoCost) return;

		// Increment uses.
		if (spellOnUseIncrement != null) spellOnUseIncrement.subcast(new SpellData(entity));
		int uses = useCounter.getOrDefault(entity.getUniqueId(), 0) + 1;
		if (numUses > 0 && uses >= numUses) turnOff(entity);
		else useCounter.put(entity.getUniqueId(), uses);

		// Charge cost.
		if (hasNoCost) return;
		if (uses % useCostInterval != 0) return;
		if (hasReagents(entity, reagents)) removeReagents(entity, reagents);
		else turnOff(entity);
		if (spellOnCost != null) spellOnCost.subcast(new SpellData(entity));
	}

	/**
	 * Turns off this spell for the specified livingEntity. This can be called from many situations, including when the spell expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(livingEntity).
	 *
	 * @param entity livingEntity to turn the buff off for
	 */
	public final void turnOff(LivingEntity entity) {
		turnOff(entity, true);
	}

	public final void turnOff(LivingEntity entity, boolean removeFromMap) {
		if (!isActive(entity)) return;

		if (useCounter != null) useCounter.remove(entity.getUniqueId());
		if (durationEndTime != null) durationEndTime.remove(entity.getUniqueId());

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null && removeFromMap) manager.endBuff(entity, this);

		turnOffBuff(entity);
		playSpellEffects(EffectPosition.DISABLED, entity, new SpellData(entity));
		cancelEffects(EffectPosition.CASTER, entity.getUniqueId().toString());
		stopEffects(entity);

		if (spellOnEnd != null) spellOnEnd.subcast(new SpellData(endSpellFromTarget ? entity : getLastCaster(entity)));
		sendMessage(strFade, entity, SpellData.NULL);

		lastCaster.remove(entity.getUniqueId());
	}

	public void stopEffects(LivingEntity livingEntity) {
		Iterator<EffectTracker> trackerIterator = getEffectTrackers().iterator();
		EffectTracker tracker;
		while (trackerIterator.hasNext()) {
			tracker = trackerIterator.next();
			if (tracker.getEntity() != null && !tracker.getEntity().equals(livingEntity)) continue;
			tracker.stop();
			trackerIterator.remove();
		}

		Iterator<AsyncEffectTracker> asyncTrackerIterator = getAsyncEffectTrackers().iterator();
		AsyncEffectTracker asyncTracker;
		while (asyncTrackerIterator.hasNext()) {
			asyncTracker = asyncTrackerIterator.next();
			if (asyncTracker.getEntity() != null && !asyncTracker.getEntity().equals(livingEntity)) continue;
			asyncTracker.stop();
			asyncTrackerIterator.remove();
		}
	}

	public void stopAllEffects() {
		Iterator<EffectTracker> trackerIterator = getEffectTrackers().iterator();
		EffectTracker effectTracker;
		while (trackerIterator.hasNext()) {
			effectTracker = trackerIterator.next();
			effectTracker.stop();
			trackerIterator.remove();
		}

		Iterator<AsyncEffectTracker> asyncTrackerIterator = getAsyncEffectTrackers().iterator();
		AsyncEffectTracker asyncEffectTracker;
		while (asyncTrackerIterator.hasNext()) {
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

		@EventHandler(ignoreCancelled = true)
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
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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
		@EventHandler(priority = EventPriority.LOWEST)
		public void onChangeWorld(PlayerChangedWorldEvent event) {
			LivingEntity player = getWhoToCancel(event.getPlayer());
			if (player == null) return;
			turnOff(player);
		}

		// entity only
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

		@EventHandler(ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			if (BuffSpell.this == event.getSpell()) return;
			if (event.getSpellCastState() != SpellCastState.NORMAL) return;

			LivingEntity entity = getWhoToCancel(event.getCaster());
			if (entity == null) return;
			if (!filter.isEmpty() && filter.check(event.getSpell())) return;
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
