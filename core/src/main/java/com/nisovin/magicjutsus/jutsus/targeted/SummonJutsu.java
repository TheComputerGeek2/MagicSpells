package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class SummonJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private Map<Player, Location> pendingSummons;
	private Map<Player, Long> pendingTimes;

	private int maxAcceptDelay;

	private boolean requireExactName;
	private boolean requireAcceptance;

	private String strUsage;
	private String acceptCommand;
	private String strSummonPending;
	private String strSummonExpired;
	private String strSummonAccepted;

	public SummonJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		maxAcceptDelay = getConfigInt("max-accept-delay", 90);

		requireExactName = getConfigBoolean("require-exact-name", false);
		requireAcceptance = getConfigBoolean("require-acceptance", true);

		strUsage = getConfigString("str-usage", "Usage: /cast summon <playername>, or /cast summon \nwhile looking at a sign with a player name on the first line.");
		acceptCommand = getConfigString("accept-command", "accept");
		strSummonPending = getConfigString("str-summon-pending", "You are being summoned! Type /accept to teleport.");
		strSummonExpired = getConfigString("str-summon-expired", "The summon has expired.");
		strSummonAccepted = getConfigString("str-summon-accepted", "You have been summoned.");

		if (requireAcceptance) {
			pendingSummons = new HashMap<>();
			pendingTimes = new HashMap<>();
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && caster instanceof Player) {
			// Get target name and landing location
			String targetName = "";
			Location landLoc = null;
			if (args != null && args.length > 0) {
				targetName = args[0];
				landLoc = caster.getLocation().add(0, .25, 0);
			} else {
				Block block = getTargetedBlock(caster, 10);
				if (block != null && (block.getType().name().contains("SIGN"))) {
					Sign sign = (Sign)block.getState();
					targetName = sign.getLine(0);
					landLoc = block.getLocation().add(.5, .25, .5);
				}
			}
			
			// Check usage
			if (targetName.isEmpty()) {
				// Fail -- show usage
				sendMessage(strUsage, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Check location
			if (landLoc == null || !BlockUtils.isSafeToStand(landLoc.clone())) {
				sendMessage(strUsage, caster, args);
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
			if (target == null) return noTarget(caster);

			// Teleport player
			if (requireAcceptance) {
				pendingSummons.put(target, landLoc);
				pendingTimes.put(target, System.currentTimeMillis());
				sendMessage(formatMessage(strSummonPending, "%a", ((Player) caster).getDisplayName()), target, args);
			} else {
				target.teleport(landLoc);
				sendMessage(formatMessage(strSummonAccepted, "%a", ((Player) caster).getDisplayName()), target, args);
			}
			
			sendMessages(caster, target);
			return PostCastAction.NO_MESSAGES;
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return target.teleport(caster);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!requireAcceptance) return;
		if (!event.getMessage().equalsIgnoreCase('/' + acceptCommand)) return;
		if (!pendingSummons.containsKey(event.getPlayer())) return;

		Player player = event.getPlayer();
		if (maxAcceptDelay > 0 && pendingTimes.get(player) + maxAcceptDelay * TimeUtil.MILLISECONDS_PER_SECOND < System.currentTimeMillis()) {
			sendMessage(strSummonExpired, player, MagicJutsus.NULL_ARGS);
		} else {
			player.teleport(pendingSummons.get(player));
			sendMessage(strSummonAccepted, player, MagicJutsus.NULL_ARGS);
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

}
