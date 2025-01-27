package com.nisovin.magicspells.util.trackers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.instant.ItemProjectileSpell;

public class ItemProjectileTracker implements Runnable, Tracker {

	private ItemProjectileSpell spell;

	private Component itemName;

	private ItemStack item;

	private int spellDelay;
	private int pickupDelay;
	private int removeDelay;
	private int tickInterval;
	private int spellInterval;
	private int itemNameDelay;
	private int specialEffectInterval;

	private float speed;
	private float yOffset;
	private float hitRadius;
	private float vertSpeed;
	private float vertHitRadius;
	private float targetYOffset;
	private float rotationOffset;

	private boolean callEvents;
	private boolean changePitch;
	private boolean vertSpeedUsed;
	private boolean stopOnHitGround;
	private boolean stopOnHitEntity;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private float startXOffset;
	private float startYOffset;
	private float startZOffset;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	private NoMagicZoneManager zoneManager;

	private ValidTargetList targetList;

	private Item entity;
	private SpellData data;
	private Vector velocity;
	private Location startLocation;
	private Location currentLocation;
	private Location previousLocation;

	private boolean landed = false;
	private boolean groundSpellCasted = false;
	private boolean stopped = false;

	private ScheduledTask task;
	private int count = 0;

	public ItemProjectileTracker(SpellData data) {
		this.startLocation = data.location();
		this.data = data;
	}

	public void start() {
		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();

		//relativeOffset
		startLocation.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(startLocation, relativeOffset.setY(0));

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();

		if (vertSpeedUsed) velocity = startLocation.clone().getDirection().setY(0).multiply(speed).setY(vertSpeed);
		else velocity = startLocation.clone().getDirection().multiply(speed);
		Util.rotateVector(velocity, rotationOffset);
		entity = startLocation.getWorld().dropItem(startLocation, item.clone());
		entity.setGravity(projectileHasGravity);
		entity.setPickupDelay(pickupDelay);
		entity.setVelocity(velocity);

		if (spell != null) {
			spell.playEffects(EffectPosition.CASTER, data.caster(), data);
			spell.playEffects(EffectPosition.PROJECTILE, entity, data);
			spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, entity.getLocation(), data.caster(), entity, data);
		}

