package com.nisovin.magicjutsus.shop;

import org.bukkit.permissions.Permissible;

public enum Perm {
	
	CREATESIGNSHOP("magicjutsus.createsignshop")
	
	;
	
	private final String node;
	public String getNode() { return this.node; }
	
	Perm(String node) {
		this.node = node;
	}
	
	public boolean has(Permissible permissible) {
		return permissible.hasPermission(this.node);
	}
	
}
