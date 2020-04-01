package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;
import java.util.Collection;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;

// Advanced perm is for listing other player's jutsus

public class SublistJutsu extends CommandJutsu {

	private List<String> jutsusToHide;
	private List<String> jutsusToShow;

	private int lineLength = 60;

	private boolean reloadGrantedJutsus;
	private boolean onlyShowCastableJutsus;

	private String strPrefix;
	private String strNoJutsus;

	public SublistJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		jutsusToHide = getConfigStringList("jutsus-to-hide", null);
		jutsusToShow = getConfigStringList("jutsus-to-show", null);

		reloadGrantedJutsus = getConfigBoolean("reload-granted-jutsus", true);
		onlyShowCastableJutsus = getConfigBoolean("only-show-castable-jutsus", false);

		strPrefix = getConfigString("str-prefix", "Known jutsus:");
		strNoJutsus = getConfigString("str-no-jutsus", "You do not know any jutsus.");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			String extra = "";
			if (args != null && args.length > 0 && jutsubook.hasAdvancedPerm("list")) {
				Player p = PlayerNameUtils.getPlayer(args[0]);
				if (p != null) {
					jutsubook = MagicJutsus.getJutsubook(p);
					extra = '(' + p.getDisplayName() + ") ";
				}
			}
			if (jutsubook != null && reloadGrantedJutsus) jutsubook.addGrantedJutsus();
			if (jutsubook == null || jutsubook.getJutsus().isEmpty()) {
				sendMessage(strNoJutsus, player, args);
				return PostCastAction.HANDLE_NORMALLY;
			}

			String s = "";
			for (Jutsu jutsu : jutsubook.getJutsus()) {
				if (!jutsu.isHelperJutsu() && (!onlyShowCastableJutsus || jutsubook.canCast(jutsu))
						&& !(jutsusToHide != null && jutsusToHide.contains(jutsu.getInternalName()))
						&& (jutsusToShow == null || jutsusToShow.contains(jutsu.getInternalName()))) {
					if (s.isEmpty()) s = jutsu.getName();
					else s += ", " + jutsu.getName();
				}
			}
			s = strPrefix + ' ' + extra + s;
			while (s.length() > lineLength) {
				int i = s.substring(0, lineLength).lastIndexOf(' ');
				sendMessage(s.substring(0, i), player, args);
				s = s.substring(i + 1);
			}
			if (!s.isEmpty()) sendMessage(s, player, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		StringBuilder s = new StringBuilder();

		Collection<Jutsu> jutsus = MagicJutsus.jutsus();
		if (args != null && args.length > 0) {
			Player p = PlayerNameUtils.getPlayer(args[0]);
			if (p == null) {
				sender.sendMessage("No such player.");
				return true;
			}
			jutsus = MagicJutsus.getJutsubook(p).getJutsus();
			s.append(p.getName()).append("'s jutsus: ");
		} else s.append("All jutsus: ");

		for (Jutsu jutsu : jutsus) {
			s.append(jutsu.getName());
			s.append(' ');
		}

		sender.sendMessage(s.toString());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof ConsoleCommandSender && !partial.contains(" ")) return tabCompletePlayerName(sender, partial);
		return null;
	}

}
