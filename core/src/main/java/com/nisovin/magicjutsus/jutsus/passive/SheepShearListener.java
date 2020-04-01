package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerShearEntityEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable that can either be set to a dye color to accept or "all"
public class SheepShearListener extends PassiveListener {

	EnumMap<DyeColor, List<PassiveJutsu>> jutsuMap = new EnumMap<>(DyeColor.class);
	List<PassiveJutsu> allColorJutsus = new ArrayList<>();
	
	List<PassiveJutsu> jutsusLoaded = new ArrayList<>();
	List<PassiveJutsu> jutsusDeclined = new ArrayList<>();
	List<PassiveJutsu> jutsusFailed = new ArrayList<>();
	List<PassiveJutsu> jutsusAccepted = new ArrayList<>();
	
	@Override
	public void initialize() {
		super.initialize();
		for (DyeColor c: DyeColor.values()) {
			jutsuMap.put(c, new ArrayList<>());
		}
	}
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.equalsIgnoreCase("all")) {
			allColorJutsus.add(jutsu);
		} else {
			DyeColor c = DyeColor.valueOf(var.toUpperCase());
			if (c == null) throw new IllegalArgumentException("Cannot resolve " + var + " to DyeColor");
			jutsuMap.get(DyeColor.valueOf(var.toUpperCase())).add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onSheepShear(PlayerShearEntityEvent event) {
		if (!(event.getEntity() instanceof Sheep)) return;
		Sheep s = (Sheep)event.getEntity();
		Player p = event.getPlayer();
		List<PassiveJutsu> jutsus = jutsuMap.get(s.getColor());
		Jutsubook jutsubook = MagicJutsus.getJutsubook(p);
		for (PassiveJutsu jutsu : jutsus) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
			if (!jutsubook.hasJutsu(jutsu)) continue;
			boolean casted = jutsu.activate(p);
			if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
		}
		for (PassiveJutsu jutsu: allColorJutsus) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
			if (!jutsubook.hasJutsu(jutsu)) continue;
			boolean casted = jutsu.activate(p);
			if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
		}
	}

}
