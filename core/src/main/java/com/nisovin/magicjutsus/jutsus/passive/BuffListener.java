package com.nisovin.magicjutsus.jutsus.passive;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.events.JutsuForgetEvent;

// No trigger variable currently used
public class BuffListener extends PassiveListener {

	private List<PassiveJutsu> jutsus = new ArrayList<>();

	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
		for (Ninjutsu s : jutsu.getActivatedJutsus()) {
			if (!(s.getJutsu() instanceof BuffJutsu)) continue;
			BuffJutsu buff = (BuffJutsu) s.getJutsu();
			buff.setAsEverlasting();
		}
	}

	@Override
	public void initialize() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			on(player);
		}
	}

	@OverridePriority
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		on(event.getPlayer());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		off(event.getPlayer());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		off(event.getEntity());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		MagicJutsus.scheduleDelayedTask(() -> on(event.getPlayer()), 1);
	}

	@OverridePriority
	@EventHandler
	public void onJutsuLearn(final JutsuLearnEvent event) {
		if (event.getJutsu() instanceof PassiveJutsu && jutsus.contains(event.getJutsu())) {
			MagicJutsus.scheduleDelayedTask(() -> on(event.getLearner(), (PassiveJutsu) event.getJutsu()), 1);
		}
	}

	@OverridePriority
	@EventHandler
	public void onJutsuForget(JutsuForgetEvent event) {
		if (event.getJutsu() instanceof PassiveJutsu && jutsus.contains(event.getJutsu())) {
			off(event.getForgetter(), (PassiveJutsu) event.getJutsu());
		}
	}

	private void on(Player player) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		for (PassiveJutsu jutsu : jutsus) {
			if (jutsubook.hasJutsu(jutsu)) on(player, jutsu);
		}
	}

	private void on(Player player, PassiveJutsu jutsu) {
		for (Ninjutsu s : jutsu.getActivatedJutsus()) {
			if (!(s.getJutsu() instanceof BuffJutsu)) continue;
			BuffJutsu buff = (BuffJutsu) s.getJutsu();
			if (buff.isActive(player)) continue;
			buff.castAtEntity(player, player, 1F);
		}
	}

	private void off(Player player) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		for (PassiveJutsu jutsu : jutsus) {
			if (jutsubook.hasJutsu(jutsu)) off(player, jutsu);
		}
	}

	private void off(Player player, PassiveJutsu jutsu) {
		for (Ninjutsu s : jutsu.getActivatedJutsus()) {
			if (!(s.getJutsu() instanceof BuffJutsu)) continue;
			BuffJutsu buff = (BuffJutsu) s.getJutsu();
			if (!buff.isActive(player)) continue;
			buff.turnOff(player);
		}
	}

}
