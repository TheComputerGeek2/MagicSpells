package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class GameModeChangeListener extends PassiveListener {

	private final EnumSet<GameMode> gameModes = EnumSet.noneOf(GameMode.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		GameMode gameMode;
		try {
			gameMode = GameMode.valueOf(var.toUpperCase());
			gameModes.add(gameMode);
		} catch (Exception e) {
			// ignored
		}
	}

	@OverridePriority
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;

		if (gameModes.isEmpty()) {
			boolean casted = passiveSpell.activate(player);
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}

		if (!gameModes.contains(event.getNewGameMode())) return;

		boolean casted = passiveSpell.activate(player);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
