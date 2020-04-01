package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.events.JutsuLearnEvent.LearnSource;

public class TeachJutsu extends CommandJutsu {

	private boolean requireKnownJutsu;

	private String strUsage;
	private String strNoJutsu;
	private String strNoTarget;
	private String strCantTeach;
	private String strCantLearn;
	private String strCastTarget;
	private String strAlreadyKnown;

	public TeachJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		requireKnownJutsu = getConfigBoolean("require-known-jutsu", true);

		strUsage = getConfigString("str-usage", "Usage: /cast teach <target> <jutsu>");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strCantTeach = getConfigString("str-cant-teach", "You can't teach that jutsu.");
		strCantLearn = getConfigString("str-cant-learn", "That person cannot learn that jutsu.");
		strCastTarget = getConfigString("str-cast-target", "%a has taught you the %s jutsu.");
		strAlreadyKnown = getConfigString("str-already-known", "That person already knows that jutsu.");
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length != 2) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			List<Player> players = MagicJutsus.plugin.getServer().matchPlayer(args[0]);
			if (players.size() != 1) {
				sendMessage(strNoTarget, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[1]);
			Player target = players.get(0);
			if (jutsu == null) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			if (jutsubook == null || (!jutsubook.hasJutsu(jutsu) && requireKnownJutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!jutsubook.canTeach(jutsu)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Jutsubook targetJutsubook = MagicJutsus.getJutsubook(target);
			if (targetJutsubook == null || !targetJutsubook.canLearn(jutsu)) {
				sendMessage(strCantLearn, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (targetJutsubook.hasJutsu(jutsu)) {
				sendMessage(strAlreadyKnown, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			boolean cancelled = callEvent(jutsu, target, player);
			if (cancelled) {
				sendMessage(strCantLearn, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			targetJutsubook.addJutsu(jutsu);
			targetJutsubook.save();
			sendMessage(formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", jutsu.getName(), "%t", target.getDisplayName()), target, args);
			sendMessage(formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", jutsu.getName(), "%t", target.getDisplayName()), player, args);
			playJutsuEffects(player, target);
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
		List<Player> players = MagicJutsus.plugin.getServer().matchPlayer(args[0]);
		if (players.size() != 1) {
			sender.sendMessage(strNoTarget);
			return true;
		}
		Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[1]);
		if (jutsu == null) {
			sender.sendMessage(strNoJutsu);
			return true;
		}
		Jutsubook targetJutsubook = MagicJutsus.getJutsubook(players.get(0));
		if (targetJutsubook == null || !targetJutsubook.canLearn(jutsu)) {
			sender.sendMessage(strCantLearn);
			return true;
		}
		if (targetJutsubook.hasJutsu(jutsu)) {
			sender.sendMessage(strAlreadyKnown);
			return true;
		}
		boolean cancelled = callEvent(jutsu, players.get(0), sender);
		if (cancelled) {
			sender.sendMessage(strCantLearn);
			return true;
		}
		targetJutsubook.addJutsu(jutsu);
		targetJutsubook.save();
		sendMessage(formatMessage(strCastTarget, "%a", getConsoleName(), "%s", jutsu.getName(), "%t", players.get(0).getDisplayName()), players.get(0), args);
		sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", jutsu.getName(), "%t", players.get(0).getDisplayName()));
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) return tabCompletePlayerName(sender, args[0]);
		if (args.length == 2) return tabCompleteJutsuName(sender, args[1]);
		return null;
	}
	
	private boolean callEvent(Jutsu jutsu, Player learner, Object teacher) {
		JutsuLearnEvent event = new JutsuLearnEvent(jutsu, learner, LearnSource.TEACH, teacher);
		EventUtil.call(event);
		return event.isCancelled();
	}

}
