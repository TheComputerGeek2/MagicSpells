package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.function.Predicate;

import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.RegistryKey;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PasteSpell extends TargetedSpell implements TargetedLocationSpell {

	private final List<EditSession> sessions = new ArrayList<>();

	private Clipboard clipboard;

	private final File file;

	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> undoDelay;

	private final ConfigData<Boolean> pasteAir;
	private final ConfigData<Boolean> removePaste;
	private final ConfigData<Boolean> pasteAtCaster;
	private final ConfigData<Boolean> pasteStructureVoid;

	private final Predicate<BlockType> preventOverwrite;

	private final List<Transformation> transformations = new ArrayList<>();

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
		pasteStructureVoid = getConfigDataBoolean("paste-structure-void", false);

		if (config.isBoolean(internalKey + "prevent-overwrite")) {
			preventOverwrite = getConfigBoolean("prevent-overwrite", false) ? _ -> true : null;
		} else preventOverwrite = getConfigRegistryEntryPredicate("prevent-overwrite", RegistryKey.BLOCK);

		List<?> transformList = getConfigList("transformations", new ArrayList<>());
		for (int i = 0; i < transformList.size(); i++) {
			if (!(transformList.get(i) instanceof Map<?, ?> map)) continue;
			ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

			String type = section.getString("type", "");
			switch (type.toLowerCase()) {
				case "translate" -> {
					ConfigData<Vector> translationData = ConfigDataUtil.getVector(section, "translation", null);

					transformations.add((data, transform) -> {
						Vector translation = translationData.get(data);
						if (translation == null) return transform;

						return transform.translate(translation.getX(), translation.getY(), translation.getZ());
					});
				}
				case "rotate" -> {
					ConfigData<String> axisData = ConfigDataUtil.getString(section, "axis", null);
					ConfigData<Double> angleData = ConfigDataUtil.getDouble(section, "angle", _ -> null);

					transformations.add((data, transform) -> {
						String axis = axisData.get(data);
						Double angle = angleData.get(data);
						if (axis == null || angle == null) return transform;

						return switch (axis.toLowerCase()) {
							case "x" -> transform.rotateX(angle);
							case "y" -> transform.rotateY(angle);
							case "z" -> transform.rotateZ(angle);
							default -> transform;
						};
					});
				}
				case "inverse" -> transformations.add((_, transform) -> transform.inverse());
				default -> MagicSpells.error("PasteSpell '" + internalName + "' has an invalid 'type' in 'transformations[" + i + "]': '" + type + "'");
			}
		}
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

		World world = target.getWorld();
		BlockVector3 pasteTo = BukkitAdapter.asBlockVector(target);

		Clipboard clipboard = this.clipboard;
		if (!transformations.isEmpty()) {
			AffineTransform transform = new AffineTransform();
			for (Transformation transformation : transformations) {
				transform = transformation.transform(data, transform);
			}

			try {
				clipboard = clipboard.transform(transform);
			} catch (WorldEditException e) {
				e.printStackTrace();
				return noTarget(data);
			}
		}

		boolean ignoreAir = !pasteAir.get(data);
		boolean ignoreStructureVoid = !pasteStructureVoid.get(data);

		if (preventOverwrite != null) {
			BlockVector3 offset = pasteTo.subtract(clipboard.getOrigin());

			for (BlockVector3 pos : clipboard.getRegion()) {
				BlockVector3 worldPos = pos.add(offset);
				Block origin = world.getBlockAt(worldPos.x(), worldPos.y(), worldPos.z());

				if (origin.isEmpty()) continue;
				if (!preventOverwrite.test(origin.getType().asBlockType())) continue;

				Material place = BukkitAdapter.adapt(clipboard.getFullBlock(pos).getBlockType());

				if (ignoreAir && place.isAir()) continue;
				if (ignoreStructureVoid && place == Material.STRUCTURE_VOID) continue;

				return noTarget(data);
			}
		}

		try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
			Operation operation = new ClipboardHolder(clipboard)
				.createPaste(editSession)
				.to(pasteTo)
				.ignoreAirBlocks(ignoreAir)
				.ignoreStructureVoidBlocks(ignoreStructureVoid)
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

	private interface Transformation {

		AffineTransform transform(SpellData data, AffineTransform transform);

	}

}
