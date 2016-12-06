package com.nisovin.magicspells.spells;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.BuffManager;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.TargetInfo;


/**
 * BuffSpell<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             targeted
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             toggle
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             use-cost
 *         </td>
 *         <td>
 *             Reagents
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             use-cost-interval
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             num-uses
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             duration
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             power-affects-duration
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-give-damage
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-take-damage
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-death
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-teleport
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-change-world
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-spell-cast
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-logout
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             str-fade
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             can-cast-with-item
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             can-cast-by-command
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *            ???
 *         </td>
 *     </tr>
 * </table>
 */
public abstract class BuffSpell extends TargetedSpell implements TargetedEntitySpell {
	
	BuffSpell thisSpell;
	
	protected boolean targeted;
	
	protected boolean toggle;
	
	protected int healthCost = 0;
	protected int manaCost = 0;
	protected int hungerCost = 0;
	protected int experienceCost = 0;
	protected int levelsCost = 0;
	
	protected SpellReagents reagents;
	
	protected int useCostInterval;
	
	protected int numUses;
	
	protected float duration;
	
	protected boolean powerAffectsDuration;
	protected boolean cancelOnGiveDamage;
	protected boolean cancelOnTakeDamage;
	protected boolean cancelOnDeath;
	protected boolean cancelOnTeleport;
	protected boolean cancelOnChangeWorld;
	protected boolean cancelOnSpellCast;
	protected boolean cancelOnLogout;
	protected String strFade;
	private boolean castWithItem;
	private boolean castByCommand;
	
	private HashMap<String,Integer> useCounter;
	private HashMap<String,Long> durationEndTime;
	
	public BuffSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;
		
