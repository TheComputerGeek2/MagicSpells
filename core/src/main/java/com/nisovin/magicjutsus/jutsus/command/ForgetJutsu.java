package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuForgetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

// Advanced perm allows you to make others forget a jutsu
// Put * for the jutsu to forget all of them

public class ForgetJutsu extends CommandJutsu {

	private boolean allowSelfForget;

	private String strUsage;
	private String strNoJutsu;
	private String strNoTarget;
	private String strResetSelf;
	private String strDoesntKnow;
	private String strCastTarget;
	private String strResetTarget;
	private String strCastSelfTarget;

	public ForgetJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		allowSelfForget = getConfigBoolean("allow-self-forget", true);

		strUsage = getConfigString("str-usage", "Usage: /cast forget <target> <jutsu>");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strResetSelf = getConfigString("str-reset-self", "You have forgotten all of your jutsus.");
		strDoesntKnow = getConfigString("str-doesnt-know", "That person does not know that jutsu.");
		strCastTarget = getConfigString("str-cast-target", "%a has made you forget the %s jutsu.");
		strResetTarget = getConfigString("str-reset-target", "You have reset %t's jutsubook.");
		strCastSelfTarget = getConfigString("str-cast-self-target", "You have forgotten the %s jutsu.");
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0 || args.length > 2) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Jutsubook casterJutsubook = MagicJutsus.getJutsubook(player);
			
			Player target;
			if (args.length == 1 && allowSelfForget) target = player;
			else if (args.length == 2 && casterJutsubook.hasAdvancedPerm("forget")) {
				List<Player> players = MagicJutsus.plugin.getServer().matchPlayer(args[0]);
				if (players.size() != 1) {
					sendMessage(strNoTarget, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				target = players.get(0);
			} else {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			String jutsuName = args.length == 1 ? args[0] : args[1];
			boolean all = false;
			Jutsu jutsu = null;
			if (jutsuName.equals("*")) all = true;
			else jutsu = MagicJutsus.getJutsuByInGameName(jutsuName);

			if (jutsu == null && !all) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (!all && !casterJutsubook.hasJutsu(jutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Jutsubook targetJutsubook = MagicJutsus.getJutsubook(target);
			if (targetJutsubook == null || (!all && !targetJutsubook.hasJutsu(jutsu))) {
				sendMessage(strDoesntKnow, player, args);
				return PostCastAction.ALREADY_HANDLED;
			} 
			
			// Remove jutsu(s)
			if (!all) {
				targetJutsubook.removeJutsu(jutsu);
				targetJutsubook.save();
				if (!player.equals(target)) {
					sendMessage(formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", jutsu.getName(), "%t", target.getDisplayName()), target, args);
					sendMessage(formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", jutsu.getName(), "%t", target.getDisplayName()), player, args);
					playJutsuEffects(player, target);
				} else {
					sendMessage(formatMessage(strCastSelfTarget, "%s", jutsu.getName()), player, args);
					playJutsuEffects(EffectPosition.CASTER, player);
				}
				return PostCastAction.NO_MESSAGES;
			}

			targetJutsubook.removeAllJutsus();
			targetJutsubook.addGrantedJutsus();
			targetJutsubook.save();

			if (!player.equals(target)) {
				sendMessage(formatMessage(strResetTarget, "%t", target.getDisplayName()), player, args);
				playJutsuEffects(player, target);
			} else {
				sendMessage(strResetSelf, player, args);
				playJutsuEffects(EffectPosition.CASTER, player);
			}
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			sender.sendMessage(strUsage);
			return true;
		}
		Player target = PlayerNameUtils.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(strNoTarget);
			return true;
		}
		Jutsu jutsu = null;
		boolean all = false;
		if (args[1].equals("*")) all = true;
		else jutsu = MagicJutsus.getJutsuByInGameName(args[1]);

		if (jutsu == null && !all) {
			sender.sendMessage(strNoJutsu);
			return true;
		}

		Jutsubook targetJutsubook = MagicJutsus.getJutsubook(target);
		if (targetJutsubook == null || (!all && !targetJutsubook.hasJutsu(jutsu))) {
			sender.sendMessage(strDoesntKnow);
			return true;
		}

		JutsuForgetEvent forgetEvent = new JutsuForgetEvent(jutsu, target);
		EventUtil.call(forgetEvent);
		if (forgetEvent.isCancelled()) return true;
		if (!all) {
			targetJutsubook.removeJutsu(jutsu);
			targetJutsubook.save();
			sendMessage(formatMessage(strCastTarget, "%a", getConsoleName(), "%s", jutsu.getName(), "%t", target.getDisplayName()), target, args);
			sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", jutsu.getName(), "%t", target.getDisplayName()));
		} else {
			targetJutsubook.removeAllJutsus();
			targetJutsubook.addGrantedJutsus();
			targetJutsubook.save();
			sender.sendMessage(formatMessage(strResetTarget, "%t", target.getDisplayName()));
		}
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) {
			// Matching player name or jutsu name
			List<String> options = new ArrayList<>();
			List<String> players = tabCompletePlayerName(sender, args[0]);
			List<String> jutsus = tabCompleteJutsuName(sender, args[0]);
			if (players != null) options.addAll(players);
			if (jutsus != null) options.addAll(jutsus);
			if (!options.isEmpty()) return options;
		}

		if (args.length == 2) return tabCompleteJutsuName(sender, args[1]);
		return null;
	}

}
