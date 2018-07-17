package com.nisovin.magicspells;

import org.bukkit.permissions.Permissible;

public enum Perm {
	
	SILENT("magicspells.silent"),
	NOREAGENTS("magicspells.noreagents"),
	NOCOOLDOWN("magicspells.nocooldown"),
	NOCASTTIME("magicspells.nocasttime"),
	NOTARGET("magicspells.notarget"),
	ADVANCEDSPELLBOOK("magicspells.advanced.spellbook"),
	ADVANCED_IMBUE("magicspells.advanced.imbue"),
	CAST("magicspells.cast."),
	LEARN("magicspells.learn."),
	GRANT("magicspells.grant."),
	TEMPGRANT("magicspells.tempgrant."),
	TEACH("magicspells.teach."),
	ADVANCED("magicspells.advanced."),
	ADVANCED_LIST("magicspells.advanced.list"),
	ADVANCED_FORGET("magicspells.advanced.forget"),
	ADVANCED_SCROLL("magicspells.advanced.scroll"),
	MODIFY_VARIABLE("magicspells.modifyvariable"),
	MODIFY_MANA("magicspells.mana.modify"),
	SET_MAX_MANA("magicspells.mana.setmax"),
	UPDATE_MANA_RANK("magicspells.mana.updaterank"),
	RESET_MANA("magicspells.mana.reset"),
	SET_MANA("magicspells.mana.set"),
	MAGICITEM("magicspells.magicitem"),
	DOWNLOAD("magicspells.download"),
	UPDATE("magicspells.update"),
	SAVESKIN("magicspells.saveskin"),
	PROFILE("magicspells.profile"),
	DEBUG("magicspells.debug"),
	FORCECAST("magicspells.forcecast"),
	RELOAD("magicspells.reload"),
	RESET_COOLDOWN("magicspells.reset_cooldown"),
	CAST_AT("magicspells.castat"),
	
	;
	
	private final boolean requireOp;
	public boolean requiresOp() { return this.requireOp; }
	private final boolean requireNode;
	public boolean requiresNode() { return this.requireNode; }
	private final String node;
	public String getNode() { return this.node; }
	public String getNode(Spell spell) { return this.node + spell.getPermissionName(); }
	
	public boolean has(Permissible permissible) {
		if (this.requiresOp() && !permissible.isOp()) return false;
		if (this.requiresNode() && !permissible.hasPermission(getNode())) return false;
		return true;
	}
	
	public boolean has(Permissible permissible, Spell spell) {
		if (this.requiresOp() && !permissible.isOp()) return false;
		if (this.requiresNode() && !permissible.hasPermission(getNode(spell))) return false;
		return true;
	}
	
	Perm(String node) {
		this(node, false);
	}
	
	Perm(String node, boolean requireOp) {
		this.node = node;
		this.requireNode = node != null;
		this.requireOp = requireOp;
	}
	
}
