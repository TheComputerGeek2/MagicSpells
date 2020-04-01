package com.nisovin.magicjutsus.variables;

public class GlobalVariable extends Variable {

	private double value = 0;
	
	@Override
	protected void init() {
		value = defaultValue;
	}
	
	@Override
	public boolean modify(String player, double amount) {
		double newvalue = value + amount;

		if (newvalue > maxValue) newvalue = maxValue;
		else if (newvalue < minValue) newvalue = minValue;

		if (value != newvalue) {
			value = newvalue;
			return true;
		}
		return false;
	}

	@Override
	public void set(String player, double amount) {
		value = amount;
	}

	@Override
	public double getValue(String player) {
		return value;
	}

	@Override
	public void reset(String player) {
		value = defaultValue;
	}

}
