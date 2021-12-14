package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RitualSpell extends InstantSpell {

	private final Map<Player, ActiveRitual> activeRituals;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> effectInterval;
	private ConfigData<Integer> ritualDuration;
	private ConfigData<Integer> reqParticipants;

	private boolean setCooldownForAll;
	private boolean showProgressOnExpBar;
	private boolean setCooldownImmediately;
	private boolean needSpellToParticipate;
	private boolean chargeReagentsImmediately;

	private String spellToCastName;
	private Spell spellToCast;

	private String strRitualLeft;
	private String strRitualJoined;
	private String strRitualFailed;
	private String strRitualSuccess;
	private String strRitualInterrupted;

	public RitualSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		activeRituals = new HashMap<>();

		tickInterval = getConfigDataInt("tick-interval", 5);
		effectInterval = getConfigDataInt("effect-interval", TimeUtil.TICKS_PER_SECOND);
		ritualDuration = getConfigDataInt("ritual-duration", 200);
		reqParticipants = getConfigDataInt("req-participants", 3);

		setCooldownForAll = getConfigBoolean("set-cooldown-for-all", true);
		showProgressOnExpBar = getConfigBoolean("show-progress-on-exp-bar", true);
		setCooldownImmediately = getConfigBoolean("set-cooldown-immediately", true);
		needSpellToParticipate = getConfigBoolean("need-spell-to-participate", false);
		chargeReagentsImmediately = getConfigBoolean("charge-reagents-immediately", true);

		spellToCastName = getConfigString("spell", "");

		strRitualLeft = getConfigString("str-ritual-left", "");
		strRitualJoined = getConfigString("str-ritual-joined", "");
		strRitualFailed = getConfigString("str-ritual-failed", "");
		strRitualSuccess = getConfigString("str-ritual-success", "");
		strRitualInterrupted = getConfigString("str-ritual-interrupted", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = MagicSpells.getSpellByInternalName(spellToCastName);
		if (spellToCast == null) MagicSpells.error("RitualSpell '" + internalName + "' has an invalid spell defined!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (spellToCast == null || !(caster instanceof Player player)) return PostCastAction.ALREADY_HANDLED;
		if (activeRituals.containsKey(player)) {
			ActiveRitual channel = activeRituals.remove(player);
			channel.stop(strRitualInterrupted);
		}
		if (state == SpellCastState.NORMAL) {
			activeRituals.put(player, new ActiveRitual(player, power, args));
			if (!chargeReagentsImmediately && !setCooldownImmediately) return PostCastAction.MESSAGES_ONLY;
			if (!chargeReagentsImmediately) return PostCastAction.NO_REAGENTS;
			if (!setCooldownImmediately) return PostCastAction.NO_COOLDOWN;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Player player)) return;
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) return;

		ActiveRitual channel = activeRituals.get(player);
		if (channel == null) return;

		if (!needSpellToParticipate || hasThisSpell(event.getPlayer())) {
			channel.addChanneler(event.getPlayer());
			sendMessage(strRitualJoined, event.getPlayer(), channel.args);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (!ritual.isChanneler(event.getPlayer())) continue;
			ritual.stop(strInterrupted);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (!ritual.isChanneler(event.getEntity())) continue;
			ritual.stop(strInterrupted);
		}
	}

	private boolean hasThisSpell(Player player) {
		return MagicSpells.getSpellbook(player).hasSpell(this);
	}

	private class ActiveRitual implements Runnable {

		private final Player caster;
		private final float power;
		private final String[] args;

		private final Map<Player, Location> channelers;
		private final int taskId;

		private final int tickInterval;
		private final int effectInterval;
		private final int ritualDuration;
		private final int reqParticipants;

		private int duration = 0;

		private ActiveRitual(Player caster, float power, String[] args) {
			this.caster = caster;
			this.power = power;
			this.args = args;

			channelers = new HashMap<>();
			channelers.put(caster, caster.getLocation());

			tickInterval = RitualSpell.this.tickInterval.get(caster, null, power, args);
			effectInterval = RitualSpell.this.effectInterval.get(caster, null, power, args);
			ritualDuration = RitualSpell.this.ritualDuration.get(caster, null, power, args);
			reqParticipants = RitualSpell.this.reqParticipants.get(caster, null, power, args);

			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);

			if (showProgressOnExpBar) MagicSpells.getExpBarManager().lock(caster, this);
			playSpellEffects(EffectPosition.CASTER, caster);
		}

		private void addChanneler(Player player) {
			if (channelers.containsKey(player)) return;
			channelers.put(player, player.getLocation());
			if (showProgressOnExpBar) MagicSpells.getExpBarManager().lock(player, this);
			playSpellEffects(EffectPosition.CASTER, player);
		}

		private void removeChanneler(Player player) {
			channelers.remove(player);
		}

		private boolean isChanneler(Player player) {
			return channelers.containsKey(player);
		}

		@Override
		public void run() {
			duration += tickInterval;
			int count = channelers.size();
			boolean interrupted = false;
			Iterator<Map.Entry<Player, Location>> iter = channelers.entrySet().iterator();

			while (iter.hasNext()) {
				Player player = iter.next().getKey();

				// Check for movement/death/offline
				Location oldloc = channelers.get(player);
				Location newloc = player.getLocation();
				if (!player.isOnline() || player.isDead() || Math.abs(oldloc.getX() - newloc.getX()) > 0.2 || Math.abs(oldloc.getY() - newloc.getY()) > 0.2 || Math.abs(oldloc.getZ() - newloc.getZ()) > 0.2) {
					if (player.equals(caster)) {
						interrupted = true;
						break;
					} else {
						iter.remove();
						count--;
						resetManaBar(player);
						if (!strRitualLeft.isEmpty()) sendMessage(strRitualLeft, player, MagicSpells.NULL_ARGS);
						continue;
					}
				}
				// Send exp bar update
				if (showProgressOnExpBar)
					MagicSpells.getExpBarManager().update(player, count, (float) duration / (float) ritualDuration, this);

				// Spell effect
				if (duration % effectInterval == 0) playSpellEffects(EffectPosition.CASTER, player);
			}

			if (interrupted) {
				stop(strRitualInterrupted);
				if (spellOnInterrupt != null && caster.isValid()) {
					if (spellOnInterrupt.isTargetedLocationSpell())
						spellOnInterrupt.castAtLocation(caster, caster.getLocation(), power);
					else spellOnInterrupt.cast(caster, power);
				}
			}

			if (duration >= ritualDuration) {
				// Channel is done
				if (count >= reqParticipants && !caster.isDead() && caster.isOnline()) {
					if (chargeReagentsImmediately || hasReagents(caster)) {
						stop(strRitualSuccess);
						playSpellEffects(EffectPosition.DELAYED, caster);
						PostCastAction action = spellToCast.castSpell(caster, SpellCastState.NORMAL, power, args);
						if (!chargeReagentsImmediately && action.chargeReagents()) removeReagents(caster);
						if (!setCooldownImmediately && action.setCooldown()) setCooldown(caster, cooldown);
						if (setCooldownForAll && action.setCooldown()) {
							for (Player p : channelers.keySet()) {
								setCooldown(p, cooldown);
							}
						}
					} else stop(strRitualFailed);
				} else stop(strRitualFailed);
			}
		}

		private void stop(String message) {
			for (Player player : channelers.keySet()) {
				sendMessage(message, player, args);
				resetManaBar(player);
			}
			channelers.clear();
			Bukkit.getScheduler().cancelTask(taskId);
			activeRituals.remove(caster);
		}

		private void resetManaBar(Player player) {
			MagicSpells.getExpBarManager().unlock(player, this);
			MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			if (MagicSpells.getManaHandler() != null) MagicSpells.getManaHandler().showMana(player);
		}

	}

}
