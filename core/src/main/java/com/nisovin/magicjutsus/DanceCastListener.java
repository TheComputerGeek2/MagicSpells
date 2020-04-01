package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.Jutsu.PostCastAction;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.Jutsu.JutsuCastResult;

public class DanceCastListener implements Listener {
	
	private static final Pattern DANCE_CAST_PATTERN = Pattern.compile("[CSUJLRFBA]+");
	
	private MagicJutsus plugin;

	private CastItem danceCastWand;

	private int duration;
	
	// TODO make this a map of dance cast items to maps of sequences to jutsus
	private Map<String, Jutsu> jutsus = new HashMap<>();

	private Map<String, String> playerCasts = new HashMap<>();
	private Map<String, Integer> playerTasks = new HashMap<>();
	private Map<String, Location> playerLocations = new HashMap<>();

	private boolean dynamicCasting = false;
	private boolean enableMovement = false;
	private boolean enableDoubleJump = false;

	private String startSound = null;
	private float startSoundVolume = 1;
	private float startSoundPitch = 1;

	private String strDanceFail;
	private String strDanceStart;
	private String strDanceComplete;

	// DEBUG INFO: level 2, dance cast registered internalname sequence
	public DanceCastListener(MagicJutsus plugin, MagicConfig config) {
		this.plugin = plugin;

		String path = "general.";
		danceCastWand = new CastItem(Util.getItemStackFromString(config.getString(path + "dance-cast-item", "MUSIC_DISC_13")));
		duration = config.getInt(path + "dance-cast-duration", 200);
		dynamicCasting = config.getBoolean(path + "dance-cast-dynamic", false);
		
		startSound = config.getString(path + "dance-cast-start-sound", null);
		if (startSound != null && startSound.contains(",")) {
			String[] split = startSound.split(",");
			startSound = split[0];
			startSoundVolume = Float.parseFloat(split[1]);
			if (split.length > 2) startSoundPitch = Float.parseFloat(split[2]);
		}

		strDanceFail = config.getString(path + "str-dance-fail", "Your dancing has no effect.");
		strDanceStart = config.getString(path + "str-dance-start", "You begin to cast a jutsu.");
		strDanceComplete = config.getString(path + "str-dance-complete", "");

		for (Jutsu jutsu : MagicJutsus.jutsus()) {
			String seq = jutsu.getDanceCastSequence();
			if (seq == null) continue;
			if (!RegexUtil.matches(DANCE_CAST_PATTERN, seq)) continue;
			jutsus.put(seq, jutsu);
			if (seq.contains("D")) enableDoubleJump = true;
			if (seq.contains("F") || seq.contains("B") || seq.contains("L") || seq.contains("R") || seq.contains("J")) enableMovement = true;
			MagicJutsus.debug("Dance cast registered: " + jutsu.getInternalName() + " - " + seq);
		}
		
		if (!jutsus.isEmpty()) MagicJutsus.registerEvents(this);
	}
	
	private boolean processDanceCast(Player player, String castSequence, boolean forceEnd) {
		boolean casted = false;
		Jutsu jutsu = jutsus.get(castSequence);
		if (jutsu != null) {
			MagicJutsus.sendMessage(strDanceComplete, player, MagicJutsus.NULL_ARGS);
			JutsuCastResult result = jutsu.cast(player);
			casted = result.state == JutsuCastState.NORMAL && result.action != PostCastAction.ALREADY_HANDLED;
		} else if (forceEnd) {
			MagicJutsus.sendMessage(strDanceFail, player, MagicJutsus.NULL_ARGS);
		}
		if (casted || forceEnd) {
			String playerName = player.getName();
			if (enableDoubleJump) {
				player.setFlying(false);
				player.setAllowFlight(false);
			}
			playerLocations.remove(playerName);
			Integer taskId = playerTasks.remove(playerName);
			if (taskId != null) MagicJutsus.cancelTask(taskId.intValue());
			playerCasts.remove(playerName);
		}
		return casted;
	}
	
