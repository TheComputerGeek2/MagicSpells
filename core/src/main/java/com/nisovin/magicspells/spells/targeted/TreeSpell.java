package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.block.data.BlockData;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class TreeSpell extends TargetedSpell implements TargetedLocationSpell {

	private TreeType treeType;

	private int speed;
	
	public TreeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		try {
			treeType = TreeType.valueOf(getConfigString("tree-type", "tree").toUpperCase().replace(" ", "_"));
		}
		catch (IllegalArgumentException ignored) {
			treeType = TreeType.TREE;
		}

		speed = getConfigInt("animation-speed", 20);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = getTargetedBlock(caster, power);

			if (target != null && !BlockUtils.isAir(target.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) target = null;
				else target = event.getTargetLocation().getBlock();
			}
			
			if (target == null || BlockUtils.isAir(target.getType())) return noTarget(caster);
			
			boolean grown = growTree(caster, target, power, args);
			if (!grown) return noTarget(caster);

			playSpellEffects(caster, target.getLocation());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean growTree(LivingEntity caster, Block target, float power, String[] args) {
		target = target.getRelative(BlockFace.UP);
		if (!BlockUtils.isAir(target.getType())) return false;
		
		Location loc = target.getLocation();
		if (speed > 0) {
			List<BlockState> blockStates = new ArrayList<>();
			target.getWorld().generateTree(loc, treeType, new TreeWatch(loc, blockStates));
			if (!blockStates.isEmpty()) {
				new GrowAnimation(loc.getBlockX(), loc.getBlockZ(), blockStates, speed);
				return true;
			}
			return false;
		}
		return target.getWorld().generateTree(loc, treeType);
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		boolean ret = growTree(caster, target.getBlock(), power, args);
		if (ret) playSpellEffects(caster, target);
		return ret;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return growTree(null, target.getBlock(), power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(target, power, null);
	}

	private static class GrowAnimation extends SpellAnimation {
		
		private List<BlockState> blockStates;

		private int blocksPerTick;

		private GrowAnimation(final int centerX, final int centerZ, final List<BlockState> blocks, int speed) {
			super(speed < 20 ? 20 / speed : 1, true);
			
			this.blockStates = blocks;
			this.blocksPerTick = speed/20 + 1;
			blockStates.sort((o1, o2) -> {
				if (o1.getY() < o2.getY()) return -1;
				if (o1.getY() > o2.getY()) return 1;

				int dist1 = Math.abs(o1.getX() - centerX) + Math.abs(o1.getZ() - centerZ);
				int dist2 = Math.abs(o2.getX() - centerX) + Math.abs(o2.getZ() - centerZ);
				return Integer.compare(dist1, dist2);
			});
		}

		@Override
		protected void onTick(int tick) {
			for (int i = 0; i < blocksPerTick; i++) {
				BlockState state = blockStates.remove(0);
				state.update(true);
				if (blockStates.isEmpty()) {
					stop(true);
					break;
				}
			}
		}
		
	}
	
	private static class TreeWatch implements BlockChangeDelegate {

		private Location loc;
		private List<BlockState> blockStates;

		private TreeWatch(Location loc, List<BlockState> blockStates) {
			this.loc = loc;
			this.blockStates = blockStates;
		}
		
		@Override
		public int getHeight() {
			return loc.getWorld().getMaxHeight();
		}

		@Override
		public boolean isEmpty(int x, int y, int z) {
			return BlockUtils.isAir(loc.getWorld().getBlockAt(x, y, z).getType());
		}

		@Override
		public boolean setBlockData(int x, int y, int z, @NotNull BlockData data) {
			BlockState state = loc.getWorld().getBlockAt(x, y, z).getState();
			state.setBlockData(data);
			blockStates.add(state);
			return true;
		}
		
		@Override
		@NotNull
		public BlockData getBlockData(int x, int y, int z) {
			return loc.getWorld().getBlockAt(x, y, z).getBlockData();
		}
		
	}

}
