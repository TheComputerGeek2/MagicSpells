package com.nisovin.magicspells.spelleffects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.materials.MagicUnknownMaterial;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.util.Util;

/**
 * NovaEffect<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>type</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>radius</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>expand-interval</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 * </table>
 */
public class NovaEffect extends SpellEffect {
	
	MagicMaterial mat;
	
	int radius = 3;
	
	int novaTickInterval = 5;
	int expandingRadiusChange = 1;

	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			String[] params = string.split(" ");
			int type = 51;
			byte data = 0;
			if (params.length >= 1) {
				try {
					type = Integer.parseInt(params[0]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			if (params.length >= 2) {
				try {
					data = Byte.parseByte(params[1]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			mat = new MagicUnknownMaterial(type, data);
			if (params.length >= 3) {
				try {
					radius = Integer.parseInt(params[2]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			if (params.length >= 4) {
				try {
					novaTickInterval = Integer.parseInt(params[3]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		mat = MagicSpells.getItemNameResolver().resolveBlock(config.getString("type", "fire"));
		radius = config.getInt("radius", radius);
		novaTickInterval = config.getInt("expand-interval", novaTickInterval);
		expandingRadiusChange = config.getInt("expanding-radius-change", expandingRadiusChange);
		if (expandingRadiusChange < 1) expandingRadiusChange = 1;
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		if (mat == null) return null;
		
		// get nearby players
		Item item = location.getWorld().dropItem(location, new ItemStack(Material.STONE, 0));
		List<Entity> nearbyEntities = item.getNearbyEntities(20, 20, 20);
		item.remove();
		List<Player> nearby = new ArrayList<Player>();
		for (Entity e : nearbyEntities) {
			if (e instanceof Player) {
				nearby.add((Player)e);
			}
		}
		
		// start animation
		Block b = location.getBlock();
		if (!BlockUtils.isPathable(b)) {
			b = b.getRelative(BlockFace.UP);
		}
		new NovaAnimation(nearby, location.getBlock(), mat, radius, novaTickInterval, expandingRadiusChange);
		return null;
	}
	

	private class NovaAnimation extends SpellAnimation {
		List<Player> nearby;
		Block center;
		MagicMaterial matNova;
		int radiusNova;
		Set<Block> blocks;
		int radiusChange;
		
		public NovaAnimation(List<Player> nearby, Block center, MagicMaterial mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			blocks = new HashSet<Block>();
			radiusChange = activeRadiusChange;
		}

		@Override
		protected void onTick(int tick) {
			// remove old fire blocks
			tick*= radiusChange;
			for (Block block : blocks) {
				for (Player p : nearby) {
					Util.restoreFakeBlockChange(p, block);
				}
			}
			blocks.clear();
			
			if (tick <= radiusNova) {
				// set next ring on fire
				int bx = center.getX();
				int y = center.getY();
				int bz = center.getZ();
				for (int x = bx - tick; x <= bx + tick; x++) {
					for (int z = bz - tick; z <= bz + tick; z++) {
						if (Math.abs(x-bx) == tick || Math.abs(z-bz) == tick) {
							Block b = center.getWorld().getBlockAt(x,y,z);
							if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
								Block under = b.getRelative(BlockFace.DOWN);
								if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) {
									b = under;
								}
								for (Player p : nearby) {
									Util.sendFakeBlockChange(p, b, matNova);
								}
								blocks.add(b);
							} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
								b = b.getRelative(BlockFace.UP);
								for (Player p : nearby) {
									Util.sendFakeBlockChange(p, b, matNova);
								}
								blocks.add(b);
							}
						}
					}
				}
			} else if (tick > radiusNova+1) {
				// stop if done
				stop();
			}
		}
	}

}