		task = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval, entity);

		MagicSpells.scheduleDelayedTask(() -> {
			entity.customName(itemName);
			entity.setCustomNameVisible(true);
		}, itemNameDelay, entity);

		MagicSpells.scheduleDelayedTask(this::stop, removeDelay, entity);
	}

	@Override
	public void run() {
		if (entity == null || !entity.isValid() || entity.isDead()) {
			stop();
			return;
		}

		count++;

		previousLocation = currentLocation.clone();
		currentLocation = entity.getLocation();
		currentLocation.setDirection(entity.getVelocity());

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) {
				return;
			}
		}

		data = data.location(currentLocation);

		if (spell != null && specialEffectInterval > 0 && count % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);

		if (zoneManager.willFizzle(currentLocation, spell)) {
			stop();
			return;
		}

		if (count % spellInterval == 0 && spellOnTick != null) spellOnTick.subcast(data);

		for (Entity e : entity.getNearbyEntities(hitRadius, vertHitRadius, hitRadius)) {
			if (!(e instanceof LivingEntity target)) continue;
			if (!targetList.canTarget(data.caster(), e)) continue;

			SpellTargetEvent event = new SpellTargetEvent(spell, data, target);
			if (!event.callEvent()) continue;

			SpellData subData = event.getSpellData();

			if (spell != null) spell.playEffects(EffectPosition.TARGET, subData.target(), subData);
			if (spellOnHitEntity != null) spellOnHitEntity.subcast(subData.noLocation());
			if (stopOnHitEntity) stop();
			return;
		}

		if (entity.isOnGround()) {
			if (spellOnHitGround != null && !groundSpellCasted) {
				spellOnHitGround.subcast(data.location(entity.getLocation()));
				groundSpellCasted = true;
			}
			if (stopOnHitGround) {
				stop();
				return;
			}
			if (!landed) MagicSpells.scheduleDelayedTask(() -> {
				if (spellOnDelay != null) spellOnDelay.subcast(data.location(entity.getLocation()));
				stop();
			}, spellDelay, entity);
			landed = true;
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (spell != null) {
			if (entity != null) spell.playEffects(EffectPosition.DELAYED, entity.getLocation(), data);
			if (removeTracker) ItemProjectileSpell.getProjectileTrackers().remove(this);
		}
		if (entity != null) entity.remove();
		MagicSpells.cancelTask(task);
		stopped = true;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public void setCaster(LivingEntity caster) {
		data = data.caster(caster);
	}

	public Item getEntity() {
		return entity;
	}

	public void setEntity(Item entity) {
		this.entity = entity;
	}

	public Vector getVelocity() {
		return velocity;
	}

	public void setVelocity(Vector velocity) {
		this.velocity = velocity;
	}

	public Location getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Location startLocation) {
		this.startLocation = startLocation;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	public Location getPreviousLocation() {
		return previousLocation;
	}

	public void setPreviousLocation(Location previousLocation) {
		this.previousLocation = previousLocation;
	}

	public float getTargetYOffset() {
		return targetYOffset;
	}

	public void setTargetYOffset(float targetYOffset) {
		this.targetYOffset = targetYOffset;
	}

	public float getStartXOffset() {
		return startXOffset;
	}

	public void setStartXOffset(float startXOffset) {
		this.startXOffset = startXOffset;
	}

	public float getStartYOffset() {
		return startYOffset;
	}

	public void setStartYOffset(float startYOffset) {
		this.startYOffset = startYOffset;
	}

	public float getStartZOffset() {
		return startZOffset;
	}

	public void setStartZOffset(float startZOffset) {
		this.startZOffset = startZOffset;
	}

	public boolean shouldCallEvents() {
		return callEvents;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public boolean shouldChangePitch() {
		return changePitch;
	}

	public void setChangePitch(boolean changePitch) {
		this.changePitch = changePitch;
	}

	public float getPower() {
		return data.power();
	}

	public void setPower(float power) {
		data = data.power(power);
	}

	public Component getItemName() {
		return itemName;
	}

	public void setItemName(Component itemName) {
		this.itemName = itemName;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public int getSpellDelay() {
		return spellDelay;
	}

	public void setSpellDelay(int spellDelay) {
		this.spellDelay = spellDelay;
	}

	public int getPickupDelay() {
		return pickupDelay;
	}

	public void setPickupDelay(int pickupDelay) {
		this.pickupDelay = pickupDelay;
	}

	public int getRemoveDelay() {
		return removeDelay;
	}

	public void setRemoveDelay(int removeDelay) {
		this.removeDelay = removeDelay;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public int getSpellInterval() {
		return spellInterval;
	}

	public void setSpellInterval(int spellInterval) {
		this.spellInterval = spellInterval;
	}

	public int getItemNameDelay() {
		return itemNameDelay;
	}

	public void setItemNameDelay(int itemNameDelay) {
		this.itemNameDelay = itemNameDelay;
	}

	public int getSpecialEffectInterval() {
		return specialEffectInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public float getYOffset() {
		return yOffset;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}

	public float getVertSpeed() {
		return vertSpeed;
	}

	public void setVertSpeed(float vertSpeed) {
		this.vertSpeed = vertSpeed;
	}

	public float getVertHitRadius() {
		return vertHitRadius;
	}

	public void setVertHitRadius(float vertHitRadius) {
		this.vertHitRadius = vertHitRadius;
	}

	public float getRotationOffset() {
		return rotationOffset;
	}

	public void setRotationOffset(float rotationOffset) {
		this.rotationOffset = rotationOffset;
	}

	public boolean isVertSpeedUsed() {
		return vertSpeedUsed;
	}

	public void setVertSpeedUsed(boolean vertSpeedUsed) {
		this.vertSpeedUsed = vertSpeedUsed;
	}

	public boolean shouldStopOnHitGround() {
		return stopOnHitGround;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
	}

	public boolean shouldStopOnHitEntity() {
		return stopOnHitEntity;
	}

	public void setStopOnHitEntity(boolean stopOnHitEntity) {
		this.stopOnHitEntity = stopOnHitEntity;
	}

	public boolean shouldProjectileHaveGravity() {
		return projectileHasGravity;
	}

	public void setProjectileHasGravity(boolean projectileHasGravity) {
		this.projectileHasGravity = projectileHasGravity;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public Subspell getSpellOnTick() {
		return spellOnTick;
	}

	public void setSpellOnTick(Subspell spellOnTick) {
		this.spellOnTick = spellOnTick;
	}

	public Subspell getSpellOnDelay() {
		return spellOnDelay;
	}

	public void setSpellOnDelay(Subspell spellOnDelay) {
		this.spellOnDelay = spellOnDelay;
	}

	public Subspell getSpellOnHitEntity() {
		return spellOnHitEntity;
	}

	public void setSpellOnHitEntity(Subspell spellOnHitEntity) {
		this.spellOnHitEntity = spellOnHitEntity;
	}

	public Subspell getSpellOnHitGround() {
		return spellOnHitGround;
	}

	public void setSpellOnHitGround(Subspell spellOnHitGround) {
		this.spellOnHitGround = spellOnHitGround;
	}

	public ItemProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ItemProjectileSpell spell) {
		this.spell = spell;
	}

	public ValidTargetList getTargetList() {
		return targetList;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

}
