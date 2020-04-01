package com.nisovin.magicjutsus.jutsus.command;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class UnbindJutsu extends CommandJutsu {

	private List<String> allowedJutsusNames;
	private Set<Jutsu> allowedJutsus = null;

	private String strUsage;
	private String strNoJutsu;
	private String strNotBound;
	private String strUnbindAll;
	private String strCantUnbind;
	private String strCantBindJutsu;

	public UnbindJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		allowedJutsusNames = getConfigStringList("allowed-jutsus", null);
		if (allowedJutsusNames != null && !allowedJutsusNames.isEmpty()) {
			allowedJutsus = new HashSet<>();
			for (String n: allowedJutsusNames) {
				Jutsu s = MagicJutsus.getJutsuByInternalName(n);
				if (s != null) allowedJutsus.add(s);
				else MagicJutsus.plugin.getLogger().warning("Invalid jutsu defined: " + n);
			}
		}

		strUsage = getConfigString("str-usage", "You must specify a jutsu name.");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strNotBound = getConfigString("str-not-bound", "That jutsu is not bound to that item.");
		strUnbindAll = getConfigString("str-unbind-all", "All jutsus from your item were cleared.");
		strCantUnbind = getConfigString("str-cant-unbind", "You cannot unbind this jutsu");
		strCantBindJutsu = getConfigString("str-cant-bind-jutsu", "That jutsu cannot be bound to an item.");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			CastItem item = new CastItem(player.getEquipment().getItemInMainHand());
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);

			if (args[0] != null && args[0].equalsIgnoreCase("*")) {
				List<Jutsu> jutsus = new ArrayList<>();

				for (CastItem i : jutsubook.getItemJutsus().keySet()) {
					if (!i.equals(item)) continue;
					jutsus.addAll(jutsubook.getItemJutsus().get(i));
				}

				for (Jutsu s : jutsus) {
					jutsubook.removeCastItem(s, item);
				}

				jutsubook.save();
				sendMessage(strUnbindAll, player, args);
				playJutsuEffects(EffectPosition.CASTER, player);
				return PostCastAction.NO_MESSAGES;
			}

			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(Util.arrayJoin(args, ' '));
			if (jutsu == null || jutsubook == null) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!jutsubook.hasJutsu(jutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!jutsu.canCastWithItem()) {
				sendMessage(strCantBindJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (allowedJutsus != null && !allowedJutsus.contains(jutsu)) {
				sendMessage(strCantUnbind, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			boolean removed = jutsubook.removeCastItem(jutsu, item);
			if (!removed) {
				sendMessage(strNotBound, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			jutsubook.save();
			sendMessage(formatMessage(strCastSelf, "%s", jutsu.getName()), player, args);
			playJutsuEffects(EffectPosition.CASTER, player);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player && !partial.contains(" ")) return tabCompleteJutsuName(sender, partial);
		return null;
	}

}
