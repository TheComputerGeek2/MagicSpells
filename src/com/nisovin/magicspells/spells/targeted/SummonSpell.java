package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.List;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;

public class SummonSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private boolean requireExactName;
	private boolean requireAcceptance;
	private int maxAcceptDelay;
	private String acceptCommand;
	private String strUsage;
	private String strSummonPending;
	private String strSummonAccepted;
	private String strSummonExpired;
	
	private HashMap<Player,Location> pendingSummons;
	private HashMap<Player,Long> pendingTimes;
	
	public SummonSpell(MagicConfig config, String spellName) {
		super(config, spellName);		
		
		requireExactName = getConfigBoolean("require-exact-name", false);
		requireAcceptance = getConfigBoolean("require-acceptance", true);
		maxAcceptDelay = getConfigInt("max-accept-delay", 90);
		acceptCommand = getConfigString("accept-command", "accept");
		strUsage = getConfigString("str-usage", "Usage: /cast summon <playername>, or /cast summon \nwhile looking at a sign with a player name on the first line.");
		strSummonPending = getConfigString("str-summon-pending", "You are being summoned! Type /accept to teleport.");
		strSummonAccepted = getConfigString("str-summon-accepted", "You have been summoned.");
		strSummonExpired = getConfigString("str-summon-expired", "The summon has expired.");

		if (requireAcceptance) {
			pendingSummons = new HashMap<>();
			pendingTimes = new HashMap<>();
		}
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// Get target name and landing location
			String targetName = "";
			Location landLoc = null;
			if (args != null && args.length > 0) {
				targetName = args[0];
				landLoc = player.getLocation().add(0, .25, 0);
			} else {
				Block block = getTargetedBlock(player, 10);
				if (block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN)) {
					Sign sign = (Sign)block.getState();
					targetName = sign.getLine(0);
					landLoc = block.getLocation().add(.5, .25, .5);
				}
			}
			
			// Check usage
			if (targetName.isEmpty()) {
				// Fail -- show usage
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Check location
			if (landLoc == null || !BlockUtils.isSafeToStand(landLoc.clone())) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Get player
			Player target = null;
			if (requireExactName) {
				target = PlayerNameUtils.getPlayer(targetName);
				if (target != null && !target.getName().equalsIgnoreCase(targetName)) target = null;
			} else {
				List<Player> players = Bukkit.getServer().matchPlayer(targetName);
				if (players != null && players.size() == 1) {
					target = players.get(0);
				}
			}
			if (target == null) {
				// Fail -- no player target
				return noTarget(player);
			}
			
			// Teleport player
			if (requireAcceptance) {
				pendingSummons.put(target, landLoc);
				pendingTimes.put(target, System.currentTimeMillis());
				sendMessage(formatMessage(strSummonPending, "%a", player.getDisplayName()), target, args);
			} else {
				target.teleport(landLoc);
				sendMessage(formatMessage(strSummonAccepted, "%a", player.getDisplayName()), target, args);
			}
			
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!requireAcceptance) return;
		if (!event.getMessage().equalsIgnoreCase('/' + acceptCommand)) return;
		if (!pendingSummons.containsKey(event.getPlayer())) return;
		
		Player player = event.getPlayer();
		if (maxAcceptDelay > 0 && pendingTimes.get(player) + maxAcceptDelay * TimeUtil.MILLISECONDS_PER_SECOND < System.currentTimeMillis()) {
			// Waited too long
			sendMessage(strSummonExpired, player, MagicSpells.NULL_ARGS);
		} else {
			// All ok, teleport
			player.teleport(pendingSummons.get(player));
			sendMessage(strSummonAccepted, player, MagicSpells.NULL_ARGS);
		}
		pendingSummons.remove(player);
		pendingTimes.remove(player);
		event.setCancelled(true);
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (partial.contains(" ")) return null;
		return tabCompletePlayerName(sender, partial);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return target.teleport(caster);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

}
