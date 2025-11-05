package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileInputStream;

import org.bukkit.Location;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PasteSpell extends TargetedSpell implements TargetedLocationSpell {

	private final List<EditSession> sessions;

	private Clipboard clipboard;

	private final File file;

	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> undoDelay;

	private final ConfigData<Boolean> pasteAir;
	private final ConfigData<Boolean> removePaste;
	private final ConfigData<Boolean> pasteAtCaster;

	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);

		yOffset = getConfigDataInt("y-offset", 0);
		undoDelay = getConfigDataInt("undo-delay", 0);

		pasteAir = getConfigDataBoolean("paste-air", false);
		removePaste = getConfigDataBoolean("remove-paste", true);
		pasteAtCaster = getConfigDataBoolean("paste-at-caster", false);

		sessions = new ArrayList<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		ClipboardFormat format = ClipboardFormats.findByPath(file.toPath());
		if (format != null) {
			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				clipboard = reader.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (clipboard == null) MagicSpells.error("PasteSpell " + internalName + " has a wrong schematic!");
	}

	@Override
	public void turnOff() {
		for (EditSession session : sessions) {
			session.undo(session);
		}

		sessions.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (pasteAtCaster.get(data)) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (clipboard == null) return noTarget(data);

		Location target = data.location();
		target.add(0, yOffset.get(data), 0);
		data = data.location(target);

		try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(target.getWorld()))) {
			Operation operation = new ClipboardHolder(clipboard)
				.createPaste(editSession)
				.to(BlockVector3.at(target.getX(), target.getY(), target.getZ()))
				.ignoreAirBlocks(!pasteAir.get(data))
				.build();

			Operations.complete(operation);
			if (removePaste.get(data)) sessions.add(editSession);

			int undoDelay = this.undoDelay.get(data);
			if (undoDelay > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					editSession.undo(editSession);
					sessions.remove(editSession);
				}, undoDelay);
			}
		} catch (WorldEditException e) {
			e.printStackTrace();
			return noTarget(data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
