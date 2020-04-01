package com.nisovin.magicjutsus.jutsus.command;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class BindJutsu extends CommandJutsu {
	
	private Set<CastItem> bindableItems;

	private Set<Jutsu> allowedJutsus;

	private boolean allowBindToFist;

	private String strUsage;
	private String strNoJutsu;
	private String strCantBindItem;
	private String strCantBindJutsu;
	private String strJutsuCantBind;

	public BindJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		List<String> bindables = getConfigStringList("bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String s : bindables) {
				bindableItems.add(new CastItem(s));
			}
		}

		List<String> allowedJutsuNames = getConfigStringList("allowed-jutsus", null);
		if (allowedJutsuNames != null && !allowedJutsuNames.isEmpty()) {
			allowedJutsus = new HashSet<>();
			for (String name: allowedJutsuNames) {
				Jutsu s = MagicJutsus.getJutsuByInternalName(name);
				if (s != null) allowedJutsus.add(s);
				else MagicJutsus.plugin.getLogger().warning("Invalid jutsu listed: " + name);

			}
		}

		allowBindToFist = getConfigBoolean("allow-bind-to-fist", false);

		strUsage = getConfigString("str-usage", "You must specify a jutsu name and hold an item in your hand.");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strCantBindItem = getConfigString("str-cant-bind-item", "That jutsu cannot be bound to that item.");
		strCantBindJutsu = getConfigString("str-cant-bind-jutsu", "That jutsu cannot be bound to an item.");
		strJutsuCantBind = getConfigString("str-jutsu-cant-bind", "That jutsu cannot be bound like this.");
	}
	
	// DEBUG INFO: level 3, trying to bind jutsu internalname to cast item castitemstring
	// DEBUG INFO: level 3, performing bind
	// DEBUG INFO: level 3, bind successful
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
				sendMessage(strJutsuCantBind, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			CastItem castItem = new CastItem(player.getEquipment().getItemInMainHand());
			MagicJutsus.debug(3, "Trying to bind jutsu '" + jutsu.getInternalName() + "' to cast item " + castItem.toString() + "...");

			if (BlockUtils.isAir(castItem.getItemType()) && !allowBindToFist) {
				sendMessage(strCantBindItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (bindableItems != null && !bindableItems.contains(castItem)) {
				sendMessage(strCantBindItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!jutsu.canBind(castItem)) {
				String msg = jutsu.getCantBindError();
				if (msg == null) msg = strCantBindItem;
				sendMessage(msg, player, args);
				return PostCastAction.NO_MESSAGES;
			}

			MagicJutsus.debug(3, "    Performing bind...");
			jutsubook.addCastItem(jutsu, castItem);
			jutsubook.save();
			MagicJutsus.debug(3, "    Bind successful.");
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
