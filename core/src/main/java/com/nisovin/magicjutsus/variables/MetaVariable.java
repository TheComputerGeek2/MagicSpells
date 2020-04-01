package com.nisovin.magicjutsus.variables;

public abstract class MetaVariable extends Variable {

	public MetaVariable() {
		permanent = false;
		bossBar = null;
		expBar = false;
		objective = null;
		minValue = Double.MIN_VALUE;
	}
	
	@Override
	protected void init() {
		permanent = false;
	}
	
	@Override
	public boolean modify(String player, double amount) {
		// No op
		return false;
	}

	@Override
	public void set(String player, double amount) {
		// No op
	}
	
	@Override
	public void reset(String player) {
		// No op
	}

}
