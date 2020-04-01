package com.nisovin.magicjutsus.jutsus.instant;

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

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class RitualJutsu extends InstantJutsu {

	private Map<Player, ActiveRitual> activeRituals;

	private int tickInterval;
	private int effectInterval;
	private int ritualDuration;
	private int reqParticipants;

	private boolean setCooldownForAll;
	private boolean showProgressOnExpBar;
	private boolean setCooldownImmediately;
	private boolean needJutsuToParticipate;
	private boolean chargeReagentsImmediately;

	private String jutsuToCastName;
	private Jutsu jutsuToCast;

	private String strRitualLeft;
	private String strRitualJoined;
	private String strRitualFailed;
	private String strRitualSuccess;
	private String strRitualInterrupted;

	public RitualJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		activeRituals = new HashMap<>();

		tickInterval = getConfigInt("tick-interval", 5);
		effectInterval = getConfigInt("effect-interval", TimeUtil.TICKS_PER_SECOND);
		ritualDuration = getConfigInt("ritual-duration", 200);
		reqParticipants = getConfigInt("req-participants", 3);

		setCooldownForAll = getConfigBoolean("set-cooldown-for-all", true);
		showProgressOnExpBar = getConfigBoolean("show-progress-on-exp-bar", true);
		setCooldownImmediately = getConfigBoolean("set-cooldown-immediately", true);
		needJutsuToParticipate = getConfigBoolean("need-jutsu-to-participate", false);
		chargeReagentsImmediately = getConfigBoolean("charge-reagents-immediately", true);

		jutsuToCastName = getConfigString("jutsu", "");

		strRitualLeft = getConfigString("str-ritual-left", "");
		strRitualJoined = getConfigString("str-ritual-joined", "");
		strRitualFailed = getConfigString("str-ritual-failed", "");
		strRitualSuccess = getConfigString("str-ritual-success", "");
		strRitualInterrupted = getConfigString("str-ritual-interrupted", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuToCast = MagicJutsus.getJutsuByInternalName(jutsuToCastName);
		if (jutsuToCast == null) MagicJutsus.error("RitualJutsu '" + internalName + "' has an invalid jutsu defined!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (jutsuToCast == null || !(livingEntity instanceof Player)) return PostCastAction.ALREADY_HANDLED;
		Player player = (Player) livingEntity;
		if (activeRituals.containsKey(player)) {
			ActiveRitual channel = activeRituals.remove(player);
			channel.stop(strRitualInterrupted);
		}
		if (state == JutsuCastState.NORMAL) {
			activeRituals.put(player, new ActiveRitual(player, power, args));
			if (!chargeReagentsImmediately && !setCooldownImmediately) return PostCastAction.MESSAGES_ONLY;
			if (!chargeReagentsImmediately) return PostCastAction.NO_REAGENTS;
			if (!setCooldownImmediately) return PostCastAction.NO_COOLDOWN;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Player)) return;
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) return;
		ActiveRitual channel = activeRituals.get(event.getRightClicked());
		if (channel == null) return;
		if (!needJutsuToParticipate || hasThisJutsu(event.getPlayer())) {
			channel.addChanneler(event.getPlayer());
			sendMessage(strRitualJoined, event.getPlayer(), MagicJutsus.NULL_ARGS);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (!ritual.isChanneler(event.getPlayer())) continue;
			ritual.stop(strInterrupted);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (!ritual.isChanneler(event.getEntity())) continue;
			ritual.stop(strInterrupted);
		}
	}
	
	private boolean hasThisJutsu(Player player) {
		return MagicJutsus.getJutsubook(player).hasJutsu(this);
	}
	
	private class ActiveRitual implements Runnable {
		
		private Player caster;
		private float power;
		private String[] args;
		private int duration = 0;
		private int taskId;
		private Map<Player, Location> channelers;

		private ActiveRitual(Player caster, float power, String[] args) {
			this.caster = caster;
			this.power = power;
			this.args = args;
			channelers = new HashMap<>();

			channelers.put(caster, caster.getLocation());
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicJutsus.plugin, this, tickInterval, tickInterval);
			if (showProgressOnExpBar) MagicJutsus.getExpBarManager().lock(caster, this);
			playJutsuEffects(EffectPosition.CASTER, caster);
		}

		private void addChanneler(Player player) {
			if (channelers.containsKey(player)) return;
			channelers.put(player, player.getLocation());
			if (showProgressOnExpBar) MagicJutsus.getExpBarManager().lock(player, this);
			playJutsuEffects(EffectPosition.CASTER, player);
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
						if (!strRitualLeft.isEmpty()) sendMessage(strRitualLeft, player, MagicJutsus.NULL_ARGS);
						continue;
					}
				}
				// Send exp bar update
				if (showProgressOnExpBar) MagicJutsus.getExpBarManager().update(player, count, (float) duration / (float) ritualDuration, this);

				// Jutsu effect
				if (duration % effectInterval == 0) playJutsuEffects(EffectPosition.CASTER, player);
			}

			if (interrupted) {
				stop(strRitualInterrupted);
				if (jutsuOnInterrupt != null && caster.isValid()) jutsuOnInterrupt.castJutsu(caster, JutsuCastState.NORMAL, power, MagicJutsus.NULL_ARGS);
			}
			
			if (duration >= ritualDuration) {
				// Channel is done
				if (count >= reqParticipants && !caster.isDead() && caster.isOnline()) {
					if (chargeReagentsImmediately || hasReagents(caster)) {
						stop(strRitualSuccess);
						playJutsuEffects(EffectPosition.DELAYED, caster);
						PostCastAction action = jutsuToCast.castJutsu(caster, JutsuCastState.NORMAL, power, args);
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
				sendMessage(message, player, MagicJutsus.NULL_ARGS);
				resetManaBar(player);
			}
			channelers.clear();
			Bukkit.getScheduler().cancelTask(taskId);
			activeRituals.remove(caster);
		}
		
		private void resetManaBar(Player player) {
			MagicJutsus.getExpBarManager().unlock(player, this);
			MagicJutsus.getExpBarManager().update(player, player.getLevel(), player.getExp());
			if (MagicJutsus.getManaHandler() != null) MagicJutsus.getManaHandler().showMana(player);
		}
		
	}

}
