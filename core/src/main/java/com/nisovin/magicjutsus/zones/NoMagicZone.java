package com.nisovin.magicjutsus.zones;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.JutsuFilter;

public abstract class NoMagicZone implements Comparable<NoMagicZone> {

	private String id;
	private String message;

	private int priority;

	private boolean allowAll;
	private boolean disallowAll;

	private JutsuFilter jutsuFilter;
	
	public final void create(String id, ConfigurationSection config) {
		this.id = id;
		message = config.getString("message", "You are in a no-magic zone.");

		priority = config.getInt("priority", 0);

		allowAll = config.getBoolean("allow-all", false);
		disallowAll = config.getBoolean("disallow-all", true);

		List<String> allowedJutsus = config.getStringList("allowed-jutsus");
		List<String> disallowedJutsus = config.getStringList("disallowed-jutsus");
		List<String> allowedJutsuTags = config.getStringList("allowed-jutsu-tags");
		List<String> disallowedJutsuTags = config.getStringList("disallowed-jutsu-tags");
		jutsuFilter = new JutsuFilter(allowedJutsus, disallowedJutsus, allowedJutsuTags, disallowedJutsuTags);

		initialize(config);
	}
	
	public abstract void initialize(ConfigurationSection config);
	
	public final ZoneCheckResult check(Player player, Jutsu jutsu) {
		return check(player.getLocation(), jutsu);
	}
	
	public final ZoneCheckResult check(Location location, Jutsu jutsu) {
		if (!inZone(location)) return ZoneCheckResult.IGNORED;
		if (disallowAll) return ZoneCheckResult.DENY;
		if (allowAll) return ZoneCheckResult.ALLOW;
		if (!jutsuFilter.check(jutsu)) return ZoneCheckResult.DENY;
		if (jutsuFilter.check(jutsu)) return ZoneCheckResult.ALLOW;
		return ZoneCheckResult.IGNORED;
	}
	
	public abstract boolean inZone(Location location);
	
	public String getId() {
		return id;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public int compareTo(NoMagicZone other) {
		if (priority < other.priority) return 1;
		if (priority > other.priority) return -1;
		return id.compareTo(other.id);
	}
	
	public enum ZoneCheckResult {
		
		ALLOW,
		DENY,
		IGNORED
		
	}
	
}
