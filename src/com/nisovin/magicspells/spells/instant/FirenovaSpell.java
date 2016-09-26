package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;

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
import com.nisovin.magicspells.util.MagicConfig;

public class FirenovaSpell extends InstantSpell implements TargetedLocationSpell {

	int range;
	int tickSpeed;
	MagicMaterial mat;
	boolean burnTallGrass;
	private boolean checkPlugins;
	int expandRate = 1;
	
	HashSet<Player> fireImmunity;
	
	public FirenovaSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 3);
		tickSpeed = getConfigInt("tick-speed", 5);
		burnTallGrass = getConfigBoolean("burn-tall-grass", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		mat = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("block-type", "51:15"));
		
		expandRate = getConfigInt("expanding-radius-change", expandRate);
		if (expandRate < 1) expandRate = 1;
		
		if (mat.getMaterial() == Material.FIRE) {
			fireImmunity = new HashSet<Player>();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (fireImmunity != null) {
				fireImmunity.add(player);
			}
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
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled() || fireImmunity == null) return;
		if (event.getEntity() instanceof Player && (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) && fireImmunity.size() > 0) {
			Player player = (Player)event.getEntity();
			if (fireImmunity.contains(player)) {
				// caster is taking damage, cancel it
				event.setCancelled(true);
				player.setFireTicks(0);
			} else if (checkPlugins) {
				// check if nearby players are taking damage
				Location loc = player.getLocation();
				for (Player p : fireImmunity) {
					if (Math.abs(p.getLocation().getX() - loc.getX()) < range+2 && Math.abs(p.getLocation().getZ() - loc.getZ()) < range+2 && Math.abs(p.getLocation().getY() - loc.getY()) < range) {
						// nearby, check plugins for pvp
						MagicSpellsEntityDamageByEntityEvent evt = new MagicSpellsEntityDamageByEntityEvent(p, player, DamageCause.ENTITY_ATTACK, event.getDamage());
						Bukkit.getServer().getPluginManager().callEvent(evt);
						if (evt.isCancelled()) {
							event.setCancelled(true);
							player.setFireTicks(0);
							break;
						}
					}
				}
			}
		}
	}
	
	private class FirenovaAnimation implements Runnable {
		Player player;
		int i = 0;
		Block center;
		HashSet<Block> fireBlocks = new HashSet<Block>();
		int taskId;
		
		public FirenovaAnimation(Player player) {
			this.player = player;
			center = player.getLocation().getBlock();
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickSpeed);
		}
		
		public FirenovaAnimation(Location location) {
			center = location.getBlock();
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickSpeed);
		}
		
		@Override
		public void run() {
			// remove old fire blocks
			for (Block block : fireBlocks) {
				if (block.getType() == mat.getMaterial()) {
					block.setType(Material.AIR);
				}
			}
			fireBlocks.clear();
						
			i += expandRate;
			if (i <= range) {
				// set next ring on fire
				int bx = center.getX();
				int y = center.getY();
				int bz = center.getZ();
				for (int x = bx - i; x <= bx + i; x++) {
					for (int z = bz - i; z <= bz + i; z++) {
						if (Math.abs(x-bx) == i || Math.abs(z-bz) == i) {
							Block b = center.getWorld().getBlockAt(x,y,z);
							if (b.getType() == Material.AIR || (burnTallGrass && b.getType() == Material.LONG_GRASS)) {
								Block under = b.getRelative(BlockFace.DOWN);
								if (under.getType() == Material.AIR || (burnTallGrass && under.getType() == Material.LONG_GRASS)) {
									b = under;
								}
								mat.setBlock(b, false);
								fireBlocks.add(b);
							} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || (burnTallGrass && b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS)) {
								b = b.getRelative(BlockFace.UP);
								mat.setBlock(b, false);
								fireBlocks.add(b);
							}
						}
					}
				}
			} else if (i > range+1) {
				// stop if done
				Bukkit.getServer().getScheduler().cancelTask(taskId);
				if (fireImmunity != null && player != null) {
					fireImmunity.remove(player);
				}
			}
		}
	}

}