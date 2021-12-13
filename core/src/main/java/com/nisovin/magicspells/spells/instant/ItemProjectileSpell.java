package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ItemProjectileTracker;

public class ItemProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ItemProjectileTracker> trackerSet;

	private final String itemName;
	private final String spellOnTickName;
	private final String spellOnDelayName;
	private final String spellOnHitEntityName;
	private final String spellOnHitGroundName;

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
	private float rotationOffset;

	private boolean checkPlugins;
	private boolean vertSpeedUsed;
	private boolean stopOnHitGround;
	private boolean stopOnHitEntity;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	private NoMagicZoneManager zoneManager;

	public ItemProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString("item", "iron_sword"));
		if (magicItem != null) item = magicItem.getItemStack();

		spellDelay = getConfigInt("spell-delay", 40);
		pickupDelay = getConfigInt("pickup-delay", 100);
		removeDelay = getConfigInt("remove-delay", 100);
		tickInterval = getConfigInt("tick-interval", 1);
		spellInterval = getConfigInt("spell-interval", 2);
		itemNameDelay = getConfigInt("item-name-delay", 10);
		specialEffectInterval = getConfigInt("special-effect-interval", 2);

		speed = getConfigFloat("speed", 1F);
		yOffset = getConfigFloat("y-offset", 0F);
		hitRadius = getConfigFloat("hit-radius", 1F);
		vertSpeed = getConfigFloat("vert-speed", 0F);
		vertHitRadius = getConfigFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigFloat("rotation-offset", 0F);

		if (vertSpeed != 0) vertSpeedUsed = true;
		checkPlugins = getConfigBoolean("check-plugins", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		projectileHasGravity = getConfigBoolean("gravity", true);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		if (yOffset != 0) relativeOffset.setY(yOffset);

		itemName = Util.colorize(getConfigString("item-name", ""));
		spellOnTickName = getConfigString("spell-on-tick", "");
		spellOnDelayName = getConfigString("spell-on-delay", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnTick = new Subspell(spellOnTickName);
		if (!spellOnTick.process()) {
			if (!spellOnTickName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
			spellOnTick = null;
		}

		spellOnDelay = new Subspell(spellOnDelayName);
		if (!spellOnDelay.process()) {
			if (!spellOnDelayName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-delay defined!");
			spellOnDelay = null;
		}

		spellOnHitEntity = new Subspell(spellOnHitEntityName);
		if (!spellOnHitEntity.process()) {
			if (!spellOnHitEntityName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			spellOnHitEntity = null;
		}

		spellOnHitGround = new Subspell(spellOnHitGroundName);
		if (!spellOnHitGround.process()) {
			if (!spellOnHitGroundName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			spellOnHitGround = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (ItemProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ItemProjectileTracker tracker = new ItemProjectileTracker(caster, caster.getLocation(), power);
			setupTracker(tracker, caster, power, args);
			tracker.start();
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location target, float power, String[] args) {
		ItemProjectileTracker tracker = new ItemProjectileTracker(livingEntity, target, power);
		setupTracker(tracker, livingEntity, power, args);
		tracker.start();
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void setupTracker(ItemProjectileTracker tracker, LivingEntity caster, float power, String[] args) {
		tracker.setSpell(this);

		tracker.setItemName(itemName);
		tracker.setItem(item);

		tracker.setSpellDelay(spellDelay);
		tracker.setPickupDelay(pickupDelay);
		tracker.setRemoveDelay(removeDelay);
		tracker.setTickInterval(tickInterval);
		tracker.setSpellInterval(spellInterval);
		tracker.setItemNameDelay(itemNameDelay);
		tracker.setSpecialEffectInterval(specialEffectInterval);

		tracker.setSpeed(speed);
		tracker.setYOffset(yOffset);
		tracker.setHitRadius(hitRadius);
		tracker.setVertSpeed(vertSpeed);
		tracker.setVertHitRadius(vertHitRadius);
		tracker.setRotationOffset(rotationOffset);

		tracker.setCallEvents(checkPlugins);
		tracker.setVertSpeedUsed(vertSpeedUsed);
		tracker.setStopOnHitGround(stopOnHitGround);
		tracker.setStopOnHitEntity(stopOnHitEntity);
		tracker.setProjectileHasGravity(projectileHasGravity);

		tracker.setRelativeOffset(relativeOffset);

		tracker.setSpellOnTick(spellOnTick);
		tracker.setSpellOnDelay(spellOnDelay);
		tracker.setSpellOnHitGround(spellOnHitGround);
		tracker.setSpellOnHitEntity(spellOnHitEntity);

		tracker.setTargetList(validTargetList);
	}

	public static Set<ItemProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
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

	public void playEffects(EffectPosition position, Location loc) {
		playSpellEffects(position, loc);
	}

	public void playEffects(EffectPosition position, Entity entity) {
		playSpellEffects(position, entity);
	}

	public void playTrackingLineEffects(EffectPosition position, Location startLocation, Location location, LivingEntity caster, Projectile projectile) {
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile);
	}

}
