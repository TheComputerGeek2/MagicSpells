package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;

public class HelpJutsu extends CommandJutsu {
	
	private boolean requireKnownJutsu;

	private String strUsage;
	private String strNoJutsu;
	private String strDescLine;
	private String strCostLine;

	public HelpJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		requireKnownJutsu = getConfigBoolean("require-known-jutsu", true);

		strUsage = getConfigString("str-usage", "Usage: /cast " + name + " <jutsu>");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strDescLine = getConfigString("str-desc-line", "%s - %d");
		strCostLine = getConfigString("str-cost-line", "Cost: %c");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(Util.arrayJoin(args, ' '));
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);

			if (jutsu == null || (requireKnownJutsu && (jutsubook == null || !jutsubook.hasJutsu(jutsu)))) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			sendMessage(formatMessage(strDescLine, "%s", jutsu.getName(), "%d", jutsu.getDescription()), player, args);
			if (jutsu.getCostStr() != null && !jutsu.getCostStr().isEmpty()) {
				sendMessage(formatMessage(strCostLine, "%c", jutsu.getCostStr()), player, args);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String [] args = Util.splitParams(partial);
		if (sender instanceof Player && args.length == 1) return tabCompleteJutsuName(sender, partial);
		return null;
	}

}
