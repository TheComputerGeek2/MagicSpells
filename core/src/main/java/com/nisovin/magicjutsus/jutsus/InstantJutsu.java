package com.nisovin.magicjutsus.jutsus;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public abstract class InstantJutsu extends Jutsu {
	
	private boolean castWithItem;
	private boolean castByCommand;
	
	public InstantJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
	}
	
	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}
	
}
