package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import com.nisovin.magicjutsus.Jutsu;

public class JutsuFilter {

	private Set<String> allowedJutsus = null;
	private Set<String> blacklistedJutsus = null;
	private Set<String> allowedTags = null;
	private Set<String> disallowedTags = null;
	
	private boolean defaultReturn;
	private boolean emptyFilter = false;
	
	public JutsuFilter(List<String> allowedJutsus, List<String> blacklistedJutsus, List<String> allowedTags, List<String> disallowedTags) {
		
		// Initialize the collections
		if (allowedJutsus != null && !allowedJutsus.isEmpty()) this.allowedJutsus = new HashSet<>(allowedJutsus);
		if (blacklistedJutsus != null && !blacklistedJutsus.isEmpty()) this.blacklistedJutsus = new HashSet<>(blacklistedJutsus);
		if (allowedTags != null && !allowedTags.isEmpty()) this.allowedTags = new HashSet<>(allowedTags);
		if (disallowedTags != null && !disallowedTags.isEmpty()) this.disallowedTags = new HashSet<>(disallowedTags);

		// Determine the default outcome if nothing catches
		defaultReturn = determineDefaultValue();
	}
	
	private boolean determineDefaultValue() {
		// This means there is a tag whitelist check
		if (allowedTags != null) return false;
		
		// If there is a jutsu whitelist check
		if (allowedJutsus != null) return false;
		
		// This means there is a tag blacklist
		if (disallowedTags != null) return true;
		
		// If there is a jutsu blacklist
		if (blacklistedJutsus != null) return true;
		
		// If all of the collections are null, then there is no filter
		emptyFilter = true;
		return true;
	}
	
	public boolean check(Jutsu jutsu) {
		// Can't do anything if null anyway
		if (jutsu == null) return false;
		
		// Quick check to exit early if possible
		if (emptyFilter) return true;
		
		// Is it whitelisted explicitly?
		if (allowedJutsus != null && allowedJutsus.contains(jutsu.getInternalName())) return true;
		
		// Is it blacklisted?
		if (blacklistedJutsus != null && blacklistedJutsus.contains(jutsu.getInternalName())) return false;
		
		// Does it have a blacklisted tag?
		if (disallowedTags != null) {
			for (String tag : disallowedTags) {
				if (jutsu.hasTag(tag)) return false;
			}
		}
		
		// Does it have a whitelisted tag?
		if (allowedTags != null) {
			for (String tag : allowedTags) {
				if (jutsu.hasTag(tag)) return true;
			}
		}
		
		return defaultReturn;
	}
	
	public static JutsuFilter fromConfig(MagicConfig config, String basePath) {
		basePath = basePath +  '.';
		List<String> jutsus = config.getStringList(basePath + "jutsus", null);
		List<String> deniedJutsus = config.getStringList(basePath + "denied-jutsus", null);
		List<String> tagList = config.getStringList(basePath + "jutsu-tags", null);
		List<String> deniedTagList = config.getStringList(basePath + "denied-jutsu-tags", null);
		return new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);
	}
	
}