	// DEBUG INFO: level 2, player playername performance dance sequence sequence
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (!event.hasItem()) return;
		if (!danceCastWand.equals(event.getItem())) return;
		
		Action action = event.getAction();
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			String castSequence = playerCasts.remove(playerName);
			if (castSequence != null) {
				castSequence = processMovement(player, castSequence);
				MagicJutsus.debug("Player " + player.getName() + " performed dance sequence " + castSequence);
				processDanceCast(player, castSequence, true);
				return;
			}
			// starting a cast
			if (!player.isSneaking() && !player.isFlying()) {
				playerCasts.put(playerName, "");
				playerLocations.put(playerName, player.getLocation());
				if (enableDoubleJump) {
					player.setAllowFlight(true);
					player.setFlying(false);
				}
				MagicJutsus.sendMessage(strDanceStart, player, MagicJutsus.NULL_ARGS);
				if (startSound != null) player.playSound(player.getLocation(), startSound, startSoundVolume, startSoundPitch);
				if (duration > 0) playerTasks.put(playerName, MagicJutsus.scheduleDelayedTask(new DanceCastDuration(playerName), duration));
			}

			return;
		}

		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			String castSequence = playerCasts.get(playerName);
			if (castSequence == null) return;
			castSequence = processMovement(player, castSequence) + 'C';
			playerCasts.put(playerName, castSequence);
			if (dynamicCasting) processDanceCast(player, castSequence, false);
		}
	}
	
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		String castSequence = playerCasts.get(playerName);
		if (castSequence == null) return;
		castSequence = processMovement(player, castSequence) + (event.isSneaking() ? "S" : "U");
		playerCasts.put(playerName, castSequence);
		if (dynamicCasting) processDanceCast(player, castSequence, false);
	}
	
	@EventHandler
	public void onFly(PlayerToggleFlightEvent event) {
		if (!enableDoubleJump) return;
		Player player = event.getPlayer();
		String playerName = player.getName();
		String castSequence = playerCasts.get(playerName);
		if (castSequence != null && event.isFlying()) {
			event.setCancelled(true);
			castSequence = processMovement(player, castSequence);
			playerCasts.put(playerName, castSequence + 'D');
		}
	}
	
	private String processMovement(Player player, String castSequence) {
		if (!enableMovement) return castSequence;

		Location firstLoc = playerLocations.get(player.getName());
		playerLocations.put(player.getName(), player.getLocation());

		if (firstLoc == null || firstLoc.distanceSquared(player.getLocation()) < 0.04F) return castSequence;

		float facing = firstLoc.getYaw();
		if (facing < 0) facing += 360;
		float dir = (float) Util.getYawOfVector(player.getLocation().toVector().subtract(firstLoc.toVector()));
		if (dir < 0) dir += 360;
		float diff = facing - dir;
		if (diff < 0) diff += 360;

		if (diff < 20 || diff > 340) castSequence += "F";
		else if (70 < diff && diff < 110) castSequence += "L";
		else if (160 < diff && diff < 200) castSequence += "B";
		else if (250 < diff && diff < 290) castSequence += "R";

		if (player.getLocation().getY() - firstLoc.getY() > 0.4) castSequence += "J";

		return castSequence;
	}
	
	private class DanceCastDuration implements Runnable {
		
		private String playerName;
		
		DanceCastDuration(String playerName) {
			this.playerName = playerName;
		}
		
		@Override
		public void run() {
			String cast = playerCasts.remove(playerName);
			playerLocations.remove(playerName);
			playerTasks.remove(playerName);
			if (cast == null) return;
			Player player = PlayerNameUtils.getPlayerExact(playerName);
			if (player != null) MagicJutsus.sendMessage(strDanceFail, player, MagicJutsus.NULL_ARGS);
		}
	}
	
}
