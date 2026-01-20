package com.nisovin.magicspells.volatilecode;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.volatilecode.latest.VolatileCodeLatest;

public class ManagerVolatile {

	private static final Map<String, String> COMPATIBLE_VERSIONS = Map.of(
	);

	private static final VolatileCodeHelper helper = new VolatileCodeHelper() {

		@Override
		public void error(String message) {
			MagicSpells.error(message);
		}

		@Override
		public int scheduleDelayedTask(Runnable task, long delay) {
			return MagicSpells.scheduleDelayedTask(task, delay);
		}

		@Override
		public void cancelTask(int id) {
			MagicSpells.cancelTask(id);
		}

		@Override
		public void registerEvents(Listener listener) {
			MagicSpells.registerEvents(listener);
		}

		@Override
		public YamlConfiguration getMainConfig() {
			return MagicSpells.getMagicConfig().getMainConfig();
		}

		@Override
		public Plugin getPlugin() {
			return MagicSpells.getInstance();
		}

	};

	public static VolatileCodeHandle constructVolatileCodeHandler() {
		String mcVersion = Bukkit.getMinecraftVersion();
		String convertedVersion = COMPATIBLE_VERSIONS.getOrDefault(mcVersion, mcVersion);
		String version = "v" + convertedVersion.replace(".", "_");

		try {
			Class<?> volatileCode;
			try {
				volatileCode = Class.forName("com.nisovin.magicspells.volatilecode." + version + ".VolatileCode_" + version);
			} catch (ClassNotFoundException _) {
				VolatileCodeHandle handle = new VolatileCodeLatest(helper);
				MagicSpells.log("Using latest volatile code handler.");

				return handle;
			}

			VolatileCodeHandle handle = (VolatileCodeHandle) volatileCode.getConstructor(VolatileCodeHelper.class).newInstance(helper);
			MagicSpells.log("Found volatile code handler for '" + mcVersion + "'.");

			return handle;
		} catch (Throwable throwable) {
			MagicSpells.error("Failed to initialize volatile code handler for '" + mcVersion + "'.");
			throwable.printStackTrace();

			return new VolatileCodeDisabled();
		}
	}

}
