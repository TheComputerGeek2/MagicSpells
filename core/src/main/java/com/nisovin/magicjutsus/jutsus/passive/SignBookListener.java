package com.nisovin.magicjutsus.jutsus.passive;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.player.PlayerEditBookEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Trigger variable is optional
// If not specified, it will trigger on any book
// If specified, it should be a comma separated list of page text to trigger on
public class SignBookListener extends PassiveListener {

	private List<PassiveJutsu> jutsus = new ArrayList<>();
	private Map<String, List<PassiveJutsu>> types = new HashMap<>();

	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			jutsus.add(jutsu);
			return;
		}

		String[] split = var.split(",");
		for (String s : split) {
			List<PassiveJutsu> passives = types.computeIfAbsent(s, p -> new ArrayList<>());
			passives.add(jutsu);
		}
	}

	@OverridePriority
	@EventHandler
	public void onBookEdit(PlayerEditBookEvent event) {
		Player player = event.getPlayer();
		BookMeta meta = event.getNewBookMeta();
		if (!meta.hasAuthor()) return;

		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		if (!jutsus.isEmpty()) {
			for (PassiveJutsu jutsu : jutsus) {
				if (!jutsubook.hasJutsu(jutsu)) continue;
				jutsu.activate(player);
			}
		}

		for (int i = 1; i <= meta.getPageCount(); i++) {
			if (!types.containsKey(meta.getPage(i))) continue;
			List<PassiveJutsu> list = types.get(meta.getPage(i));
			for (PassiveJutsu jutsu : list) {
				if (!jutsubook.hasJutsu(jutsu)) continue;
				jutsu.activate(player);
				return;
			}

		}
	}

}
