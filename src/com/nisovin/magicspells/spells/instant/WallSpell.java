package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TemporaryBlockSet;

// TODO see about adding options to define more complex wall shapes/patterns
// TODO add a spell on block broken option
public class WallSpell extends InstantSpell {

	private int distance;
	private int wallWidth;
	private int wallHeight;
	private int wallDepth;
	private int yOffset;
	private MagicMaterial wallMaterial;
	private int wallDuration;
	boolean preventBreaking;
	private boolean alwaysOnGround;
	private boolean preventDrops;
	private boolean checkPlugins;
	private boolean checkPluginsPerBlock;
	private String strNoTarget;
	
	ArrayList<TemporaryBlockSet> blockSets;
	
	public WallSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.distance = getConfigInt("distance", 3);
		this.wallWidth = getConfigInt("wall-width", 5);
		this.wallHeight = getConfigInt("wall-height", 3);
		this.wallDepth = getConfigInt("wall-depth", 1);
		this.yOffset = getConfigInt("y-offset", -1);
		String type = getConfigString("wall-type", "stone");
		this.wallMaterial = MagicSpells.getItemNameResolver().resolveBlock(type);
		this.wallDuration = getConfigInt("wall-duration", 15);
		this.alwaysOnGround = getConfigBoolean("always-on-ground", false);
		this.preventBreaking = getConfigBoolean("prevent-breaking", false);
		this.preventDrops = getConfigBoolean("prevent-drops", true);
		this.checkPlugins = getConfigBoolean("check-plugins", true);
		this.checkPluginsPerBlock = getConfigBoolean("check-plugins-per-block", this.checkPlugins);
		this.strNoTarget = getConfigString("str-no-target", "Unable to create a wall.");
		
		this.blockSets = new ArrayList<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if ((this.preventBreaking || this.preventDrops) && this.wallDuration > 0) registerEvents(new BreakListener());
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = getTargetedBlock(player, this.distance > 0 && this.distance < 15 ? this.distance : 3);
			if (target == null || target.getType() != Material.AIR) {
				// Fail
				sendMessage(this.strNoTarget, player, args);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				
				// Check plugins
				if (this.checkPlugins) {
					BlockState eventBlockState = target.getState();
					this.wallMaterial.setBlock(target, false);
					MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(target, eventBlockState, target, HandHandler.getItemInMainHand(player), player, true);
					EventUtil.call(event);
					BlockUtils.setTypeAndData(target, Material.AIR, false);
					if (event.isCancelled()) {
						sendMessage(this.strNoTarget, player, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
				
				if (this.alwaysOnGround) {
					this.yOffset = 0;
					Block b = target.getRelative(0, -1, 0);
					while (b.getType() == Material.AIR && this.yOffset > -5) {
						this.yOffset--;
						b = b.getRelative(0, -1, 0);
					}
				}
				
				TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, this.wallMaterial, this.checkPluginsPerBlock, player);
				Location loc = target.getLocation();
				Vector dir = player.getLocation().getDirection();
				int wallWidth = Math.round(this.wallWidth * power);
				int wallHeight = Math.round(this.wallHeight * power);
				if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
					int depthDir = dir.getX() > 0 ? 1 : -1;
					for (int z = loc.getBlockZ() - (wallWidth/2); z <= loc.getBlockZ() + (wallWidth/2); z++) {
						for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
							for (int x = target.getX(); x < target.getX() + wallDepth && x > target.getX() - wallDepth; x += depthDir) {
								blockSet.add(player.getWorld().getBlockAt(x, y, z));
							}
						}
					}
				} else {
					int depthDir = dir.getZ() > 0 ? 1 : -1;
					for (int x = loc.getBlockX() - (wallWidth/2); x <= loc.getBlockX() + (wallWidth/2); x++) {
						for (int y = loc.getBlockY() + yOffset; y < loc.getBlockY() + wallHeight + yOffset; y++) {
							for (int z = target.getZ(); z < target.getZ() + wallDepth && z > target.getZ() - wallDepth; z += depthDir) {
								blockSet.add(player.getWorld().getBlockAt(x, y, z));
							}
						}
					}
				}
				if (wallDuration > 0) {
					blockSets.add(blockSet);
					blockSet.removeAfter(Math.round(wallDuration * power), (TemporaryBlockSet set) -> blockSets.remove(set));
				}
				
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class BreakListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		void onBlockBreak(BlockBreakEvent event) {
			if (blockSets.isEmpty()) return;
			Block block = event.getBlock();
			for (TemporaryBlockSet blockSet : blockSets) {
				if (!blockSet.contains(block)) continue;
				event.setCancelled(true);
				if (!preventBreaking) block.setType(Material.AIR);
			}
		}
		
	}
	
	@Override
	public void turnOff() {
		this.blockSets.stream().forEachOrdered(TemporaryBlockSet::remove);
		this.blockSets.clear();
	}
	
}
