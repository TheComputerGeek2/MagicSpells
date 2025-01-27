package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.RayTraceResult;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;

public class MaterializeSpell extends TargetedSpell implements TargetedLocationSpell {

	/*These extra features were inspired by Shadoward12's Rune/Pattern-Tester spell,
	Thank You! Shadoward12!*/

	private List<Block> blocks;
	private boolean removeBlocks;

	//Normal Features
	private Set<Material> materials;
	private Material material;
	private int resetDelay;
	private boolean falling;
	private boolean applyPhysics;
	private boolean checkPlugins;
	boolean playBreakEffect;
	private String strFailed;

	//Pattern Configuration
	private boolean usePattern;
	private List<String> patterns;
	private Material[][] rowPatterns;
	private boolean restartPatternEachRow;
	private boolean randomizePattern;
	private boolean stretchPattern;

	//Cuboid Parameters
	private String area;
	private ConfigData<Integer> height;
	private ConfigData<Double> fallHeight;

	//Cuboid Variables;
	private int rowSize;
	private int columnSize;

	//Cuboid Checks;
	private boolean hasMiddle;

	public MaterializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String blockType = getConfigString("block-type", "stone");
		material = Util.getMaterial(blockType);
		if (material == null || !material.isBlock()) MagicSpells.error("MaterializeSpell '" + internalName + "' has an invalid block-type defined!");

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
		height = getConfigDataInt("height", 1);
		fallHeight = getConfigDataDouble("fall-height", 0.5);

		removeBlocks = getConfigBoolean("remove-blocks", true);
		blocks = new ArrayList<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		//First, lets split the "area" that was given.
		String[] areaParts = area.split("x", 2);

		//Let's define the size of the row and column to form a shape array;
		rowSize = Integer.parseInt(areaParts[0]);
		columnSize = Integer.parseInt(areaParts[1]);

		/*For this to work smoothly, we need to see if the shape array has a middle;
		It becomes very complicated when working with shape arrays without a block as a geometrical middle
		So unfortunately. Shape arrays without a block as its geometrical center cannot be accepted.
		3x2, 9x8. Basically, if the product of the length and width is even. Don't use it. */
		hasMiddle = ((rowSize * columnSize) % 2) == 1;

		if (!hasMiddle && patterns != null) {
			MagicSpells.error("MaterializeSpell " + internalName + " is using a shape array without a geometrical center! A single block will spawn instead.");
		}

		//After the reset-delay passes, we need to remove all the blocks that were materialized.
		//We store them within "materials" and "rowPatterns" as well
		boolean ready;

		materials = new HashSet<>();

		if (patterns != null) ready = parseBlocks(patterns);
		else ready = false;

