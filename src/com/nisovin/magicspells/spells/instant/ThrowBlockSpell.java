package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ThrowBlockSpell extends InstantSpell implements TargetedLocationSpell {

	MagicMaterial material;
	int tntFuse;
	float velocity;
	boolean applySpellPowerToVelocity;
	float verticalAdjustment;
	float yOffset;
	int rotationOffset;
	float fallDamage;
	int fallDamageMax;
	boolean dropItem;
	boolean removeBlocks;
	boolean preventBlocks;
	boolean callTargetEvent;
	boolean checkPlugins;
	boolean ensureSpellCast;
	boolean stickyBlocks;
	boolean projectileHasGravity;
	String spellOnLand;
	TargetedLocationSpell spell;
	
	Map<Entity, FallingBlockInfo> fallingBlocks;
	int cleanTask = -1;
	
	public ThrowBlockSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String blockTypeInfo = getConfigString("block-type", "anvil");
		if (blockTypeInfo.toLowerCase().startsWith("primedtnt:")) {
			String[] split = blockTypeInfo.split(":");
			material = null;
			tntFuse = Integer.parseInt(split[1]);
		} else {
			material = MagicSpells.getItemNameResolver().resolveBlock(blockTypeInfo);
			tntFuse = 0;
		}
		velocity = getConfigFloat("velocity", 1);
		applySpellPowerToVelocity = getConfigBoolean("apply-spell-power-to-velocity", false);
		verticalAdjustment = getConfigFloat("vertical-adjustment", 0.5F);
		yOffset = getConfigFloat("y-offset", 0F);
		rotationOffset = getConfigInt("rotation-offset", 0);
		fallDamage = getConfigFloat("fall-damage", 2.0F);
		fallDamageMax = getConfigInt("fall-damage-max", 20);
		dropItem = getConfigBoolean("drop-item", false);
		removeBlocks = getConfigBoolean("remove-blocks", false);
		preventBlocks = getConfigBoolean("prevent-blocks", false);
		callTargetEvent = getConfigBoolean("call-target-event", true);
		checkPlugins = getConfigBoolean("check-plugins", false);
		ensureSpellCast = getConfigBoolean("ensure-spell-cast", true);
		stickyBlocks = getConfigBoolean("sticky-blocks", false);
		spellOnLand = getConfigString("spell-on-land", null);
		projectileHasGravity = getConfigBoolean("gravity", true);
	}	
	
	@Override
	public void initialize() {
		super.initialize();
		if (material == null && tntFuse == 0) {
			MagicSpells.error("Invalid block-type for " + internalName + " spell");
		}
		if (spellOnLand != null && !spellOnLand.isEmpty()) {
			Spell s = MagicSpells.getSpellByInternalName(spellOnLand);
			if (s != null && s instanceof TargetedLocationSpell) {
				spell = (TargetedLocationSpell)s;
			} else {
				MagicSpells.error("Invalid spell-on-land for " + internalName + " spell");
			}
		}
		if (fallDamage > 0 || removeBlocks || preventBlocks || spell != null || ensureSpellCast || stickyBlocks) {
			fallingBlocks = new HashMap<Entity, ThrowBlockSpell.FallingBlockInfo>();
			if (material != null) {
				registerEvents(new ThrowBlockListener(this));
			} else if (tntFuse > 0) {
				registerEvents(new TntListener());
			}
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = getVector(player.getLocation(), power);
			Location l = player.getEyeLocation().add(v);
			l.add(0, yOffset, 0);
			spawnFallingBlock(player, power, l, v);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private Vector getVector(Location loc, float power) {
		Vector v = loc.getDirection();
		if (verticalAdjustment != 0) {
			v.setY(v.getY() + verticalAdjustment);
		}
		if (rotationOffset != 0) {
			Util.rotateVector(v, rotationOffset);
		}
		v.normalize().multiply(velocity);
		if (applySpellPowerToVelocity) {
			v.multiply(power);
		}
		return v;
	}
	
	private void spawnFallingBlock(Player player, float power, Location location, Vector velocity) {
		Entity entity = null;
		FallingBlockInfo info = new FallingBlockInfo(player, power);
		if (material != null) {
			FallingBlock block = material.spawnFallingBlock(location);
			block.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, block);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, player.getLocation(), block.getLocation(), player, block);
			block.setVelocity(velocity);
			block.setDropItem(dropItem);
			if (fallDamage > 0) {
				MagicSpells.getVolatileCodeHandler().setFallingBlockHurtEntities(block, fallDamage, fallDamageMax);
			}
			if (ensureSpellCast || stickyBlocks) {
				new ThrowBlockMonitor(block, info);
			}
			entity = block;
		} else if (tntFuse > 0) {
			TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class);
			tnt.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, tnt);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, player.getLocation(), tnt.getLocation(), player, tnt);
			tnt.setFuseTicks(tntFuse);
			tnt.setVelocity(velocity);
			entity = tnt;
		}
		if (fallingBlocks != null) {
			fallingBlocks.put(entity, info);
			if (cleanTask < 0) {
				startTask();
			}
		}
	}
	
	void startTask() {
		cleanTask = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Entity> iter = fallingBlocks.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = iter.next();
					if (entity instanceof FallingBlock) {
						FallingBlock block = (FallingBlock)entity;
						if (!block.isValid()) {
							iter.remove();
							if (removeBlocks) {
								Block b = block.getLocation().getBlock();
								if (material.equals(b) || (material.getMaterial() == Material.ANVIL && b.getType() == Material.ANVIL)) {
									b.setType(Material.AIR);
									playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
								}
							}
						}
					} else if (entity instanceof TNTPrimed) {
						TNTPrimed tnt = (TNTPrimed)entity;
						if (!tnt.isValid() || tnt.isDead()) {
							iter.remove();
						}
					}
				}
				if (fallingBlocks.size() == 0) {
					cleanTask = -1;
				} else {
					startTask();
				}
			}
		}, 500);
	}

	class ThrowBlockMonitor implements Runnable {
		FallingBlock block;
		FallingBlockInfo info;
		int task;
		int counter = 0;
		
		public ThrowBlockMonitor(FallingBlock block, FallingBlockInfo info) {
			this.block = block;
			this.info = info;
			this.task = MagicSpells.scheduleRepeatingTask(this, 20, 1);
		}
		
		@Override
		public void run() {
			if (stickyBlocks && !block.isDead()) {
				if (block.getVelocity().lengthSquared() < .01) {
					if (!preventBlocks) {
						Block b = block.getLocation().getBlock();
						if (b.getType() == Material.AIR) {
							BlockUtils.setBlockFromFallingBlock(b, block, true);
						}
					}
					if (!info.spellActivated && spell != null) {
						if (info.player != null) {
							spell.castAtLocation(info.player, block.getLocation(), info.power);
						} else {
							spell.castAtLocation(block.getLocation(), info.power);
						}
						info.spellActivated = true;
					}
					block.remove();
				}
			}
			if (ensureSpellCast && block.isDead()) {
				if (!info.spellActivated && spell != null) {
					if (info.player != null) {
						spell.castAtLocation(info.player, block.getLocation(), info.power);
					} else {
						spell.castAtLocation(block.getLocation(), info.power);
					}
				}
				info.spellActivated = true;
				MagicSpells.cancelTask(task);
			}
			if (counter++ > 1500) {
				MagicSpells.cancelTask(task);
			}
		}
	}

	class ThrowBlockListener implements Listener {
		
		ThrowBlockSpell thisSpell;
		
		public ThrowBlockListener(ThrowBlockSpell spell) {
			this.thisSpell = spell;
		}
		
		@EventHandler(ignoreCancelled=true)
		public void onDamage(EntityDamageByEntityEvent event) {
			FallingBlockInfo info = null;
			if (removeBlocks || preventBlocks) {
				info = fallingBlocks.get(event.getDamager());
			} else {
				info = fallingBlocks.remove(event.getDamager());
			}
			if (info != null && event.getEntity() instanceof LivingEntity) {
				float power = info.power;
				if (callTargetEvent && info.player != null) {
					SpellTargetEvent evt = new SpellTargetEvent(thisSpell, info.player, (LivingEntity)event.getEntity(), power);
					Bukkit.getPluginManager().callEvent(evt);
					if (evt.isCancelled()) {
						event.setCancelled(true);
						return;
					} else {
						power = evt.getPower();
					}
				}
				double damage = event.getDamage() * power;
				if (checkPlugins && info.player != null) {
					MagicSpellsEntityDamageByEntityEvent evt = new MagicSpellsEntityDamageByEntityEvent(info.player, event.getEntity(), DamageCause.ENTITY_ATTACK, damage);
					Bukkit.getPluginManager().callEvent(evt);
					if (evt.isCancelled()) {
						event.setCancelled(true);
						return;
					}
				}
				event.setDamage(damage);
				if (spell != null && !info.spellActivated) {
					if (info.player != null) {
						spell.castAtLocation(info.player, event.getEntity().getLocation(), power);
					} else {
						spell.castAtLocation(event.getEntity().getLocation(), power);
					}
					info.spellActivated = true;
				}
			}
		}
		
		@EventHandler(ignoreCancelled=true)
		public void onBlockLand(EntityChangeBlockEvent event) {
			if (!preventBlocks && spell == null) return;
			FallingBlockInfo info = fallingBlocks.get(event.getEntity());
			if (info != null) {
				if (preventBlocks) {
					event.getEntity().remove();
					event.setCancelled(true);
				}
				if (spell != null && !info.spellActivated) {
					if (info.player != null) {
						spell.castAtLocation(info.player, event.getBlock().getLocation().add(.5, .5, .5), info.power);
					} else {
						spell.castAtLocation(event.getBlock().getLocation().add(.5, .5, .5), info.power);
					}
					info.spellActivated = true;
				}
			}
		}
	
	}
	
	class TntListener implements Listener {
		
		@EventHandler
		void onExplode(EntityExplodeEvent event) {
			FallingBlockInfo info = fallingBlocks.get(event.getEntity());
			if (info != null) {
				if (preventBlocks) {
					event.blockList().clear();
					event.setYield(0f);
					event.setCancelled(true);
					event.getEntity().remove();
				}
				if (spell != null && !info.spellActivated) {
					if (info.player != null) {
						spell.castAtLocation(info.player, event.getEntity().getLocation(), info.power);
					} else {
						spell.castAtLocation(event.getEntity().getLocation(), info.power);
					}
					info.spellActivated = true;					
				}
			}
		}
		
	}
		
	@Override
	public void turnOff() {
		if (fallingBlocks != null) {
			fallingBlocks.clear();
		}
	}
	
	class FallingBlockInfo {
		Player player;
		float power;
		boolean spellActivated;
		public FallingBlockInfo(Player player, float power) {
			this.player = player;
			this.power = power;
			this.spellActivated = false;
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Vector v = getVector(target, power);
		spawnFallingBlock(caster, power, target.clone().add(0, yOffset, 0), v);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Vector v = getVector(target, power);
		spawnFallingBlock(null, power, target.clone().add(0, yOffset, 0), v);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}

}
