package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import com.nisovin.magicjutsus.Jutsu;

public class JutsuUtil {
	
	// Currently will only work with direct permission nodes, doesn't handle child nodes yet
	// NOTE: allJutsus should be a thread safe collection for read access
	public static Collection<Jutsu> getJutsusByPermissionNames(final Collection<Jutsu> allJutsus, final Set<String> names) {
		Predicate<Jutsu> predicate = jutsu -> names.contains(jutsu.getPermissionName());
		return getJutsusByX(allJutsus, predicate);
	}
	
	// NOTE: allJutsus should be a thread safe collection for read access
	// NOTE: streams do work for making the collection thread safe
	public static Collection<Jutsu> getJutsusByX(final Collection<Jutsu> allJutsus, final Predicate<Jutsu> predicate) {
		return allJutsus
			.parallelStream()
			.filter(predicate)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
}
