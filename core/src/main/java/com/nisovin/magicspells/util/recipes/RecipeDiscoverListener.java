package com.nisovin.magicspells.util.recipes;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

public class RecipeDiscoverListener implements Listener {

	private static final ScopedValue<Void> SUPPRESS_TOAST = ScopedValue.newInstance();

	@EventHandler
	private void onDiscover(PlayerRecipeDiscoverEvent event) {
		if (!SUPPRESS_TOAST.isBound()) return;
		event.shouldShowNotification(false);
	}

	public static void suppressToastWhile(Runnable op) {
		ScopedValue.where(SUPPRESS_TOAST, null).run(op);
	}

}
