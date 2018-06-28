package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.materials.MagicItemMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class MaterializeSpell extends TargetedSpell implements TargetedLocationSpell {

	/*These extra features were inspired by Shadoward12's Rune/Pattern-Tester spell
	Thank You! Shadoward12!*/

	//Normal Features
	MagicMaterial material;
	Set<Material> materials;
	private int resetDelay;
	private boolean falling;
	private boolean applyPhysics;
	private boolean checkPlugins;
	boolean playBreakEffect;
	private String strFailed;
	
	//Pattern Configuration
	private boolean usePattern;
	private List<String> patterns;
	private MagicMaterial[][] rowPatterns;
	private boolean restartPatternEachRow;
	private boolean randomizePattern;
	private boolean stretchPattern;

	//Cuboid Parameters
	private String area;
	private int height;
	private double fallheight;

	//Cuboid Variables;
	private int rowSize;
	private int columnSize;

	//Cuboid Checks;
	private boolean hasMiddle;

	//Randomization
	private Random rand = new Random();

	public MaterializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String blockType = getConfigString("block-type", "stone");
		material = MagicSpells.getItemNameResolver().resolveBlock(blockType);
		if (material == null) MagicSpells.error("Invalid block-type on materialize spell '" + internalName + '\'');
		resetDelay = getConfigInt("reset-delay", 0);
		falling = getConfigBoolean("falling", false);
		applyPhysics = getConfigBoolean("apply-physics", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("play-break-effect", true);
		strFailed = getConfigString("str-failed", "");

		usePattern = getConfigBoolean("use-pattern", false);
		patterns = getConfigStringList("patterns", null);
		restartPatternEachRow = getConfigBoolean("restart-pattern-each-row", false);
		randomizePattern = getConfigBoolean("randomize-pattern", false);
		stretchPattern = getConfigBoolean("stretch-pattern", false);

		area = getConfigString("area", "1x1");
		height = getConfigInt("height", 1);
		fallheight = getConfigDouble("fall-height", 0.5);
	}

	public void initialize() {
		super.initialize();

		//First, lets split the "area" that was given.
		String[] areaparts = area.split("x", 2);

		//Lets define the size of the row and column to form an shape array;
		rowSize = Integer.parseInt(areaparts[0]);
		columnSize = Integer.parseInt(areaparts[1]);

		/*For this to work smoothly, we need to see if the shape array has a middle;
		It becomes very complicated when working with shape arrays without a block as a geometrical middle
		So unfortunately. Shape arrays without a block as its geomtrical center cannot be accepted.
		3x2, 9x8. Basically, if the product of the length and width is even. Don't use it. */
		hasMiddle = ((rowSize * columnSize) % 2 ) == 1;

		if (!hasMiddle && patterns != null) {
			MagicSpells.error("MaterializeSpell " + internalName + " is using a shape array without a geometrical center! A single block will spawn instead.");
		}

		//If height is 0, the code ceases to function. Lets not have that.
		if (height == 0) height = 1;

		//After the reset-delay passes, we need to remove all the blocks that were materialized.
		//We store them within "materials" and "rowPatterns" aswell
		boolean ready;

		materials = new HashSet<>();

		if (patterns != null) ready = parseBlocks(patterns);
		else ready = false;

		//If the parser failed, we'll have to force a string inside;
		if (!ready) {
			rowPatterns = new MagicMaterial[1][1];
			rowPatterns[0][0] = material;
			materials.add(material.getMaterial());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> lastTwo;
			try {
				lastTwo = getLastTwoTargetedBlocks(player, power);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				lastTwo = null;
			}
			if (lastTwo != null && lastTwo.size() == 2 && lastTwo.get(1).getType() != Material.AIR && lastTwo.get(0).getType() == Material.AIR) {
				Block block = lastTwo.get(0);
				Block against = lastTwo.get(1);
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(player, strFailed);
				block = event.getTargetLocation().getBlock();

				boolean done = doMaterialize(player, against);
				if (!done) return noTarget(player, strFailed);

			} else {
				// Fail no target
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean doMaterialize(Player player, Block target) {
		//The target block is ALWAYS the block on top of the ground block
		if (hasMiddle && patterns != null && usePattern) {
			//Unfortunately, shape array placement is world relative, will fix later.
			//This is the top-left edge of the shape array
			Location patternStart = target.getLocation();

			patternStart.setX(patternStart.getX() - Math.ceil(rowSize / 2));
			patternStart.setZ(patternStart.getZ() - Math.ceil(columnSize / 2));

			Block air;
			Block ground;

			/*The row position dictates which block within a row pattern will be used
			when placing the new block.*/
			int rowPosition = 0;

			//Lets start at the bottom floor then work our way up; or down if height is less than 0.
			for (int y = 0; y < height; y++) {
				/*The pattern position is the pattern being read for a specific row
				This should always reset when it goes over into a new height.*/
				int patternPosition = 0;

				//The block placement loop will start finish a row of coloumns then move down a row..
				for (int z = 0; z < columnSize; z++) {
					//Everytime a shape row is finished, we need to start at the topleft and move down 1 row.
					Location spawnBlock = patternStart.clone().add(0, y, z);

					//Lets parse the list of patterns for that row.
					if (patternPosition >= patterns.size()) patternPosition = 0;

					int rowLength =	getRowLength(patternPosition);

					//If they want the pattern to restart on each row, reset rowpositon to 0.
					if (restartPatternEachRow) rowPosition = 0;

					//Lets spawn a block on each column before we move down a row.
					for (int x = 0; x < rowSize; x++) {
						ground = spawnBlock.getBlock();
						air = ground.getRelative(BlockFace.UP);

						/*Now if we are looking for a block outside of the rowlist range.
						We need to go back to the start and repeat that row pattern*/
						if (rowPosition >= rowLength) rowPosition = 0;

						//Doesn't really become a pattern if you randomize it but ok!
						if (!stretchPattern || y < 1) material = blockGenerator(randomizePattern, patternPosition, rowPosition);
						else material = MagicMaterial.fromBlock(ground);

						//Add one to the row position so that it will move to the next block.
						rowPosition++;

						//As soon as a block can't be spawned, it will return an error.
						boolean done = materialize(player, air, ground);
						if (!done) return false;

						//Done with placing that one block? Move on to the next one.
						spawnBlock.setX((ground.getX() + 1));
					}
					//If multiple patterns were requested, lets move to the next line.
					patternPosition++;
				}
			}
		} else {
			boolean done = materialize(player, target.getRelative(BlockFace.UP), target);
			if (!done) return false;
		}
		return true;
	}

	private int getRowLength(int patternPosition) {

		return rowPatterns[patternPosition].length;
	}

	private boolean parseBlocks(List<String> patternList) {

		if (patternList == null) return false;

		int patternSize = patternList.size();
		int iteration = 0;

		rowPatterns = new MagicMaterial[patternSize][];

		//Lets parse all the rows within patternList
		for (String list : patternList) {
			String[] split = list.split(",");
			int arraySize = split.length;
			int blockPosition = 0;

			rowPatterns[iteration] = new MagicMaterial[arraySize];

			for (String block : split) {
				MagicMaterial mat = MagicSpells.getItemNameResolver().resolveBlock(block);
				if (mat == null) mat = MagicSpells.getItemNameResolver().resolveBlock("stone");

				if (mat.getMaterial() != null) {
					materials.add(mat.getMaterial());
					rowPatterns[iteration][blockPosition] = mat;
				}
				blockPosition++;
			}

			iteration++;
		}
		return true;
	}

	private MagicMaterial blockGenerator(boolean randomize, int patternPosition, int rowPosition) {

		MagicMaterial mat;

		int randomIndex = rand.nextInt(getRowLength(patternPosition));

		if (!randomize) mat = rowPatterns[patternPosition][rowPosition];
		else mat = rowPatterns[patternPosition][randomIndex];

		return mat;
	}

	private boolean materialize(Player player, Block block, Block against) {
		BlockState blockState = block.getState();

		if (checkPlugins && player != null) {
			material.setBlock(block, false);
			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, blockState, against, HandHandler.getItemInMainHand(player), player, true);
			EventUtil.call(event);
			blockState.update(true);
			if (event.isCancelled()) return false;
		}
		if (!falling) {
			material.setBlock(block, applyPhysics);
		} else {
			material.spawnFallingBlock(block.getLocation().add(0.5, fallheight, 0.5));
		}

		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
			playSpellEffects(EffectPosition.TARGET, block.getLocation());
			playSpellEffectsTrail(player.getLocation(), block.getLocation());
		}

		if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

		if (resetDelay > 0 && !falling) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				
				@Override
				public void run() {
					if (materials.contains(block.getType())) {
						block.setType(Material.AIR);
						playSpellEffects(EffectPosition.DELAYED, block.getLocation());
						playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
						if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
					}
				}
				
			}, resetDelay);
		}
		return true;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		Block against = target.clone().add(target.getDirection()).getBlock();
		if (block.equals(against)) against = block.getRelative(BlockFace.DOWN);
		if (block.getType() == Material.AIR) return doMaterialize(caster, against);
		Block block2 = block.getRelative(BlockFace.UP);
		if (block2.getType() == Material.AIR) return doMaterialize(null, block);
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Block block = target.getBlock();
		if (block.getType() == Material.AIR) return doMaterialize(null, block);
		Block block2 = block.getRelative(BlockFace.UP);
		if (block2.getType() == Material.AIR) return doMaterialize(null, block2);
		return false;
	}

}