		//If the parser failed, we'll have to force a string inside;
		if (!ready) {
			rowPatterns = new Material[1][1];
			rowPatterns[0][0] = material;
			materials.add(material);
		}
	}

	@Override
	public void turnOff() {
		for (Block b : blocks) {
			b.setType(Material.AIR);
		}

		blocks.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(data);

		Block against = result.getHitBlock();
		Block block = against.getRelative(result.getHitBlockFace());

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, block.getLocation());
		if (!event.callEvent()) return noTarget(strFailed, event);

		data = event.getSpellData();
		block = event.getTargetLocation().getBlock();

		if (!hasMiddle) {
			boolean done = materialize(caster, block, against, data);
			if (!done) return noTarget(strFailed, data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		//Unfortunately, shape array placement is world relative, will fix later.
		//This is the top-left edge of the shape array
		Location patternStart = against.getLocation();

		patternStart.setX(against.getX() - Math.ceil(rowSize / 2F));
		patternStart.setZ(against.getZ() - Math.ceil(columnSize / 2F));

		//spawnBlock is the current position in the loop where it will spawn the block
		Location spawnBlock = patternStart;

		Block air;
		Block ground;

			/*The row position dictates which block within a row pattern will be used
			when placing the new block.*/
		int rowPosition = 0;

		int height = this.height.get(data);

		//If height is 0, the code ceases to function. Let's not have that.
		if (height == 0) height = 1;

		//Let's start at the bottom floor then work our way up; or down if height is less than 0.
		for (int y = 0; y < height; y++) {
				/*The pattern position is the pattern being read for a specific row
				This should always reset when it goes over into a new height.*/
			int patternPosition = 0;

			//The block placement loop will start finish a row of coloumns then move down a row.
			for (int z = 0; z < columnSize; z++) {
				//Everytime a shape row is finished, we need to start at the topleft and move down 1 row.
				spawnBlock = patternStart.clone().add(0, y, z);

				//Let's parse the list of patterns for that row.
				if (patterns != null && patternPosition >= patterns.size()) patternPosition = 0;

				int rowLength = getRowLength(patternPosition);

				//If they want the pattern to restart on each row, reset rowpositon to 0.
				if (restartPatternEachRow) rowPosition = 0;

				//Let's spawn a block on each column before we move down a row.
				for (int x = 0; x < rowSize; x++) {
					ground = spawnBlock.getBlock();
					air = ground.getRelative(BlockFace.UP);

					//Now if we are looking for a block outside the rowlist range.
					//We need to go back to the start and repeat that row pattern
					if (rowPosition >= rowLength) rowPosition = 0;

					//Doesn't really become a pattern if you randomize it but ok!
					if (!stretchPattern || y < 1)
						material = blockGenerator(randomizePattern, patternPosition, rowPosition);
					else material = ground.getType();

					//Add one to the row position so that it will move to the next block.
					rowPosition++;

					//As soon as a block can't be spawned, it will return an error.
					boolean done = materialize(caster, air, ground, data.location(block.getLocation()));
					if (!done) return noTarget(strFailed, data);

					//Done with placing that one block? Move on to the next one.
					spawnBlock.setX((ground.getX() + 1));
				}
				//If multiple patterns were requested, lets move to the next line.
				patternPosition++;
			}
		}

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) {
			Block block = data.location().getBlock();
			if (block.getType().isAir()) {
				boolean done = materialize(null, block, block, data);
				return done ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(strFailed, data);
			}

			Block block2 = block.getRelative(BlockFace.UP);
			if (block2.getType().isAir()) {
				data = data.location(block2.getLocation());

				boolean done = materialize(null, block2, block, data);
				return done ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(strFailed, data);
			}

			return noTarget(strFailed, data);
		}

		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location target = data.location();

		Block block = target.getBlock();
		Block against = target.add(target.getDirection()).getBlock();
		if (block.equals(against)) against = block.getRelative(BlockFace.DOWN);

		if (block.getType().isAir()) {
			boolean done = materialize(caster, block, against, data);
			return done ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(strFailed, data);
		}

		Block block2 = block.getRelative(BlockFace.UP);
		if (block2.getType().isAir()) {
			data = data.location(block2.getLocation());

			boolean done = materialize(caster, block2, block, data);
			return done ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(strFailed, data);
		}

		return noTarget(strFailed, data);
	}

	private int getRowLength(int patternPosition) {
		return rowPatterns[patternPosition].length;
	}

	private boolean parseBlocks(List<String> patternList) {
		if (patternList == null) return false;

		int patternSize = patternList.size();
		int iteration = 0;

		rowPatterns = new Material[patternSize][];

		//Let's parse all the rows within patternList
		for (String list : patternList) {
			String[] split = list.split(",");
			int arraySize = split.length;
			int blockPosition = 0;

			rowPatterns[iteration] = new Material[arraySize];

			for (String block : split) {
				Material mat = Util.getMaterial(block);
				if (mat == null) mat = Material.STONE;

				materials.add(mat);
				rowPatterns[iteration][blockPosition] = mat;
				blockPosition++;
			}

			iteration++;
		}
		return true;
	}

	private Material blockGenerator(boolean randomize, int patternPosition, int rowPosition) {
		Material mat;

		int randomIndex = random.nextInt(getRowLength(patternPosition));

		if (!randomize) mat = rowPatterns[patternPosition][rowPosition];
		else mat = rowPatterns[patternPosition][randomIndex];

		return mat;
	}

	private boolean materialize(Player player, Block block, Block against, SpellData data) {
		BlockState blockState = block.getState();

		if (checkPlugins && player != null) {
			block.setType(material, false);
			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, blockState, against, player.getEquipment().getItemInMainHand(), player, true);
			EventUtil.call(event);
			blockState.update(true);
			if (event.isCancelled()) return false;
		}
		if (falling) {
			Location location = block.getLocation().add(0.5, fallHeight.get(data), 0.5);
			block.getWorld().spawn(location, FallingBlock.class, fb -> fb.setBlockData(material.createBlockData()));
		}
		else block.setType(material, applyPhysics);

		playSpellEffects(EffectPosition.TARGET, block.getLocation(), data);
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player, data);
			playSpellEffectsTrail(player.getLocation(), block.getLocation(), data);
		}

		if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getBlockData());
		if (removeBlocks) blocks.add(block);

		if (resetDelay > 0 && !falling) {
			MagicSpells.scheduleDelayedTask(() -> {
				if (materials.contains(block.getType())) {
					blocks.remove(block);
					playSpellEffects(EffectPosition.DELAYED, block.getLocation(), data);
					if (checkPlugins && player != null) {
						MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(block, player);
						EventUtil.call(event);
						if (event.isCancelled()) return;
					}
					block.setType(Material.AIR);
					playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation(), data);
					if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getBlockData());
				}
			}, resetDelay, block.getLocation());
		}
		return true;
	}

}
