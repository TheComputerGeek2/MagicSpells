package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.nisovin.magicspells.Subspell;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;

// TODO make this a targeted spell

// TODO make a migration aid which interprets old class names as the new versions
// TODO the migration aid should have a very verbose warning and warn any ops if it did anything on login

// TODO allow this to cast a spell at each location it puts a block
/*
 * The special position for effects is implemented here to run on each nova block placed.
 */
public class FirenovaSpell extends InstantSpell implements TargetedLocationSpell {

	int range;
	int tickSpeed;
	MagicMaterial mat;
	boolean burnTallGrass;
	private boolean checkPlugins;
	int expandRate = 1;
	
	HashSet<Player> fireImmunity;
	Subspell subSpell = null;
	
	// Using the names to be version safe
	private Set<String> activeDamageCauses = null;
	
	private static final Map<String, Set<String>> materialsToImmunities = new HashMap<>();
	static {
		materialsToImmunities.put("FIRE", Sets.newHashSet("FIRE", "FIRE_TICK"));
		materialsToImmunities.put("STATIONARY_LAVA", Sets.newHashSet("FIRE_TICK", "LAVA"));
		materialsToImmunities.put("LAVA", Sets.newHashSet("FIRE_TICK", "LAVA"));
		materialsToImmunities.put("MAGMA", Sets.newHashSet("HOT_FLOOR"));
		materialsToImmunities.put("CACTUS", Sets.newHashSet("CONTACT"));
	}
	
	public FirenovaSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 3);
		tickSpeed = getConfigInt("tick-speed", 5);
		burnTallGrass = getConfigBoolean("burn-tall-grass", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		mat = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("block-type", "FIRE"));
		
		expandRate = getConfigInt("expanding-radius-change", expandRate);
		if (expandRate < 1) expandRate = 1;
		
		if (materialsToImmunities.containsKey(mat.getMaterial().name())) {
			activeDamageCauses = materialsToImmunities.get(mat.getMaterial().name());
			fireImmunity = new HashSet<>();
		}
		
		subSpell = new Subspell(getConfigString("spell", ""));
		if (!subSpell.process()) subSpell = null;
		if (subSpell != null) {
			if (!subSpell.isTargetedLocationSpell()) subSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (this.fireImmunity != null) fireImmunity.add(player);
			new FirenovaAnimation(player);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		new FirenovaAnimation(target);
		return true;
	}

	@EventHandler
	public void doFireImmunity(EntityDamageEvent event) {
		if (event.isCancelled() || this.fireImmunity == null) return;
		if (!(event.getEntity() instanceof Player)) return;
		if (this.fireImmunity.isEmpty()) return;
		if (this.activeDamageCauses == null) return;
		if (!this.activeDamageCauses.contains(event.getCause().name())) return;
		Player player = (Player)event.getEntity();
		
		if (this.fireImmunity.contains(player)) {
			// Caster is taking damage, cancel it
			event.setCancelled(true);
			player.setFireTicks(0);
		} else if (this.checkPlugins) {
			// Check if nearby players are taking damage
			Location loc = player.getLocation();
			for (Player p : this.fireImmunity) {
				if (Math.abs(p.getLocation().getX() - loc.getX()) < this.range + 2 && Math.abs(p.getLocation().getZ() - loc.getZ()) < this.range + 2 && Math.abs(p.getLocation().getY() - loc.getY()) < this.range) {
					// Nearby, check plugins for pvp
					MagicSpellsEntityDamageByEntityEvent evt = new MagicSpellsEntityDamageByEntityEvent(p, player, DamageCause.ENTITY_ATTACK, event.getDamage());
					EventUtil.call(evt);
					if (evt.isCancelled()) {
						event.setCancelled(true);
						player.setFireTicks(0);
						break;
					}
				}
			}
		}
	}
	
	private class FirenovaAnimation implements Runnable {
		
		Player player;
		int i = 0;
		Block center;
		HashSet<Block> fireBlocks = new HashSet<>();
		int taskId;
		
		public FirenovaAnimation(Player caster) {
			this.player = caster;
			this.center = caster.getLocation().getBlock();
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickSpeed);
		}
		
		public FirenovaAnimation(Location location) {
			this.center = location.getBlock();
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickSpeed);
		}
		
		@Override
		public void run() {
			// Remove old fire blocks
			for (Block block : this.fireBlocks) {
				if (block.getType() != mat.getMaterial()) continue;
				block.setType(Material.AIR);
			}
			this.fireBlocks.clear();
			
			this.i += expandRate;
			if (i <= range) {
				// Set next ring on fire
				int bx = this.center.getX();
				int y = this.center.getY();
				int bz = this.center.getZ();
				for (int x = bx - this.i; x <= bx + this.i; x++) {
					for (int z = bz - this.i; z <= bz + this.i; z++) {
						if (!(Math.abs(x - bx) == this.i || Math.abs(z - bz) == this.i)) continue;
						Block b = this.center.getWorld().getBlockAt(x, y, z);
						if (b.getType() == Material.AIR || (burnTallGrass && b.getType() == Material.TALL_GRASS)) {
							Block under = b.getRelative(BlockFace.DOWN);
							if (under.getType() == Material.AIR || (burnTallGrass && under.getType() == Material.TALL_GRASS)) {
								b = under;
							}
							mat.setBlock(b, false);
							playSpellEffects(EffectPosition.SPECIAL, b.getLocation());
							if (subSpell != null) subSpell.castAtLocation(this.player, b.getLocation(), 1);
							this.fireBlocks.add(b);
						} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || (burnTallGrass && b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS)) {
							b = b.getRelative(BlockFace.UP);
							mat.setBlock(b, false);
							playSpellEffects(EffectPosition.SPECIAL, b.getLocation());
							if (subSpell != null) subSpell.castAtLocation(this.player, b.getLocation(), 1);
							this.fireBlocks.add(b);
						}
					}
				}
			} else if (this.i > range + 1) {
				// Stop if done
				Bukkit.getServer().getScheduler().cancelTask(this.taskId);
				if (fireImmunity != null && this.player != null) {
					fireImmunity.remove(this.player);
				}
			}
		}
		
	}

}