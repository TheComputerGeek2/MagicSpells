package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RitualSpell extends InstantSpell {

	private final Map<Player, ActiveRitual> activeRituals;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> effectInterval;
	private final ConfigData<Integer> ritualDuration;
	private final ConfigData<Integer> reqParticipants;

	private final ConfigData<Boolean> setCooldownForAll;
	private final ConfigData<Boolean> showProgressOnExpBar;
	private final ConfigData<Boolean> setCooldownImmediately;
	private final ConfigData<Boolean> needSpellToParticipate;
	private final ConfigData<Boolean> chargeReagentsImmediately;

	private String spellToCastName;
	private Spell spellToCast;

	private final String strRitualLeft;
	private final String strRitualJoined;
	private final String strRitualFailed;
	private final String strRitualSuccess;
	private final String strRitualInterrupted;

	public RitualSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		activeRituals = new HashMap<>();

		tickInterval = getConfigDataInt("tick-interval", 5);
		effectInterval = getConfigDataInt("effect-interval", TimeUtil.TICKS_PER_SECOND);
		ritualDuration = getConfigDataInt("ritual-duration", 200);
		reqParticipants = getConfigDataInt("req-participants", 3);

		setCooldownForAll = getConfigDataBoolean("set-cooldown-for-all", true);
		showProgressOnExpBar = getConfigDataBoolean("show-progress-on-exp-bar", true);
		setCooldownImmediately = getConfigDataBoolean("set-cooldown-immediately", true);
		needSpellToParticipate = getConfigDataBoolean("need-spell-to-participate", false);
		chargeReagentsImmediately = getConfigDataBoolean("charge-reagents-immediately", true);

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
		spellToCastName = null;
	}

	@Override
	public CastResult cast(SpellCastState state, SpellData data) {
		if (spellToCast == null || !(data.caster() instanceof Player caster))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (activeRituals.containsKey(caster)) {
			ActiveRitual channel = activeRituals.remove(caster);
			channel.stop(strRitualInterrupted);
		}

		if (state != SpellCastState.NORMAL) return new CastResult(PostCastAction.HANDLE_NORMALLY, data);

		ActiveRitual ritual = new ActiveRitual(caster, data);
		activeRituals.put(caster, ritual);

		PostCastAction action;
		if (!ritual.chargeReagentsImmediately && !ritual.setCooldownImmediately) action = PostCastAction.MESSAGES_ONLY;
		else if (!ritual.chargeReagentsImmediately) action = PostCastAction.NO_REAGENTS;
		else if (!ritual.setCooldownImmediately) action = PostCastAction.NO_COOLDOWN;
		else action = PostCastAction.HANDLE_NORMALLY;

		return new CastResult(action, data);
	}

	@Override
	public CastResult cast(SpellData data) {
		return cast(SpellCastState.NORMAL, data);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
		if (!(event.getRightClicked() instanceof Player player)) return;
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) return;

		ActiveRitual channel = activeRituals.get(player);
		if (channel == null) return;

		if (!channel.needSpellToParticipate || hasThisSpell(event.getPlayer())) {
			channel.addChanneler(event.getPlayer());
			sendMessage(strRitualJoined, event.getPlayer(), channel.data);
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
		private final SpellData data;

		private final Map<Player, Location> channelers;
		private final int taskId;

		private final boolean setCooldownForAll;
		private final boolean showProgressOnExpBar;
		private final boolean setCooldownImmediately;
		private final boolean needSpellToParticipate;
		private final boolean chargeReagentsImmediately;

		private final int tickInterval;
		private final int effectInterval;
		private final int ritualDuration;
		private final int reqParticipants;

		private int duration = 0;

		private ActiveRitual(Player caster, SpellData data) {
			this.caster = caster;
			this.data = data;

			channelers = new HashMap<>();
			channelers.put(caster, caster.getLocation());

			setCooldownForAll = RitualSpell.this.setCooldownForAll.get(data);
			showProgressOnExpBar = RitualSpell.this.showProgressOnExpBar.get(data);
			setCooldownImmediately = RitualSpell.this.setCooldownImmediately.get(data);
			needSpellToParticipate = RitualSpell.this.needSpellToParticipate.get(data);
			chargeReagentsImmediately = RitualSpell.this.chargeReagentsImmediately.get(data);

			tickInterval = RitualSpell.this.tickInterval.get(data);
			effectInterval = RitualSpell.this.effectInterval.get(data);
			ritualDuration = RitualSpell.this.ritualDuration.get(data);
			reqParticipants = RitualSpell.this.reqParticipants.get(data);

			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);

			if (showProgressOnExpBar) MagicSpells.getExpBarManager().lock(caster, this);
			playSpellEffects(EffectPosition.CASTER, caster, data);
		}

		private void addChanneler(Player player) {
			if (channelers.containsKey(player)) return;
			channelers.put(player, player.getLocation());
			if (showProgressOnExpBar) MagicSpells.getExpBarManager().lock(player, this);
			playSpellEffects(EffectPosition.CASTER, player, data);
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
						if (!strRitualLeft.isEmpty()) sendMessage(strRitualLeft, player, data);
						continue;
					}
				}
				// Send exp bar update
				if (showProgressOnExpBar)
					MagicSpells.getExpBarManager().update(player, count, (float) duration / (float) ritualDuration, this);

				// Spell effect
				if (duration % effectInterval == 0) playSpellEffects(EffectPosition.CASTER, player, data);
			}

			if (interrupted) {
				stop(strRitualInterrupted);
				if (spellOnInterrupt != null && caster.isValid()) spellOnInterrupt.subcast(data.location(caster.getLocation()));
			}

			if (duration >= ritualDuration) {
				// Channel is done
				if (count >= reqParticipants && !caster.isDead() && caster.isOnline()) {
					if (chargeReagentsImmediately || hasReagents(caster)) {
						stop(strRitualSuccess);
						playSpellEffects(EffectPosition.DELAYED, caster, data);

						CastResult result = spellToCast.cast(data);
						if (!chargeReagentsImmediately && result.action().chargeReagents()) removeReagents(caster);
						if (!setCooldownImmediately && result.action().setCooldown()) setCooldown(caster, data);
						if (setCooldownForAll && result.action().setCooldown()) {
							for (Player p : channelers.keySet()) {
								setCooldown(p, data.caster(p));
							}
						}
					} else stop(strRitualFailed);
				} else stop(strRitualFailed);
			}
		}

		private void stop(String message) {
			for (Player player : channelers.keySet()) {
				sendMessage(message, player, data);
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
