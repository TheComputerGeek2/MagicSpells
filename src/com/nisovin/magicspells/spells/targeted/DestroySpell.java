package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicBlockMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;

public class DestroySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	int horizRadius;
	int vertRadius;
	int horizRadiusSq;
	int vertRadiusSq;
	float velocity;
	VelocityType velocityType;
	boolean preventLandingBlocks;
	int fallingBlockDamage;
	Set<Material> blockTypesToThrow;
	Set<Material> blockTypesToRemove;
	
	Set<FallingBlock> fallingBlocks;
	
	public DestroySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		horizRadius = getConfigInt("horiz-radius", 3);
		horizRadiusSq = horizRadius * horizRadius;
		vertRadius = getConfigInt("vert-radius", 3);
		vertRadiusSq = vertRadius * vertRadius;
		velocity = getConfigFloat("velocity", 0);
		
		String vType = getConfigString("velocity-type", "none");
		// TODO use a switch here
		if (vType.equalsIgnoreCase("out")) {
			velocityType = VelocityType.OUT;
		} else if (vType.equalsIgnoreCase("up")) {
			velocityType = VelocityType.UP;
		} else if (vType.equalsIgnoreCase("fountain")) {
			velocityType = VelocityType.UP_OUT;
		} else if (vType.equalsIgnoreCase("random")) {
			velocityType = VelocityType.RANDOM;
		} else if (vType.equalsIgnoreCase("randomup")) {
			velocityType = VelocityType.RANDOMUP;
		} else if (vType.equalsIgnoreCase("down")) {
			velocityType = VelocityType.DOWN;
		} else if (vType.equalsIgnoreCase("toward")) {
			velocityType = VelocityType.TOWARD;
		} else if (vType.equalsIgnoreCase("away")) {
			velocityType = VelocityType.AWAY;
		} else {
			velocityType = VelocityType.NONE;
		}
		
		preventLandingBlocks = config.getBoolean("prevent-landing-blocks", false);
		fallingBlockDamage = getConfigInt("falling-block-damage", 0);
		
		List<String> toThrow = getConfigStringList("block-types-to-throw", null);
		if (toThrow != null && !toThrow.isEmpty()) {
			blockTypesToThrow = EnumSet.noneOf(Material.class);
			for (String s : toThrow) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m == null) continue;
				Material material = m.getMaterial();
				if (material == null) continue;
				blockTypesToThrow.add(material);
			}
		}
		
		List<String> toRemove = getConfigStringList("block-types-to-remove", null);
		if (toRemove != null && !toRemove.isEmpty()) {
			blockTypesToRemove = EnumSet.noneOf(Material.class);
			for (String s : toRemove) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m == null) continue;
				Material material = m.getMaterial();
				if (material == null) continue;
				blockTypesToRemove.add(material);
			}
		}
		
		if (preventLandingBlocks) {
			registerEvents(new FallingBlockListener());
			// TODO convert this to lambda
			MagicSpells.scheduleRepeatingTask(new Runnable() {
				@Override
				public void run() {
					if (fallingBlocks.isEmpty()) return;
					Iterator<FallingBlock> iter = fallingBlocks.iterator();
					while (iter.hasNext()) {
						if (!iter.next().isValid()) iter.remove();
					}
				}
				// TODO convert this to terms of time unit values
			}, 600, 600);
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block b = getTargetedBlock(player, power);
			if (b != null && b.getType() != Material.AIR) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, b.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) {
					b = null;
				} else {
					b = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}
			if (b != null && b.getType() != Material.AIR) {
				Location loc = b.getLocation().add(.5, .5, .5);
				doIt(player.getLocation(), loc);
				playSpellEffects(player, loc);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void doIt(Location source, Location target) {
		int centerX = target.getBlockX();
		int centerY = target.getBlockY();
		int centerZ = target.getBlockZ();

		List<Block> blocksToThrow = new ArrayList<>();
		List<Block> blocksToRemove = new ArrayList<>();
		
		for (int y = centerY - vertRadius; y <= centerY + vertRadius; y++) {
			for (int x = centerX - horizRadius; x <= centerX + horizRadius; x++) {
				for (int z = centerZ - horizRadius; z <= centerZ + horizRadius; z++) {
					Block b = target.getWorld().getBlockAt(x, y, z);
					if (b.getType() != Material.BEDROCK && b.getType() != Material.AIR) {
						if (blockTypesToThrow != null) {
							if (blockTypesToThrow.contains(b.getType())) {
								blocksToThrow.add(b);
							} else if (blockTypesToRemove != null) {
								if (blockTypesToRemove.contains(b.getType())) {
									blocksToRemove.add(b);
								}
							} else if (!b.getType().isSolid()) {
								blocksToRemove.add(b);
							}
						} else {
							if (b.getType().isSolid()) {
								blocksToThrow.add(b);
							} else {
								blocksToRemove.add(b);
							}
						}
					}
				}
			}
		}
		
		// TODO Java 8 this
		for (Block b : blocksToRemove) {
			b.setType(Material.AIR);
		}
		
		for (Block b : blocksToThrow) {
			MagicMaterial mat = new MagicBlockMaterial(b.getType());
			Location l = new Location(target.getWorld(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
			FallingBlock fb = mat.spawnFallingBlock(l);
			playSpellEffects(EffectPosition.PROJECTILE, fb);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, source, fb.getLocation(), null, fb);
			fb.setDropItem(false);
			Vector v;
			if (velocityType == VelocityType.OUT) {
				v = l.toVector().subtract(target.toVector()).normalize();
				v.setX(v.getX() + ((Math.random() - .5) / 4));
				v.setZ(v.getZ() + ((Math.random() - .5) / 4));
				v.multiply(velocity);
			} else if (velocityType == VelocityType.UP) {
				v = new Vector(0, velocity, 0);
				v.setY(v.getY() + ((Math.random() - .5) / 4));
			} else if (velocityType == VelocityType.UP_OUT) {
				v = l.toVector().setY(0).subtract(target.toVector().setY(0)).normalize().setY(1);
				v.setX(v.getX() + ((Math.random() - .5) / 4));
				v.setZ(v.getZ() + ((Math.random() - .5) / 4));
				v.multiply(velocity);
			} else if (velocityType == VelocityType.RANDOM) {
				v = new Vector(Math.random() - .5, Math.random() - .5, Math.random() - .5);
				v.normalize().multiply(velocity);
			} else if (velocityType == VelocityType.RANDOMUP) {
				v = new Vector(Math.random() - .5, Math.random() / 2, Math.random() - .5);
				v.normalize().multiply(velocity);
				fb.setVelocity(v);
			} else if (velocityType == VelocityType.DOWN) {
				v = new Vector(0, -velocity, 0);
			} else if (velocityType == VelocityType.TOWARD) {
				v = source.toVector().subtract(l.toVector()).normalize().multiply(velocity);
			} else if (velocityType == VelocityType.AWAY) {
				v = l.toVector().subtract(source.toVector()).normalize().multiply(velocity);
			} else {
				v = new Vector(0, (Math.random() - .5) / 4, 0);
			}
			
			if (v != null) fb.setVelocity(v);
			if (fallingBlockDamage > 0) MagicSpells.getVolatileCodeHandler().setFallingBlockHurtEntities(fb, fallingBlockDamage, fallingBlockDamage);
			
			if (preventLandingBlocks) fallingBlocks.add(fb);
			b.setType(Material.AIR);
		}
		
	}
	
	/*private boolean blockInRange(Block block, Location location) {
		Location l = block.getLocation().add(.5, .5, .5);
		System.out.println(distanceSq(l.getX(), l.getZ(), location.getX(), location.getZ()) + "  - " + horizRadiusSq);
		System.out.println(distanceSq(l.getY(), l.getZ(), location.getY(), location.getZ()) + " - " + vertRadiusSq);
		return 
				distanceSq(l.getX(), l.getZ(), location.getX(), location.getZ()) < horizRadiusSq &&
				distanceSq(l.getY(), l.getZ(), location.getY(), location.getZ()) < vertRadiusSq &&
				distanceSq(l.getY(), l.getX(), location.getY(), location.getX()) < vertRadiusSq;
	}
	
	private double distanceSq(double x1, double y1, double x2, double y2) {
		double xdiff = (x2 - x1);
		double ydiff = (y2 - y1);
		return xdiff * xdiff + ydiff * ydiff;
	}*/

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		doIt(caster.getLocation(), target);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		doIt(from, target.getLocation());
		playSpellEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		doIt(from, target.getLocation());
		playSpellEffects(from, target);
		return true;
	}
	
	// TODO move to its own file or use lambda
	class FallingBlockListener implements Listener {
		
		@EventHandler
		public void onBlockLand(EntityChangeBlockEvent event) {
			boolean removed = fallingBlocks.remove(event.getEntity());
			if (removed) event.setCancelled(true);
		}
		
	}
	
	// TODO does this belong in its own file?
	public enum VelocityType {
		
		NONE,
		UP,
		OUT,
		UP_OUT,
		RANDOM,
		RANDOMUP,
		DOWN,
		TOWARD,
		AWAY
		
	}
	
}
