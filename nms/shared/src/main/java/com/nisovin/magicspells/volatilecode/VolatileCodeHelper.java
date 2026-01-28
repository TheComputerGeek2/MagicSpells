package com.nisovin.magicspells.volatilecode;

import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.bukkit.configuration.file.YamlConfiguration;

public interface VolatileCodeHelper {

	void error(String message);

	int scheduleDelayedTask(Runnable task, long delay);

	void cancelTask(int id);

	void registerEvents(Listener listener);

	YamlConfiguration getMainConfig();

	Plugin getPlugin();

}
