package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileInputStream;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

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

public class PasteSpell extends TargetedSpell implements TargetedLocationSpell {

	private List<EditSession> sessions;

	private File file;
	private Clipboard clipboard;

	private int yOffset;
	private int undoDelay;

	private boolean pasteAir;
	private boolean removePaste;
	private boolean pasteAtCaster;
	
	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);
		
		yOffset = getConfigInt("y-offset", 0);
		undoDelay = getConfigInt("undo-delay", 0);
		if (undoDelay < 0) undoDelay = 0;

		pasteAir = getConfigBoolean("paste-air", false);
		removePaste = getConfigBoolean("remove-paste", true);
		pasteAtCaster = getConfigBoolean("paste-at-caster", false);

		sessions = new ArrayList<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
			clipboard = reader.read();
		} catch (IOException e) {
			e.printStackTrace();
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pasteAtCaster ? caster.getLocation().getBlock() : getTargetedBlock(caster, power);
			if (target == null) return noTarget(caster);
			Location loc = target.getLocation();
			loc.add(0, yOffset, 0);
			boolean ok = castAtLocation(caster, loc, power, args);
			if (!ok) return noTarget(caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		boolean ok = pasteInstant(target, args);
		if (!ok) return false;
		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

	private boolean pasteInstant(Location target, String[] args) {
		if (clipboard == null) return false;

		try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(target.getWorld()), -1)) {
			Operation operation = new ClipboardHolder(clipboard)
					.createPaste(editSession)
					.to(BlockVector3.at(target.getX(), target.getY(), target.getZ()))
					.ignoreAirBlocks(!pasteAir)
					.build();
			Operations.complete(operation);
			if (removePaste) sessions.add(editSession);

			if (undoDelay > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					editSession.undo(editSession);
					sessions.remove(editSession);
				}, undoDelay);
			}
		} catch (WorldEditException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
