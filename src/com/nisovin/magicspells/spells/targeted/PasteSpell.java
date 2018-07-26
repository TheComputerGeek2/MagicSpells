package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;

public class PasteSpell extends TargetedSpell implements TargetedLocationSpell {

	File file;
	int yOffset;
	int maxBlocks;
	boolean pasteAir;
	boolean pasteEntities;
	boolean pasteAtCaster;
	boolean playBlockBreakEffect;
	
	int tickInterval;
	int blocksPerTick;
	
	// This will only work with instant paste for now
	boolean undo;
	int undoDelayTicks;
	
	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);
		
		yOffset = getConfigInt("y-offset", 0);
		maxBlocks = getConfigInt("max-blocks", 10000);
		pasteAir = getConfigBoolean("paste-air", false);
		pasteEntities = getConfigBoolean("paste-entities", true);
		pasteAtCaster = getConfigBoolean("paste-at-caster", false);
		
		undo = getConfigBoolean("undo", false);
		undoDelayTicks = getConfigInt("undo-delay-ticks", TimeUtil.TICKS_PER_SECOND * 10);
		if (undoDelayTicks < 0) undoDelayTicks = 0;
		
		playBlockBreakEffect = getConfigBoolean("play-block-break-effect", true);
		
		float blocksPerSecond = getConfigFloat("blocks-per-second", 0F);
		if (blocksPerSecond == 0) {
			tickInterval = 0;
			blocksPerTick = 0;
		} else if (blocksPerSecond > 20) {
			tickInterval = 1;
			blocksPerTick = (int)Math.ceil(blocksPerSecond / 20);
		} else {
			tickInterval = Math.round(20 / blocksPerSecond);
			blocksPerTick = 1;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pasteAtCaster ? player.getLocation().getBlock() : getTargetedBlock(player, power);
			if (target == null) return noTarget(player);
			Location loc = target.getLocation();
			loc.add(0, yOffset, 0);
			boolean ok = castAtLocation(loc, power);
			if (!ok) return noTarget(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean ok;
		if (tickInterval == 0) {
			ok = pasteInstant(target);
		} else {
			ok = pasteOverTime(target);
		}
		if (ok) {
			if (caster != null) {
				playSpellEffects(caster, target);
			} else {
				playSpellEffects(EffectPosition.TARGET, target);
			}
		}
		return ok;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}
	
	private boolean pasteInstant(Location target) {
		try {
			CuboidClipboard cuboid = SchematicFormat.MCEDIT.load(file);
			final EditSession session = new EditSession(new BukkitWorld(target.getWorld()), maxBlocks);
			cuboid.paste(session, new Vector(target.getX(), target.getY(), target.getZ()), !pasteAir, pasteEntities);
			if (undo) {
				MagicSpells.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> session.undo(session), undoDelayTicks);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean pasteOverTime(Location target) {
		try {
			Builder builder = new Builder(target);
			builder.build();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	class Builder {

		Block center;
		List<BlockState> blocks = new ArrayList<>();
		
		int current = 0;
		int taskId;
		
		public Builder(Location target) throws Exception {
			this.center = target.getBlock();
			
			CuboidClipboard clipboard = SchematicFormat.MCEDIT.load(file);
			Vector size = clipboard.getSize();
			Vector offset = clipboard.getOffset();

			List<BlockState> air = new ArrayList<>();
			List<BlockState> solids = new ArrayList<>();
			List<BlockState> nonsolids = new ArrayList<>();
			
			for (int y = 0; y < size.getBlockY(); y++) {
				for (int x = 0; x < size.getBlockX(); x++) {
					for (int z = 0; z < size.getBlockZ(); z++) {
						BaseBlock b = clipboard.getBlock(new Vector(x, y, z));
						int blockX = target.getBlockX() + x + offset.getBlockX();
						int blockY = target.getBlockY() + y + offset.getBlockY();
						int blockZ = target.getBlockZ() + z + offset.getBlockZ();
						Block block = target.getWorld().getBlockAt(blockX, blockY, blockZ);
						if (b.getId() != 0 || (pasteAir && block.getType() != Material.AIR)) {
							BlockState state = block.getState();
							setBlockStateFromWorldEditBlock(state, b);
							if (state.getType() == Material.AIR) {
								air.add(state);
							} else if (state.getType().isSolid()) {
								solids.add(state);
							} else {
								nonsolids.add(state);
							}
						}
					}
				}
			}
			
			blocks.addAll(air);
			blocks.addAll(solids);
			blocks.addAll(nonsolids);
		}
		
		public void build() {
			taskId = MagicSpells.scheduleRepeatingTask(new Runnable() {
				@Override
				public void run() {
					if (current >= blocks.size()) {
						MagicSpells.cancelTask(taskId);
					} else {
						for (int i = 0; i < blocksPerTick; i++) {
							BlockState state = blocks.get(current);
							state.update(true, false);
							if (playBlockBreakEffect && state.getType() != Material.AIR) {
								center.getWorld().playEffect(state.getLocation(), Effect.STEP_SOUND, state.getType());
							}
							current++;
							if (current >= blocks.size()) {
								MagicSpells.cancelTask(taskId);
								break;
							}
						}
					}
				}
			}, 1, tickInterval);
		}
		
		private void setBlockStateFromWorldEditBlock(BlockState state, BaseBlock block) {
			state.setType(Material.getMaterial(block.getId() + "", true));
			state.setRawData((byte)block.getData());
		}
		
	}

}
