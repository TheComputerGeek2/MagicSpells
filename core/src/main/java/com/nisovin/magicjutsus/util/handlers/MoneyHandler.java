package com.nisovin.magicjutsus.util.handlers;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import com.nisovin.magicjutsus.util.compat.CompatBasics;

public class MoneyHandler {

	private Economy economy;

	public MoneyHandler() {
		RegisteredServiceProvider<Economy> provider = CompatBasics.getServiceProvider(Economy.class);
		if (provider != null) economy = provider.getProvider();
	}

	public boolean hasMoney(Player player, float money) {
		if (economy == null) return false;
		return economy.has(player, money);
	}

	public void removeMoney(Player player, float money) {
		if (economy == null) return;
		economy.withdrawPlayer(player, money);
	}

	public void addMoney(Player player, float money) {
		if (economy == null) return;
		economy.depositPlayer(player, money);
	}

	public double checkMoney(Player player) {
		if (economy == null) return 0;
		return economy.getBalance(player);
	}

}
