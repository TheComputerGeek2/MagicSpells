package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Tag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class ThrowBlockSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Map<Entity, FallingBlockInfo> fallingBlocks = new HashMap<>();
	private static ThrowBlockListener throwBlockListener;
	private static int cleanTask = -1;

	private final ConfigData<BlockData> material;

	private final ConfigData<Integer> tntFuse;

	private final ConfigData<Float> yOffset;
	private final ConfigData<Float> velocity;
	private final ConfigData<Float> rotationOffset;
	private final ConfigData<Float> verticalAdjustment;

	private final ConfigData<Boolean> dropItem;
	private final ConfigData<Boolean> stickyBlocks;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> removeBlocks;
	private final ConfigData<Boolean> preventBlocks;
	private final ConfigData<Boolean> callTargetEvent;
	private final ConfigData<Boolean> ensureSpellCast;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> projectileHasGravity;
	private final ConfigData<Boolean> applySpellPowerToVelocity;

	private final String spellOnLandName;

	private Subspell spellOnLand;

	public ThrowBlockSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String blockType = getConfigString("block-type", "stone");
		if (blockType.toLowerCase().startsWith("primedtnt:")) {
			String[] split = blockType.split(":", 2);
			material = null;

			ConfigData<Integer> tntFuse;
			try {
				int fuse = Integer.parseInt(split[1]);
				tntFuse = data -> fuse;
			} catch (NumberFormatException e) {
				tntFuse = FunctionData.build(split[1], Double::intValue, 0);
				if (tntFuse == null)
					MagicSpells.error("Invalid tnt fuse '" + split[1] + "' for ThrowBlockSpell '" + internalName + "'.");
			}
			this.tntFuse = tntFuse;
		} else {
			material = getConfigDataBlockData("block-type", Material.STONE.createBlockData());
			tntFuse = null;
		}

		rotationOffset = getConfigDataFloat("rotation-offset", 0F);

		yOffset = getConfigDataFloat("y-offset", 0F);
		velocity = getConfigDataFloat("velocity", 1);
		verticalAdjustment = getConfigDataFloat("vertical-adjustment", 0.5F);

		dropItem = getConfigDataBoolean("drop-item", false);
		stickyBlocks = getConfigDataBoolean("sticky-blocks", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		removeBlocks = getConfigDataBoolean("remove-blocks", false);
		preventBlocks = getConfigDataBoolean("prevent-blocks", false);
		callTargetEvent = getConfigDataBoolean("call-target-event", true);
		ensureSpellCast = getConfigDataBoolean("ensure-spell-cast", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		projectileHasGravity = getConfigDataBoolean("gravity", true);
		applySpellPowerToVelocity = getConfigDataBoolean("apply-spell-power-to-velocity", false);

		spellOnLandName = getConfigString("spell-on-land", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		if (material == null && tntFuse == null) {
			MagicSpells.error("ThrowBlockSpell '" + internalName + "' has an invalid block-type defined!");
		}

		spellOnLand = initSubspell(spellOnLandName,
				"ThrowBlockSpell '" + internalName + "' has an invalid spell-on-land defined!",
				true);

		if (throwBlockListener == null) {
			throwBlockListener = new ThrowBlockListener();
			registerEvents(throwBlockListener);
		}
	}

	@Override
	public void turnOff() {
		MagicSpells.cancelTask(cleanTask);
		throwBlockListener = null;

		for (Map.Entry<Entity, FallingBlockInfo> entry : fallingBlocks.entrySet()) {
			Entity entity = entry.getKey();
			FallingBlockInfo info = entry.getValue();

			if (entity.isValid()) {
				entity.remove();
				return;
			}

			if (!info.removeBlocks || !(entity instanceof FallingBlock block)) return;

			Block b = block.getLocation().getBlock();
			if (info.material.equals(b.getType()) || (info.material == Material.ANVIL && Tag.ANVIL.isTagged(b.getType()))) {
				info.getSpell().playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation(), info.data);
				b.setType(Material.AIR);
			}
		}
		fallingBlocks.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		Location location = data.caster().getEyeLocation();
		data = data.location(location);

		location.add(0, yOffset.get(data), 0);
		data = data.location(location);

		Vector velocity = getVelocity(location, data);
		location.add(velocity);
		data = data.location(location);

		boolean spawned = spawnFallingBlock(location, velocity, data);
		if (!spawned) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location location = data.location();

		location.add(0, yOffset.get(data), 0);
		data = data.location(location);

		Vector velocity = getVelocity(location, data);
		location.add(velocity);
		data = data.location(location);

		boolean spawned = spawnFallingBlock(location, velocity, data);
		if (!spawned) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		playSpellEffects(EffectPosition.CASTER, location, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private Vector getVelocity(Location loc, SpellData data) {
		Vector v = loc.getDirection();

		float verticalAdjustment = this.verticalAdjustment.get(data);
		if (verticalAdjustment != 0) v.setY(v.getY() + verticalAdjustment);

		float rotationOffset = this.rotationOffset.get(data);
		if (rotationOffset != 0) Util.rotateVector(v, rotationOffset);

		float velocity = this.velocity.get(data);
		if (applySpellPowerToVelocity.get(data)) velocity *= data.power();

		return v.normalize().multiply(velocity);
	}

	private boolean spawnFallingBlock(Location location, Vector velocity, SpellData data) {
		FallingBlockInfo info;
		Entity entity;

		if (material != null) {
			BlockData blockData = this.material.get(data);
			info = new FallingBlockInfo(data, blockData.getMaterial());

			FallingBlock block = location.getWorld().spawn(location, FallingBlock.class, fb -> fb.setBlockData(blockData));
			block.setVelocity(velocity);
			block.setDropItem(dropItem.get(data));
			block.setGravity(projectileHasGravity.get(data));

			boolean stickyBlocks = this.stickyBlocks.get(data);
			boolean ensureSpellCast = this.ensureSpellCast.get(data);
			if (stickyBlocks || ensureSpellCast) new ThrowBlockMonitor(block, info, stickyBlocks, ensureSpellCast);

			playSpellEffects(EffectPosition.PROJECTILE, block, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, data.caster().getLocation(), block.getLocation(), data.caster(), block, data);

			entity = block;
		} else if (tntFuse != null) {
			info = new FallingBlockInfo(data);

			TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class, tntPrimed -> {
				tntPrimed.setGravity(projectileHasGravity.get(data));
				tntPrimed.setFuseTicks(tntFuse.get(data));
				tntPrimed.setVelocity(velocity);
				tntPrimed.setSource(data.caster());
			});

			playSpellEffects(EffectPosition.PROJECTILE, tnt, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, data.caster().getLocation(), tnt.getLocation(), data.caster(), tnt, data);

			entity = tnt;
		} else return false;

		fallingBlocks.put(entity, info);
		startCleanTask();

		return true;
	}

	private static void startCleanTask() {
		if (cleanTask > -1) return;

		cleanTask = MagicSpells.scheduleDelayedTask(() -> {
			Iterator<Map.Entry<Entity, FallingBlockInfo>> iter = fallingBlocks.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Entity, FallingBlockInfo> entry = iter.next();
				FallingBlockInfo info = entry.getValue();

				Entity entity = entry.getKey();
				if (entity.isValid()) continue;

				iter.remove();
				if (entity instanceof FallingBlock block) {
					if (!info.removeBlocks) continue;

					Block b = block.getLocation().getBlock();
					if (info.material.equals(b.getType()) || (info.material == Material.ANVIL && Tag.ANVIL.isTagged(b.getType()))) {
						info.getSpell().playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation(), info.data);
						b.setType(Material.AIR);
					}
				}
			}

			if (fallingBlocks.isEmpty()) cleanTask = -1;
			else startCleanTask();
		}, 500);
	}

	private class ThrowBlockMonitor implements Runnable {

		private final FallingBlock block;
		private final FallingBlockInfo info;

		private final int task;

		private final boolean stickyBlocks;
		private final boolean ensureSpellCast;

		private int counter = 0;

		private ThrowBlockMonitor(FallingBlock block, FallingBlockInfo info, boolean stickyBlocks, boolean ensureSpellCast) {
			this.block = block;
			this.info = info;

			this.stickyBlocks = stickyBlocks;
			this.ensureSpellCast = ensureSpellCast;

			task = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, 1);
		}

		@Override
		public void run() {
			if (stickyBlocks && block.isValid()) {
				if (block.getVelocity().lengthSquared() < .01) {
					if (!info.preventBlocks) {
						Block b = block.getLocation().getBlock();
						if (b.getType() == Material.AIR) BlockUtils.setBlockFromFallingBlock(b, block, true);
					}

					if (!info.spellActivated && spellOnLand != null) {
						spellOnLand.subcast(info.data.location(block.getLocation()));
						info.spellActivated = true;
					}

					block.remove();
				}
			}

			if (ensureSpellCast && !block.isValid()) {
				if (!info.spellActivated && spellOnLand != null) {
					spellOnLand.subcast(info.data.location(block.getLocation()));
					info.spellActivated = true;
				}

				MagicSpells.cancelTask(task);
			}

			if (counter++ > 1500) MagicSpells.cancelTask(task);
		}

	}

	private static class ThrowBlockListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			if (!(event.getEntity() instanceof LivingEntity target)) return;
			Entity block = event.getDamager();

			FallingBlockInfo info = fallingBlocks.get(block);
			if (info == null) return;

			ThrowBlockSpell spell = info.getSpell();

			SpellData subData = info.data;
			if (info.callTargetEvent && info.data.hasCaster()) {
				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, info.data, target);
				if (!targetEvent.callEvent()) {
					event.setCancelled(true);
					return;
				}

				subData = targetEvent.getSpellData();
			}

			double damage = event.getDamage();
			if (info.powerAffectsDamage) damage *= info.data.power();

			if (info.checkPlugins && info.data.hasCaster() && spell.checkFakeDamageEvent(info.data.caster(), info.data.target(), DamageCause.ENTITY_ATTACK, damage)) {
				event.setCancelled(true);
				return;
			}

			SpellApplyDamageEvent spellEvent = new SpellApplyDamageEvent(info.getSpell(), info.data.caster(), info.data.target(), damage, DamageCause.ENTITY_ATTACK, "");
			spellEvent.callEvent();
			event.setDamage(spellEvent.getFinalDamage());

			if (spell.spellOnLand != null && !info.spellActivated) {
				spell.spellOnLand.subcast(subData.retarget(null, block.getLocation()));
				info.spellActivated = true;
			}
		}

		@EventHandler(ignoreCancelled = true)
		private void onBlockLand(EntityChangeBlockEvent event) {
			FallingBlockInfo info = fallingBlocks.get(event.getEntity());
			if (info == null) return;

			if (info.preventBlocks) {
				event.getEntity().remove();
				event.setCancelled(true);
			}

			ThrowBlockSpell spell = info.getSpell();
			if (spell.spellOnLand != null && !info.spellActivated) {
				spell.spellOnLand.subcast(info.data.location(event.getBlock().getLocation().add(0.5, 0.5, 0.5)));
				info.spellActivated = true;
			}
		}

		@EventHandler
		private void onExplode(EntityExplodeEvent event) {
			Entity entity = event.getEntity();
			FallingBlockInfo info = fallingBlocks.remove(entity);
			if (info == null) return;

			if (info.preventBlocks) {
				event.blockList().clear();
				event.setYield(0F);
				event.setCancelled(true);
				event.getEntity().remove();
			}

			ThrowBlockSpell spell = info.getSpell();
			if (spell.spellOnLand != null && !info.spellActivated) {
				spell.spellOnLand.subcast(info.data.location(entity.getLocation()));
				info.spellActivated = true;
			}
		}

	}

	private class FallingBlockInfo {

		private final SpellData data;

		private final Material material;

		private final boolean checkPlugins;
		private final boolean removeBlocks;
		private final boolean preventBlocks;
		private final boolean callTargetEvent;
		private final boolean powerAffectsDamage;

		private boolean spellActivated;

		private FallingBlockInfo(SpellData data, Material material) {
			checkPlugins = ThrowBlockSpell.this.checkPlugins.get(data);
			removeBlocks = ThrowBlockSpell.this.removeBlocks.get(data);
			preventBlocks = ThrowBlockSpell.this.preventBlocks.get(data);
			callTargetEvent = ThrowBlockSpell.this.callTargetEvent.get(data);
			powerAffectsDamage = ThrowBlockSpell.this.powerAffectsDamage.get(data);

			spellActivated = false;

			this.data = data.noTargeting();
			this.material = material;
		}

		private FallingBlockInfo(SpellData data) {
			this(data, null);
		}

		public ThrowBlockSpell getSpell() {
			return ThrowBlockSpell.this;
		}

	}

	public Subspell getSpellOnLand() {
		return spellOnLand;
	}

	public void setSpellOnLand(Subspell spellOnLand) {
		this.spellOnLand = spellOnLand;
	}

}