		targeted = getConfigBoolean("targeted", false);
		toggle = getConfigBoolean("toggle", true);
		reagents = getConfigReagents("use-cost");
		useCostInterval = getConfigInt("use-cost-interval", 0);
		numUses = getConfigInt("num-uses", 0);
		duration = getConfigFloat("duration", 0);
		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);
		cancelOnGiveDamage = getConfigBoolean("cancel-on-give-damage", false);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", false);
		cancelOnDeath = getConfigBoolean("cancel-on-death", false);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", false);
		cancelOnChangeWorld = getConfigBoolean("cancel-on-change-world", false);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnLogout = getConfigBoolean("cancel-on-logout", false);
		if (cancelOnGiveDamage || cancelOnTakeDamage) {
			registerEvents(new DamageListener());
		}
		if (cancelOnDeath) {
			registerEvents(new DeathListener());
		}
		if (cancelOnTeleport) {
			registerEvents(new TeleportListener());
		}
		if (cancelOnChangeWorld) {
			registerEvents(new ChangeWorldListener());
		}
		if (cancelOnSpellCast) {
			registerEvents(new SpellCastListener());
		}
		if (cancelOnLogout) {
			registerEvents(new QuitListener());
		}
		
		strFade = getConfigString("str-fade", "");
		
		if (numUses > 0 || (reagents != null && useCostInterval > 0)) {
			useCounter = new HashMap<String,Integer>();
		}
		if (duration > 0) {
			durationEndTime = new HashMap<String,Long>();
		}
				
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
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
	public final PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		Player target;
		if (targeted) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(player, power);
			if (targetInfo == null) return noTarget(player);
			target = targetInfo.getTarget();
			power = targetInfo.getPower();
		} else {
			target = player;
		}
		PostCastAction action = activate(player, target, power, args, state == SpellCastState.NORMAL);
		if (targeted && action == PostCastAction.HANDLE_NORMALLY) {
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return action;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			return activate(caster, (Player)target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (target instanceof Player) {
			return activate(null, (Player)target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
		} else {
			return false;
		}
	}
	
	private PostCastAction activate(Player caster, Player target, float power, String[] args, boolean normal) {
		if (isActive(target)) {
			if (toggle) {
				turnOff(target);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				if (normal) {
					boolean ok = recastBuff(target, power, args);
					if (ok) {
						startSpellDuration(target, power);
						if (caster == null) {
							playSpellEffects(EffectPosition.TARGET, target);
						} else {
							playSpellEffects(caster, target);
						}
					}
				}
				return PostCastAction.HANDLE_NORMALLY;
			}
		}
		if (normal) {
			boolean ok = castBuff(target, power, args);
			if (ok) {
				startSpellDuration(target, power);
				if (caster == null) {
					playSpellEffects(EffectPosition.TARGET, target);
				} else {
					playSpellEffects(caster, target);
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public abstract boolean castBuff(Player player, float power, String[] args);
	
	public boolean recastBuff(Player player, float power, String[] args) {
		return true;
	}
	
	public void setAsEverlasting() {
		duration = 0;
		numUses = 0;
		useCostInterval = 0;
	}
	
	/**
	 * Begins counting the spell duration for a player
	 * @param player the player to begin counting duration
	 */
	private void startSpellDuration(final Player player, float power) {
		if (duration > 0 && durationEndTime != null) {
			float dur = duration;
			if (powerAffectsDuration) dur *= power;
			durationEndTime.put(player.getName(), System.currentTimeMillis() + Math.round(dur * 1000));
			final String name = player.getName();
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				@Override
				public void run() {
					Player p = PlayerNameUtils.getPlayerExact(name);
					if (p == null) p = player;
					if (isExpired(p)) {
						turnOff(p);
					}
				}
			}, Math.round(dur * 20) + 20); // overestimate ticks, since the duration is real-time ms based			
		}
		
		playSpellEffectsBuff(player, new SpellEffect.SpellEffectActiveChecker() {
			@Override
			public boolean isActive(Entity entity) {
				return thisSpell.isActiveAndNotExpired((Player)entity);
			}
		});
		
		BuffManager buffman = MagicSpells.getBuffManager();
		if (buffman != null) buffman.addBuff(player, this);
	}
	
	/**
	 * Checks whether the spell's duration has expired for a player
	 * @param player the player to check
	 * @return true if the spell has expired, false otherwise
	 */
	protected boolean isExpired(Player player) {
		if (duration <= 0 || durationEndTime == null) {
			return false;
		} else {
			Long endTime = durationEndTime.get(player.getName());
			if (endTime == null) {
				return false;
			} else if (endTime > System.currentTimeMillis()) {
				return false;
			} else {
				return true;
			}			
		}
	}
	
	public boolean isActiveAndNotExpired(Player player) {
		if (duration > 0 && isExpired(player)) return false;
		return isActive(player);
	}
	
	/**
	 * Checks if this buff spell is active for the specified player
	 * @param player the player to check
	 * @return true if the spell is active, false otherwise
	 */
	public abstract boolean isActive(Player player);
	
	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * This does not automatically charge the use cost.
	 * @param player the player to add the use for
	 * @return the player's current number of uses (returns 0 if the use counting feature is disabled)
	 */
	protected int addUse(Player player) {
		if (numUses > 0 || (reagents != null && useCostInterval > 0)) {
			Integer uses = useCounter.get(player.getName());
			if (uses == null) {
				uses = 1;
			} else {
				uses++;
			}
			
			if (numUses > 0 && uses >= numUses) {
				turnOff(player);
			} else {
				useCounter.put(player.getName(), uses);
			}
			return uses;
		} else {
			return 0;
		}
	}
	
	/**
	 * Removes this spell's use cost from the player's inventory. If the reagents aren't available, the spell will expire.
	 * @param player the player to remove the cost from
	 * @return true if the reagents were removed, or if the use cost is disabled, false otherwise
	 */
	protected boolean chargeUseCost(Player player) {
		if (reagents != null && useCostInterval > 0 && useCounter != null && useCounter.containsKey(player.getName())) {
			int uses = useCounter.get(player.getName());
			if (uses % useCostInterval == 0) {
				if (hasReagents(player, reagents)) {
					removeReagents(player, reagents);
					return true;
				} else {
					turnOff(player);
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * Removes this spell's use cost from the player's inventory. This does not return anything, to get useful return values, use
	 * addUse() and chargeUseCost().
	 * @param player the player to add a use and charge cost to
	 */
	protected void addUseAndChargeCost(Player player) {
		addUse(player);
		chargeUseCost(player);
	}
	
	/**
	 * Turns off this spell for the specified player. This can be called from many situations, including when the spell expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(player).
	 * @param player
	 */
	public final void turnOff(Player player) {
		if (isActive(player)) {
			if (useCounter != null) useCounter.remove(player.getName());
			if (durationEndTime != null) durationEndTime.remove(player.getName());
			BuffManager buffman = MagicSpells.getBuffManager();
			if (buffman != null) buffman.removeBuff(player, this);
			sendMessage(strFade, player, null);
			playSpellEffects(EffectPosition.DISABLED, player);
			turnOffBuff(player);
			cancelEffects(EffectPosition.CASTER, player.getUniqueId().toString());
		}
	}
	
	protected abstract void turnOffBuff(Player player);
	
	@Override
	protected abstract void turnOff();
	
	@Override
	public boolean isBeneficialDefault() {
		return true;
	}
	
	public boolean isTargeted() {
		return targeted;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (isActive(event.getPlayer()) && isExpired(event.getPlayer())) {
			turnOff(event.getPlayer());
		}
	}
	
	public class DamageListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerDamage(EntityDamageEvent event) {
			if (cancelOnTakeDamage && event.getEntity() instanceof Player && isActive((Player)event.getEntity())) {
				turnOff((Player)event.getEntity());
			} else if (cancelOnGiveDamage && event instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
				if (evt.getDamager() instanceof Player && isActive((Player)evt.getDamager())) {
					turnOff((Player)evt.getDamager());
				} else if (evt.getDamager() instanceof Projectile && ((Projectile)evt.getDamager()).getShooter() instanceof LivingEntity) {
					LivingEntity shooter = (LivingEntity)((Projectile)evt.getDamager()).getShooter();
					if (shooter instanceof Player && isActive((Player)shooter)) {
						turnOff((Player)shooter);
					}
				}
			}
		}
	}
	
	public class DeathListener implements Listener {
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent event) {
			if (isActive(event.getEntity())) {
				turnOff(event.getEntity());
			}
		}
	}
	
	public class TeleportListener implements Listener {
		@EventHandler(priority=EventPriority.LOWEST)
		public void onTeleport(PlayerTeleportEvent event) {
			if (isActive(event.getPlayer())) {
				if (!event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName()) || event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 25) {
					turnOff(event.getPlayer());
				}
			}
		}
	}
	
	public class ChangeWorldListener implements Listener {
		@EventHandler(priority=EventPriority.LOWEST)
		public void onChangeWorld(PlayerChangedWorldEvent event) {
			if (isActive(event.getPlayer())) {
				turnOff(event.getPlayer());
			}
		}
	}
	
	public class SpellCastListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onChangeWorld(SpellCastEvent event) {
			if (event.getCaster() == null) {
				return;
			}
			if (thisSpell != event.getSpell() && event.getSpellCastState() == SpellCastState.NORMAL && isActive(event.getCaster())) {
				turnOff(event.getCaster());
			}
		}
	}

	public class QuitListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent event) {
			if (isActive(event.getPlayer())) {
				turnOff(event.getPlayer());
			}
		}
	}
	
}