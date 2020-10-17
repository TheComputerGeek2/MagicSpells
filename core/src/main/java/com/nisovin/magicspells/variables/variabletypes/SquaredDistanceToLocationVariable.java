package com.nisovin.magicspells.variables.variabletypes;

import org.bukkit.Location;

public class SquaredDistanceToLocationVariable extends DistanceToLocationVariable {

	public SquaredDistanceToLocationVariable() {
		super();
	}
	
	@Override
	protected double calculateReportedDistance(double multiplier, Location origin, Location target) {
		return target.distanceSquared(origin) * multiplier;
	}
	
}
