package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class ThrowBlockJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private Map<Entity, FallingBlockInfo> fallingBlocks;

	private Material material;
	private String materialName;

	private int tntFuse;
	private int rotationOffset;

	private float yOffset;
	private float velocity;
	private float verticalAdjustment;

	private boolean dropItem;
	private boolean stickyBlocks;
	private boolean checkPlugins;
	private boolean removeBlocks;
	private boolean preventBlocks;
	private boolean callTargetEvent;
	private boolean ensureJutsuCast;
	private boolean projectileHasGravity;
	private boolean applyJutsuPowerToVelocity;

	private String jutsuOnLandName;
	private Ninjutsu jutsuOnLand;

	private int cleanTask = -1;
	
	public ThrowBlockJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("block-type", "stone");
		if (materialName.toLowerCase().startsWith("primedtnt:")) {
			String[] split = materialName.split(":");
			material = null;
			tntFuse = Integer.parseInt(split[1]);
		} else {
			material = Material.getMaterial(materialName.toUpperCase());
			tntFuse = 0;
		}

		rotationOffset = getConfigInt("rotation-offset", 0);

		yOffset = getConfigFloat("y-offset", 0F);
		velocity = getConfigFloat("velocity", 1);
		verticalAdjustment = getConfigFloat("vertical-adjustment", 0.5F);

		dropItem = getConfigBoolean("drop-item", false);
		stickyBlocks = getConfigBoolean("sticky-blocks", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		removeBlocks = getConfigBoolean("remove-blocks", false);
		preventBlocks = getConfigBoolean("prevent-blocks", false);
		callTargetEvent = getConfigBoolean("call-target-event", true);
		ensureJutsuCast = getConfigBoolean("ensure-jutsu-cast", true);
		projectileHasGravity = getConfigBoolean("gravity", true);
		applyJutsuPowerToVelocity = getConfigBoolean("apply-jutsu-power-to-velocity", false);

		jutsuOnLandName = getConfigString("jutsu-on-land", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (material == null || !material.isBlock() && tntFuse == 0) {
			MagicJutsus.error("ThrowBlockJutsu '" + internalName + "' has an invalid block-type defined!");
		}

		jutsuOnLand = new Ninjutsu(jutsuOnLandName);
		if (!jutsuOnLand.process() || !jutsuOnLand.isTargetedLocationJutsu()) {
			if (!jutsuOnLandName.isEmpty()) MagicJutsus.error("ThrowBlockJutsu '" + internalName + "' has an invalid jutsu-on-land defined!");
			jutsuOnLand = null;
		}

		if (removeBlocks || preventBlocks || jutsuOnLand != null || ensureJutsuCast || stickyBlocks) {
			fallingBlocks = new HashMap<>();
			if (material != null) registerEvents(new ThrowBlockListener(this));
			else if (tntFuse > 0) registerEvents(new TntListener());
		}
	}

	@Override
	public void turnOff() {
		if (fallingBlocks != null) fallingBlocks.clear();
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Vector v = getVector(livingEntity.getLocation(), power);
			Location l = livingEntity.getEyeLocation().add(v);
			l.add(0, yOffset, 0);
			spawnFallingBlock(livingEntity, power, l, v);
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Vector v = getVector(target, power);
		spawnFallingBlock(caster, power, target.clone().add(0, yOffset, 0), v);
		playJutsuEffects(EffectPosition.CASTER, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Vector v = getVector(target, power);
		spawnFallingBlock(null, power, target.clone().add(0, yOffset, 0), v);
		playJutsuEffects(EffectPosition.CASTER, target);
		return true;
	}

	private Vector getVector(Location loc, float power) {
		Vector v = loc.getDirection();
		if (verticalAdjustment != 0) v.setY(v.getY() + verticalAdjustment);
		if (rotationOffset != 0) Util.rotateVector(v, rotationOffset);
		v.normalize().multiply(velocity);
		if (applyJutsuPowerToVelocity) v.multiply(power);
		return v;
	}
	
	private void spawnFallingBlock(LivingEntity livingEntity, float power, Location location, Vector velocity) {
		Entity entity = null;
		FallingBlockInfo info = new FallingBlockInfo(livingEntity, power);

		if (material != null) {
			FallingBlock block = location.getWorld().spawnFallingBlock(location, material.createBlockData());
			block.setGravity(projectileHasGravity);
			playJutsuEffects(EffectPosition.PROJECTILE, block);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, livingEntity.getLocation(), block.getLocation(), livingEntity, block);
			block.setVelocity(velocity);
			block.setDropItem(dropItem);
			if (ensureJutsuCast || stickyBlocks) new ThrowBlockMonitor(block, info);
			entity = block;
		} else if (tntFuse > 0) {
			TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class);
			tnt.setGravity(projectileHasGravity);
			playJutsuEffects(EffectPosition.PROJECTILE, tnt);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, livingEntity.getLocation(), tnt.getLocation(), livingEntity, tnt);
			tnt.setFuseTicks(tntFuse);
			tnt.setVelocity(velocity);
			entity = tnt;
		}

		if (entity == null) return;

		if (fallingBlocks != null) {
			fallingBlocks.put(entity, info);
			if (cleanTask < 0) startTask();
		}
	}
	
	private void startTask() {
		cleanTask = MagicJutsus.scheduleDelayedTask(() -> {
			Iterator<Entity> iter = fallingBlocks.keySet().iterator();
			while (iter.hasNext()) {
				Entity entity = iter.next();
				if (entity instanceof FallingBlock) {
					FallingBlock block = (FallingBlock) entity;
					if (block.isValid()) continue;
					iter.remove();
					if (!removeBlocks) continue;
					Block b = block.getLocation().getBlock();
					if (material.equals(b.getType()) || (material == Material.ANVIL && b.getType() == Material.ANVIL)) {
						playJutsuEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
						b.setType(Material.AIR);
					}
				} else if (entity instanceof TNTPrimed) {
					TNTPrimed tnt = (TNTPrimed) entity;
					if (!tnt.isValid() || tnt.isDead()) iter.remove();
				}
			}
			if (fallingBlocks.isEmpty()) cleanTask = -1;
			else startTask();
		}, 500);
	}

	private class ThrowBlockMonitor implements Runnable {
		
		private FallingBlock block;
		private FallingBlockInfo info;
		private int task;
		private int counter = 0;
		
		private ThrowBlockMonitor(FallingBlock fallingBlock, FallingBlockInfo fallingBlockInfo) {
			block = fallingBlock;
			info = fallingBlockInfo;
			task = MagicJutsus.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, 1);
		}
		
		@Override
		public void run() {
			if (stickyBlocks && !block.isDead()) {
				if (block.getVelocity().lengthSquared() < .01) {
					if (!preventBlocks) {
						Block b = block.getLocation().getBlock();
						if (b.getType() == Material.AIR) BlockUtils.setBlockFromFallingBlock(b, block, true);
					}
					if (!info.jutsuActivated && jutsuOnLand != null) {
						if (info.caster != null) jutsuOnLand.castAtLocation(info.caster, block.getLocation(), info.power);
						else jutsuOnLand.castAtLocation(null, block.getLocation(), info.power);
						info.jutsuActivated = true;
					}
					block.remove();
				}
			}
			if (ensureJutsuCast && block.isDead()) {
				if (!info.jutsuActivated && jutsuOnLand != null) {
					if (info.caster != null) jutsuOnLand.castAtLocation(info.caster, block.getLocation(), info.power);
					else jutsuOnLand.castAtLocation(null, block.getLocation(), info.power);
				}
				info.jutsuActivated = true;
				MagicJutsus.cancelTask(task);
			}
			if (counter++ > 1500) MagicJutsus.cancelTask(task);
		}
		
	}

	private class ThrowBlockListener implements Listener {
		
		private ThrowBlockJutsu thisJutsu;
		
		private ThrowBlockListener(ThrowBlockJutsu throwBlockJutsu) {
			thisJutsu = throwBlockJutsu;
		}
		
		@EventHandler(ignoreCancelled=true)
		private void onDamage(EntityDamageByEntityEvent event) {
			FallingBlockInfo info;
			if (removeBlocks || preventBlocks) info = fallingBlocks.get(event.getDamager());
			else info = fallingBlocks.remove(event.getDamager());
			if (info == null || !(event.getEntity() instanceof LivingEntity)) return;
			LivingEntity entity = (LivingEntity) event.getEntity();
			float power = info.power;
			if (callTargetEvent && info.caster != null) {
				JutsuTargetEvent evt = new JutsuTargetEvent(thisJutsu, info.caster, entity, power);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}
				power = evt.getPower();
			}
			double damage = event.getDamage() * power;
			if (checkPlugins && info.caster != null) {
				MagicJutsusEntityDamageByEntityEvent evt = new MagicJutsusEntityDamageByEntityEvent(info.caster, entity, DamageCause.ENTITY_ATTACK, damage);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}
			}
			event.setDamage(damage);
			if (jutsuOnLand != null && !info.jutsuActivated) {
				if (info.caster != null) jutsuOnLand.castAtLocation(info.caster, entity.getLocation(), power);
				else jutsuOnLand.castAtLocation(null, entity.getLocation(), power);
				info.jutsuActivated = true;
			}
		}
		
		@EventHandler(ignoreCancelled=true)
		private void onBlockLand(EntityChangeBlockEvent event) {
			if (!preventBlocks && jutsuOnLand == null) return;
			FallingBlockInfo info = fallingBlocks.get(event.getEntity());
			if (info == null) return;
			if (preventBlocks) {
				event.getEntity().remove();
				event.setCancelled(true);
			}
			if (jutsuOnLand != null && !info.jutsuActivated) {
				if (info.caster != null) jutsuOnLand.castAtLocation(info.caster, event.getBlock().getLocation().add(0.5, 0.5, 0.5), info.power);
				else jutsuOnLand.castAtLocation(null, event.getBlock().getLocation().add(0.5, 0.5, 0.5), info.power);
				info.jutsuActivated = true;
			}
		}
	
	}
	
	private class TntListener implements Listener {
		
		@EventHandler
		private void onExplode(EntityExplodeEvent event) {
			Entity entity = event.getEntity();
			FallingBlockInfo info = fallingBlocks.get(entity);
			if (info == null) return;
			if (preventBlocks) {
				event.blockList().clear();
				event.setYield(0F);
				event.setCancelled(true);
				event.getEntity().remove();
			}
			if (jutsuOnLand != null && !info.jutsuActivated) {
				if (info.caster != null) jutsuOnLand.castAtLocation(info.caster, entity.getLocation(), info.power);
				else jutsuOnLand.castAtLocation(null, entity.getLocation(), info.power);
				info.jutsuActivated = true;
			}
		}
		
	}

	private static class FallingBlockInfo {
		
		private LivingEntity caster;
		private float power;
		private boolean jutsuActivated;

		private FallingBlockInfo(LivingEntity caster, float castPower) {
			this.caster = caster;
			power = castPower;
			jutsuActivated = false;
		}
		
	}

}
