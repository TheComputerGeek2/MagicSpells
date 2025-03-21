package com.nisovin.magicspells.util.glow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;

public class LibsDisguiseHelper {

	public static String getDisguisedScoreboardEntryName(@Nullable Player player, @NotNull Entity entity) {
		Disguise disguise = player == null ? DisguiseAPI.getDisguise(entity) : DisguiseAPI.getDisguise(player, entity);
		if (disguise == null) return entity.getScoreboardEntryName();

		return disguise instanceof PlayerDisguise pd ? pd.getProfileName() : disguise.getUUID().toString();
	}

}
