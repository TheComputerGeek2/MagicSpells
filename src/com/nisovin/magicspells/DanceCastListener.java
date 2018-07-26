package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.nisovin.magicspells.util.RegexUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.ConfigData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.Util;

public class DanceCastListener implements Listener {
	
	private static final Pattern DANCE_CAST_PATTERN = Pattern.compile("[CSUJLRFBA]+");
	
	MagicSpells plugin;
	
	@ConfigData(field="general.dance-cast-item", dataType="String")
	CastItem danceCastWand;
	
	@ConfigData(field="general.dance-cast-duration", dataType="int")
	int duration;
	
	// TODO make this a map of dance cast items to maps of sequences to spells
	Map<String, Spell> spells = new HashMap<>();
	
	Map<String, String> playerCasts = new HashMap<>();
	Map<String, Location> playerLocations = new HashMap<>();
	Map<String, Integer> playerTasks = new HashMap<>();
	
	@ConfigData(field="general.dance-cast-dynamic", dataType="boolean", defaultValue="false")
	boolean dynamicCasting = false;
	
	boolean enableDoubleJump = false;
	
	boolean enableMovement = false;
	
	@ConfigData(field="general.dance-cast-start-sound", dataType="String", defaultValue="null")
	String startSound = null;
	float startSoundVolume = 1;
	float startSoundPitch = 1;
	
	@ConfigData(field="general.str-dance-start", dataType="String")
	String strDanceStart;
	
	@ConfigData(field="general.str-dance-complete", dataType="String")
	String strDanceComplete;
	
	@ConfigData(field="general.str-dance-fail", dataType="String")
	String strDanceFail;
	
	// DEBUG INFO: level 2, dance cast registered internalname sequence
	public DanceCastListener(MagicSpells plugin, MagicConfig config) {
		this.plugin = plugin;
		
		danceCastWand = new CastItem(Util.getItemStackFromString(config.getString("general.dance-cast-item", "music_disc_cat")));
		duration = config.getInt("general.dance-cast-duration", 200);
		dynamicCasting = config.getBoolean("general.dance-cast-dynamic", false);
		
		startSound = config.getString("general.dance-cast-start-sound", null);
		if (startSound != null && startSound.contains(",")) {
			String[] split = startSound.split(",");
			startSound = split[0];
			startSoundVolume = Float.parseFloat(split[1]);
			if (split.length > 2) startSoundPitch = Float.parseFloat(split[2]);
		}
		
		strDanceStart = config.getString("general.str-dance-start", "You begin to cast a spell.");
		strDanceComplete = config.getString("general.str-dance-complete", "");
		strDanceFail = config.getString("general.str-dance-fail", "Your dancing has no effect.");
		
		for (Spell spell : MagicSpells.spells()) {
			String seq = spell.getDanceCastSequence();
			if (seq == null) continue;
			if (!RegexUtil.matches(DANCE_CAST_PATTERN, seq)) continue;
			spells.put(seq, spell);
			if (seq.contains("D")) enableDoubleJump = true;
			if (seq.contains("F") || seq.contains("B") || seq.contains("L") || seq.contains("R") || seq.contains("J")) enableMovement = true;
			MagicSpells.debug("Dance cast registered: " + spell.getInternalName() + " - " + seq);
		}
		
		if (!spells.isEmpty()) MagicSpells.registerEvents(this);
	}
	
	private boolean processDanceCast(Player player, String castSequence, boolean forceEnd) {
		boolean casted = false;
		Spell spell = spells.get(castSequence);
		if (spell != null) {
			MagicSpells.sendMessage(strDanceComplete, player, MagicSpells.NULL_ARGS);
			SpellCastResult result = spell.cast(player);
			casted = result.state == SpellCastState.NORMAL && result.action != PostCastAction.ALREADY_HANDLED;
		} else if (forceEnd) {
			MagicSpells.sendMessage(strDanceFail, player, MagicSpells.NULL_ARGS);
		}
		if (casted || forceEnd) {
			String playerName = player.getName();
			if (enableDoubleJump) {
				player.setFlying(false);
				player.setAllowFlight(false);
			}
			playerLocations.remove(playerName);
			Integer taskId = playerTasks.remove(playerName);
			if (taskId != null) MagicSpells.cancelTask(taskId.intValue());
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
				MagicSpells.debug("Player " + player.getName() + " performed dance sequence " + castSequence);
				processDanceCast(player, castSequence, true);
			} else {
				// starting a cast
				if (!player.isSneaking() && !player.isFlying()) {
					playerCasts.put(playerName, "");
					playerLocations.put(playerName, player.getLocation());
					if (enableDoubleJump) {
						player.setAllowFlight(true);
						player.setFlying(false);
					}
					MagicSpells.sendMessage(strDanceStart, player, MagicSpells.NULL_ARGS);
					if (startSound != null) {
						MagicSpells.getVolatileCodeHandler().playSound(player, startSound, startSoundVolume, startSoundPitch);
						//SoundUtils.playSound(player, startSound, startSoundVolume, startSoundPitch); //the new system again
					}
					if (duration > 0) playerTasks.put(playerName, MagicSpells.scheduleDelayedTask(new DanceCastDuration(playerName), duration));
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
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
	
	String processMovement(Player player, String castSequence) {
		if (!enableMovement) return castSequence;
		
		Location firstLoc = playerLocations.get(player.getName());
		playerLocations.put(player.getName(), player.getLocation());
		
		if (firstLoc == null || firstLoc.distanceSquared(player.getLocation()) < 0.04F) return castSequence;
		
		float facing = firstLoc.getYaw();
		if (facing < 0) facing += 360;
		float dir = (float)Util.getYawOfVector(player.getLocation().toVector().subtract(firstLoc.toVector()));
		if (dir < 0) dir += 360;
		float diff = facing - dir;
		if (diff < 0) diff += 360;
		
		if (diff < 20 || diff > 340) {
			castSequence += "F";
		} else if (70 < diff && diff < 110) {
			castSequence += "L";
		} else if (160 < diff && diff < 200) {
			castSequence += "B";
		} else if (250 < diff && diff < 290) {
			castSequence += "R";
		}
		
		if (player.getLocation().getY() - firstLoc.getY() > 0.4) castSequence += "J";
		
		return castSequence;
	}
	
	public class DanceCastDuration implements Runnable {
		
		String playerName;
		
		public DanceCastDuration(String playerName) {
			this.playerName = playerName;
		}
		
		@Override
		public void run() {
			String cast = playerCasts.remove(playerName);
			playerLocations.remove(playerName);
			playerTasks.remove(playerName);
			if (cast == null) return;
			Player player = PlayerNameUtils.getPlayerExact(playerName);
			if (player != null) MagicSpells.sendMessage(strDanceFail, player, MagicSpells.NULL_ARGS);
		}
	}
	
}
