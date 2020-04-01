package com.nisovin.magicjutsus;

import org.bukkit.permissions.Permissible;

public enum Perm {
	
	SILENT("magicjutsus.silent"),
	NOREAGENTS("magicjutsus.noreagents"),
	NOCOOLDOWN("magicjutsus.nocooldown"),
	NOCASTTIME("magicjutsus.nocasttime"),
	NOTARGET("magicjutsus.notarget"),
	ADVANCEDJUTSUBOOK("magicjutsus.advanced.jutsubook"),
	ADVANCED_IMBUE("magicjutsus.advanced.imbue"),
	CAST("magicjutsus.cast."),
	LEARN("magicjutsus.learn."),
	GRANT("magicjutsus.grant."),
	TEMPGRANT("magicjutsus.tempgrant."),
	TEACH("magicjutsus.teach."),
	ADVANCED("magicjutsus.advanced."),
	ADVANCED_LIST("magicjutsus.advanced.list"),
	ADVANCED_FORGET("magicjutsus.advanced.forget"),
	ADVANCED_SCROLL("magicjutsus.advanced.scroll"),
	SHOW_VARIABLE(null, true),
	MODIFY_VARIABLE(null, true),
	MODIFY_MANA(null, true),
	SET_MAX_MANA(null, true),
	UPDATE_MANA_RANK(null, true),
	RESET_MANA(null, true),
	SET_MANA(null, true),
	MAGICITEM(null, true),
	DOWNLOAD(null, true),
	UPDATE(null, true),
	SAVESKIN(null, true),
	PROFILE(null, true),
	DEBUG(null, true),
	FORCECAST(null, true),
	RELOAD(null, true),
	RESET_COOLDOWN(null, true),
	CAST_AT(null, true),
	
	;

	private final String node;
	private final boolean requireOp;
	private final boolean requireNode;

	Perm(String node) {
		this(node, false);
	}

	Perm(String node, boolean requireOp) {
		this.node = node;
		this.requireOp = requireOp;
		requireNode = node != null;
	}

	public String getNode() {
		return node;
	}

	public String getNode(Jutsu jutsu) {
		return node + jutsu.getPermissionName();
	}

	public boolean requiresOp() {
		return requireOp;
	}

	public boolean requiresNode() {
		return requireNode;
	}
	
	public boolean has(Permissible permissible) {
		if (requiresOp() && !permissible.isOp()) return false;
		if (requiresNode() && !permissible.hasPermission(getNode())) return false;
		return true;
	}
	
	public boolean has(Permissible permissible, Jutsu jutsu) {
		if (requiresOp() && !permissible.isOp()) return false;
		if (requiresNode() && !permissible.hasPermission(getNode(jutsu))) return false;
		return true;
	}
	
}
