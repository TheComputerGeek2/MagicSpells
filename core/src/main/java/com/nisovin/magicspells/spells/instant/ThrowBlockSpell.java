package com.nisovin.magicspells.spells.instant;

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

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ThrowBlockSpell extends InstantSpell implements TargetedLocationSpell {

	private Map<Entity, FallingBlockInfo> fallingBlocks;

	private Material material;

	private int tntFuse;

	private float yOffset;
	private float velocity;
	private float rotationOffset;
	private float verticalAdjustment;

	private boolean dropItem;
	private boolean stickyBlocks;
	private boolean checkPlugins;
	private boolean removeBlocks;
	private boolean preventBlocks;
	private boolean callTargetEvent;
	private boolean ensureSpellCast;
	private boolean projectileHasGravity;
	private boolean applySpellPowerToVelocity;

	private final String spellOnLandName;

	private Subspell spellOnLand;

	private int cleanTask = -1;
	
	public ThrowBlockSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String materialName = getConfigString("block-type", "stone");
		if (materialName.toLowerCase().startsWith("primedtnt:")) {
			String[] split = materialName.split(":");
			material = null;
			tntFuse = Integer.parseInt(split[1]);
		} else {
			material = Util.getMaterial(materialName);
			tntFuse = 0;
		}

		rotationOffset = getConfigFloat("rotation-offset", 0F);

		yOffset = getConfigFloat("y-offset", 0F);
		velocity = getConfigFloat("velocity", 1);
		verticalAdjustment = getConfigFloat("vertical-adjustment", 0.5F);

		dropItem = getConfigBoolean("drop-item", false);
		stickyBlocks = getConfigBoolean("sticky-blocks", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		removeBlocks = getConfigBoolean("remove-blocks", false);
		preventBlocks = getConfigBoolean("prevent-blocks", false);
		callTargetEvent = getConfigBoolean("call-target-event", true);
		ensureSpellCast = getConfigBoolean("ensure-spell-cast", true);
		projectileHasGravity = getConfigBoolean("gravity", true);
		applySpellPowerToVelocity = getConfigBoolean("apply-spell-power-to-velocity", false);

		spellOnLandName = getConfigString("spell-on-land", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (material == null || !material.isBlock() && tntFuse == 0) {
			MagicSpells.error("ThrowBlockSpell '" + internalName + "' has an invalid block-type defined!");
		}

		spellOnLand = new Subspell(spellOnLandName);
		if (!spellOnLand.process() || !spellOnLand.isTargetedLocationSpell()) {
			if (!spellOnLandName.isEmpty()) MagicSpells.error("ThrowBlockSpell '" + internalName + "' has an invalid spell-on-land defined!");
			spellOnLand = null;
		}

		if (removeBlocks || preventBlocks || spellOnLand != null || ensureSpellCast || stickyBlocks) {
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = getVector(caster, caster.getLocation(), power, args);
			Location l = caster.getEyeLocation().add(v);
			l.add(0, yOffset, 0);
			spawnFallingBlock(caster, l, v, power, args);
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		Vector v = getVector(caster, target, power, args);
		spawnFallingBlock(caster, target.clone().add(0, yOffset, 0), v, power, args);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		Vector v = getVector(null, target, power, args);
		spawnFallingBlock(null, target.clone().add(0, yOffset, 0), v, power, args);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(target, power, null);
	}

	private Vector getVector(LivingEntity caster, Location loc, float power, String[] args) {
		Vector v = loc.getDirection();
		if (verticalAdjustment != 0) v.setY(v.getY() + verticalAdjustment);
		if (rotationOffset != 0) Util.rotateVector(v, rotationOffset);
		v.normalize().multiply(velocity);
		if (applySpellPowerToVelocity) v.multiply(power);
		return v;
	}
	
	private void spawnFallingBlock(LivingEntity caster, Location location, Vector velocity, float power, String[] args) {
		Entity entity = null;
		FallingBlockInfo info = new FallingBlockInfo(caster, power);

		if (material != null) {
			FallingBlock block = location.getWorld().spawnFallingBlock(location, material.createBlockData());
			block.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, block);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), block.getLocation(), caster, block);
			block.setVelocity(velocity);
			block.setDropItem(dropItem);
			if (ensureSpellCast || stickyBlocks) new ThrowBlockMonitor(block, info);
			entity = block;
		} else if (tntFuse > 0) {
			TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class);
			tnt.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, tnt);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), tnt.getLocation(), caster, tnt);
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
		cleanTask = MagicSpells.scheduleDelayedTask(() -> {
			Iterator<Entity> iter = fallingBlocks.keySet().iterator();
			while (iter.hasNext()) {
				Entity entity = iter.next();
				if (entity instanceof FallingBlock block) {
					if (block.isValid()) continue;
					iter.remove();
					if (!removeBlocks) continue;
					Block b = block.getLocation().getBlock();
					if (material.equals(b.getType()) || (material == Material.ANVIL && b.getType() == Material.ANVIL)) {
						playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
						b.setType(Material.AIR);
					}
				} else if (entity instanceof TNTPrimed tnt) {
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
			task = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, 1);
		}
		
		@Override
		public void run() {
			if (stickyBlocks && !block.isDead()) {
				if (block.getVelocity().lengthSquared() < .01) {
					if (!preventBlocks) {
						Block b = block.getLocation().getBlock();
						if (b.getType() == Material.AIR) BlockUtils.setBlockFromFallingBlock(b, block, true);
					}
					if (!info.spellActivated && spellOnLand != null) {
						if (info.caster != null) spellOnLand.castAtLocation(info.caster, block.getLocation(), info.power);
						else spellOnLand.castAtLocation(null, block.getLocation(), info.power);
						info.spellActivated = true;
					}
					block.remove();
				}
			}
			if (ensureSpellCast && block.isDead()) {
				if (!info.spellActivated && spellOnLand != null) {
					if (info.caster != null) spellOnLand.castAtLocation(info.caster, block.getLocation(), info.power);
					else spellOnLand.castAtLocation(null, block.getLocation(), info.power);
				}
				info.spellActivated = true;
				MagicSpells.cancelTask(task);
			}
			if (counter++ > 1500) MagicSpells.cancelTask(task);
		}
		
	}

	private class ThrowBlockListener implements Listener {
		
		private ThrowBlockSpell thisSpell;
		
		private ThrowBlockListener(ThrowBlockSpell throwBlockSpell) {
			thisSpell = throwBlockSpell;
		}
		
		@EventHandler(ignoreCancelled=true)
		private void onDamage(EntityDamageByEntityEvent event) {
			FallingBlockInfo info;
			if (removeBlocks || preventBlocks) info = fallingBlocks.get(event.getDamager());
			else info = fallingBlocks.remove(event.getDamager());
			if (info == null || !(event.getEntity() instanceof LivingEntity entity)) return;
			float power = info.power;
			if (callTargetEvent && info.caster != null) {
				SpellTargetEvent evt = new SpellTargetEvent(thisSpell, info.caster, entity, power);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}
				power = evt.getPower();
			}
			double damage = event.getDamage() * power;
			if (checkPlugins && info.caster != null) {
				MagicSpellsEntityDamageByEntityEvent evt = new MagicSpellsEntityDamageByEntityEvent(info.caster, entity, DamageCause.ENTITY_ATTACK, damage, ThrowBlockSpell.this);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}
			}
			event.setDamage(damage);
			if (spellOnLand != null && !info.spellActivated) {
				if (info.caster != null) spellOnLand.castAtLocation(info.caster, entity.getLocation(), power);
				else spellOnLand.castAtLocation(null, entity.getLocation(), power);
				info.spellActivated = true;
			}
		}
		
		@EventHandler(ignoreCancelled=true)
		private void onBlockLand(EntityChangeBlockEvent event) {
			if (!preventBlocks && spellOnLand == null) return;
			FallingBlockInfo info = fallingBlocks.get(event.getEntity());
			if (info == null) return;
			if (preventBlocks) {
				event.getEntity().remove();
				event.setCancelled(true);
			}
			if (spellOnLand != null && !info.spellActivated) {
				if (info.caster != null) spellOnLand.castAtLocation(info.caster, event.getBlock().getLocation().add(0.5, 0.5, 0.5), info.power);
				else spellOnLand.castAtLocation(null, event.getBlock().getLocation().add(0.5, 0.5, 0.5), info.power);
				info.spellActivated = true;
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
			if (spellOnLand != null && !info.spellActivated) {
				if (info.caster != null) spellOnLand.castAtLocation(info.caster, entity.getLocation(), info.power);
				else spellOnLand.castAtLocation(null, entity.getLocation(), info.power);
				info.spellActivated = true;
			}
		}
		
	}

	private static class FallingBlockInfo {
		
		private LivingEntity caster;
		private float power;
		private boolean spellActivated;

		private FallingBlockInfo(LivingEntity caster, float castPower) {
			this.caster = caster;
			power = castPower;
			spellActivated = false;
		}
		
	}

	public Map<Entity, FallingBlockInfo> getFallingBlocks() {
		return fallingBlocks;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public int getTntFuse() {
		return tntFuse;
	}

	public void setTntFuse(int tntFuse) {
		this.tntFuse = tntFuse;
	}

	public float getRotationOffset() {
		return rotationOffset;
	}

	public void setRotationOffset(float rotationOffset) {
		this.rotationOffset = rotationOffset;
	}

	public float getYOffset() {
		return yOffset;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}

	public float getVelocity() {
		return velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}

	public float getVerticalAdjustment() {
		return verticalAdjustment;
	}

	public void setVerticalAdjustment(float verticalAdjustment) {
		this.verticalAdjustment = verticalAdjustment;
	}

	public Subspell getSpellOnLand() {
		return spellOnLand;
	}

	public void setSpellOnLand(Subspell spellOnLand) {
		this.spellOnLand = spellOnLand;
	}

	public boolean shouldDropItem() {
		return dropItem;
	}

	public void setDropItem(boolean dropItem) {
		this.dropItem = dropItem;
	}

	public boolean shouldBlocksStick() {
		return stickyBlocks;
	}

	public void setStickyBlocks(boolean stickyBlocks) {
		this.stickyBlocks = stickyBlocks;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

	public boolean shouldRemoveBlocks() {
		return removeBlocks;
	}

	public void setRemoveBlocks(boolean removeBlocks) {
		this.removeBlocks = removeBlocks;
	}

	public boolean shouldPreventBlocks() {
		return preventBlocks;
	}

	public void setPreventBlocks(boolean preventBlocks) {
		this.preventBlocks = preventBlocks;
	}

	public boolean shouldCallTargetEvent() {
		return callTargetEvent;
	}

	public void setCallTargetEvent(boolean callTargetEvent) {
		this.callTargetEvent = callTargetEvent;
	}

	public boolean shouldEnsureSpellCast() {
		return ensureSpellCast;
	}

	public void setEnsureSpellCast(boolean ensureSpellCast) {
		this.ensureSpellCast = ensureSpellCast;
	}

	public boolean shouldProjectileHaveGravity() {
		return projectileHasGravity;
	}

	public void setProjectileHasGravity(boolean projectileHasGravity) {
		this.projectileHasGravity = projectileHasGravity;
	}

	public boolean shouldSpellPowerApplyToVelocity() {
		return applySpellPowerToVelocity;
	}

	public void setApplySpellPowerToVelocity(boolean applySpellPowerToVelocity) {
		this.applySpellPowerToVelocity = applySpellPowerToVelocity;
	}

}
