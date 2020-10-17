package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.player.PlayerEditBookEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable is optional
// If not specified, it will trigger on any book
// If specified, it should be a comma separated list of page text to trigger on
public class SignBookListener extends PassiveListener {

	private final List<String> text = new ArrayList<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		text.addAll(Arrays.asList(split));
	}

	@OverridePriority
	@EventHandler
	public void onBookEdit(PlayerEditBookEvent event) {
		Player player = event.getPlayer();
		BookMeta meta = event.getNewBookMeta();
		if (!meta.hasAuthor()) return;
		if (!hasSpell(player)) return;

		if (text.isEmpty()) {
			passiveSpell.activate(player);
			return;
		}

		for (int i = 1; i <= meta.getPageCount(); i++) {
			if (!text.contains(meta.getPage(i))) continue;
			passiveSpell.activate(player);
			return;
		}
	}

}
