package com.nisovin.magicspells;

import org.jetbrains.annotations.NotNull;

import org.bukkit.permissions.Permissible;

import org.incendo.cloud.permission.Permission;

public enum Perm implements Permission {
	
	SILENT("magicspells.silent"),
	NO_REAGENTS("magicspells.noreagents"),
	NO_COOLDOWN("magicspells.nocooldown"),
	NO_CAST_TIME("magicspells.nocasttime"),
	NO_TARGET("magicspells.notarget"),
	ADVANCED_SPELLBOOK("magicspells.advanced.spellbook"),
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

	// Command permissions.
	COMMAND_HELP("magicspells.command.help"),
	COMMAND_RELOAD("magicspells.command.reload"),
	COMMAND_RELOAD_SPELLBOOK("magicspells.command.reload.spellbook"),
	COMMAND_RELOAD_EFFECTLIB("magicspells.command.reload.effectlib"),
	COMMAND_RESET_COOLDOWN("magicspells.command.resetcd"),
	COMMAND_MANA_SHOW("magicspells.command.mana.show"),
	COMMAND_MANA_RESET("magicspells.command.mana.reset"),
	COMMAND_MANA_SET_MAX("magicspells.command.mana.setmax"),
	COMMAND_MANA_ADD("magicspells.command.mana.add"),
	COMMAND_MANA_SET("magicspells.command.mana.set"),
	COMMAND_MANA_UPDATE_RANK("magicspells.command.mana.updaterank"),
	COMMAND_VARIABLE_SHOW("magicspells.command.variable.show"),
	COMMAND_VARIABLE_MODIFY("magicspells.command.variable.modify"),
	COMMAND_MAGIC_ITEM("magicspells.command.magicitem"),
	COMMAND_UTIL_DOWNLOAD("magicspells.command.util.download"),
	COMMAND_UTIL_UPDATE("magicspells.command.util.update"),
	COMMAND_UTIL_SAVE_SKIN("magicspells.command.util.saveskin"),
	COMMAND_UTIL_LIST_GOALS("magicspells.command.util.listgoals"),
	COMMAND_PROFILE_REPORT("magicspells.command.profilereport"),
	COMMAND_DEBUG("magicspells.command.debug"),
	COMMAND_TASKINFO("magicspells.command.taskinfo"),
	COMMAND_MAGICXP("magicspells.command.magicxp"),
	COMMAND_CAST_POWER("magicspells.command.cast.power"),
	COMMAND_CAST_SELF("magicspells.command.cast.self"),
	COMMAND_CAST_SELF_HELPER("magicspells.command.cast.self.helper"),
	COMMAND_CAST_AS("magicspells.command.cast.as"),
	COMMAND_CAST_ON("magicspells.command.cast.on"),
	COMMAND_CAST_AT("magicspells.command.cast.at"),

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

	@Override
	public String toString() {
		return node;
	}

	public String getNode(Spell spell) {
		return node + spell.getPermissionName();
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
	
	public boolean has(Permissible permissible, Spell spell) {
		if (requiresOp() && !permissible.isOp()) return false;
		if (requiresNode() && !permissible.hasPermission(getNode(spell))) return false;
		return true;
	}

	@Override
	public @NotNull String permissionString() {
		return node;
	}

}
