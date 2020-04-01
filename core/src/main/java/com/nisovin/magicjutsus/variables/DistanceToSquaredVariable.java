package com.nisovin.magicjutsus.variables;

import org.bukkit.Location;

public class DistanceToSquaredVariable extends DistanceToVariable {

	public DistanceToSquaredVariable() {
		super();
	}
	
	@Override
	protected double calculateReportedDistance(double multiplier, Location origin, Location target) {
		return target.distanceSquared(origin) * multiplier;
	}
	
}
