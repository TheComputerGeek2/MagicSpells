package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ItemProjectileTracker;

public class ItemProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ItemProjectileTracker> trackerSet;

	private final String spellOnTickName;
	private final String spellOnDelayName;
	private final String spellOnHitEntityName;
	private final String spellOnHitGroundName;

	private ItemStack item;

	private Component itemName;

	private ConfigData<Integer> spellDelay;
	private ConfigData<Integer> pickupDelay;
	private ConfigData<Integer> removeDelay;
	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> spellInterval;
	private ConfigData<Integer> itemNameDelay;
	private ConfigData<Integer> specialEffectInterval;

	private ConfigData<Float> speed;
	private ConfigData<Float> yOffset;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> vertSpeed;
	private ConfigData<Float> vertHitRadius;
	private ConfigData<Float> rotationOffset;

	private boolean checkPlugins;
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

		spellDelay = getConfigDataInt("spell-delay", 40);
		pickupDelay = getConfigDataInt("pickup-delay", 100);
		removeDelay = getConfigDataInt("remove-delay", 100);
		tickInterval = getConfigDataInt("tick-interval", 1);
		spellInterval = getConfigDataInt("spell-interval", 2);
		itemNameDelay = getConfigDataInt("item-name-delay", 10);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 2);

		speed = getConfigDataFloat("speed", 1F);
		yOffset = getConfigDataFloat("y-offset", 0F);
		hitRadius = getConfigDataFloat("hit-radius", 1F);
		vertSpeed = getConfigDataFloat("vert-speed", 0F);
		vertHitRadius = getConfigDataFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigDataFloat("rotation-offset", 0F);

		checkPlugins = getConfigBoolean("check-plugins", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		projectileHasGravity = getConfigBoolean("gravity", true);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");

		itemName = Util.getMiniMessage(getConfigString("item-name", null));
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
			ItemProjectileTracker tracker = new ItemProjectileTracker(caster, caster.getLocation(), power, args);
			setupTracker(tracker, caster, power, args);
			tracker.start();
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location target, float power, String[] args) {
		ItemProjectileTracker tracker = new ItemProjectileTracker(livingEntity, target, power, args);
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

		tracker.setSpellDelay(spellDelay.get(caster, null, power, args));
		tracker.setPickupDelay(pickupDelay.get(caster, null, power, args));
		tracker.setRemoveDelay(removeDelay.get(caster, null, power, args));
		tracker.setTickInterval(tickInterval.get(caster, null, power, args));
		tracker.setSpellInterval(spellInterval.get(caster, null, power, args));
		tracker.setItemNameDelay(itemNameDelay.get(caster, null, power, args));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(caster, null, power, args));

		tracker.setSpeed(speed.get(caster, null, power, args));

		float yOffset = this.yOffset.get(caster, null, power, args);
		tracker.setYOffset(yOffset);

		float vertSpeed = this.vertSpeed.get(caster, null, power, args);
		tracker.setVertSpeed(vertSpeed);

		tracker.setHitRadius(hitRadius.get(caster, null, power, args));
		tracker.setVertHitRadius(vertHitRadius.get(caster, null, power, args));
		tracker.setRotationOffset(rotationOffset.get(caster, null, power, args));

		tracker.setCallEvents(checkPlugins);
		tracker.setVertSpeedUsed(vertSpeed != 0);
		tracker.setStopOnHitGround(stopOnHitGround);
		tracker.setStopOnHitEntity(stopOnHitEntity);
		tracker.setProjectileHasGravity(projectileHasGravity);

		Vector relativeOffset = yOffset != 0 ? this.relativeOffset.clone().setY(yOffset) : this.relativeOffset;
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

	public Component getItemName() {
		return itemName;
	}

	public void setItemName(Component itemName) {
		this.itemName = itemName;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
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

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

	public void playTrackingLineEffects(EffectPosition position, Location startLocation, Location location, LivingEntity caster, Projectile projectile, SpellData data) {
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile, data);
	}

}
