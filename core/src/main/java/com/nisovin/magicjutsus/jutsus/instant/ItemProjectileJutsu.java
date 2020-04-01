package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ItemProjectileJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private List<Item> itemList;

	private ItemStack item;

	private int jutsuDelay;
	private int pickupDelay;
	private int removeDelay;
	private int tickInterval;
	private int jutsuInterval;
	private int itemNameDelay;
	private int specialEffectInterval;

	private float speed;
	private float yOffset;
	private float hitRadius;
	private float vertSpeed;
	private float vertHitRadius;
	private float rotationOffset;

	private boolean vertSpeedUsed;
	private boolean stopOnHitGround;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private String itemName;
	private String jutsuOnTickName;
	private String jutsuOnDelayName;
	private String jutsuOnHitEntityName;
	private String jutsuOnHitGroundName;

	private Ninjutsu jutsuOnTick;
	private Ninjutsu jutsuOnDelay;
	private Ninjutsu jutsuOnHitEntity;
	private Ninjutsu jutsuOnHitGround;

	public ItemProjectileJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		itemList = new ArrayList<>();

		item = Util.getItemStackFromString(getConfigString("item", "iron_sword"));

		jutsuDelay = getConfigInt("jutsu-delay", 40);
		pickupDelay = getConfigInt("pickup-delay", 100);
		removeDelay = getConfigInt("remove-delay", 100);
		tickInterval = getConfigInt("tick-interval", 1);
		jutsuInterval = getConfigInt("jutsu-interval", 2);
		itemNameDelay = getConfigInt("item-name-delay", 10);
		specialEffectInterval = getConfigInt("special-effect-interval", 2);

		speed = getConfigFloat("speed", 1F);
		yOffset = getConfigFloat("y-offset", 0F);
		hitRadius = getConfigFloat("hit-radius", 1F);
		vertSpeed = getConfigFloat("vert-speed", 0F);
		vertHitRadius = getConfigFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigFloat("rotation-offset", 0F);

		if (vertSpeed != 0) vertSpeedUsed = true;
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		projectileHasGravity = getConfigBoolean("gravity", true);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		if (yOffset != 0) relativeOffset.setY(yOffset);

		itemName = ChatColor.translateAlternateColorCodes('&', getConfigString("item-name", ""));
		jutsuOnTickName = getConfigString("jutsu-on-tick", "");
		jutsuOnDelayName = getConfigString("jutsu-on-delay", "");
		jutsuOnHitEntityName = getConfigString("jutsu-on-hit-entity", "");
		jutsuOnHitGroundName = getConfigString("jutsu-on-hit-ground", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuOnTick = new Ninjutsu(jutsuOnTickName);
		if (!jutsuOnTick.process()) {
			if (!jutsuOnTickName.isEmpty()) MagicJutsus.error("ItemProjectileJutsu '" + internalName + "' has an invalid jutsu-on-tick defined!");
			jutsuOnTick = null;
		}

		jutsuOnDelay = new Ninjutsu(jutsuOnDelayName);
		if (!jutsuOnDelay.process()) {
			if (!jutsuOnDelayName.isEmpty()) MagicJutsus.error("ItemProjectileJutsu '" + internalName + "' has an invalid jutsu-on-delay defined!");
			jutsuOnDelay = null;
		}

		jutsuOnHitEntity = new Ninjutsu(jutsuOnHitEntityName);
		if (!jutsuOnHitEntity.process()) {
			if (!jutsuOnHitEntityName.isEmpty()) MagicJutsus.error("ItemProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-entity defined!");
			jutsuOnHitEntity = null;
		}

		jutsuOnHitGround = new Ninjutsu(jutsuOnHitGroundName);
		if (!jutsuOnHitGround.process()) {
			if (!jutsuOnHitGroundName.isEmpty()) MagicJutsus.error("ItemProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
			jutsuOnHitGround = null;
		}
	}

	@Override
	public void turnOff() {
		for (Item item : itemList) {
			item.remove();
		}

		itemList.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			new ItemProjectile(livingEntity, livingEntity.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location target, float power) {
		new ItemProjectile(livingEntity, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private class ItemProjectile implements Runnable {

		private LivingEntity caster;
		private Item entity;
		private Vector velocity;
		private Location startLocation;
		private Location currentLocation;
		private float power;

		private boolean landed = false;
		private boolean groundJutsuCasted = false;

		private int taskId;
		private int count = 0;

		private ItemProjectile(LivingEntity caster, Location from, float power) {
			this.caster = caster;
			this.power = power;

			startLocation = from.clone();

			//relativeOffset
			Vector startDirection = from.getDirection().normalize();
			Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
			startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
			startLocation.setY(startLocation.getY() + relativeOffset.getY());

			currentLocation = startLocation.clone();

			if (vertSpeedUsed) velocity = from.getDirection().setY(0).multiply(speed).setY(vertSpeed);
			else velocity = from.getDirection().multiply(speed);
			Util.rotateVector(velocity, rotationOffset);
			entity = from.getWorld().dropItem(startLocation, item.clone());
			entity.setGravity(projectileHasGravity);
			entity.setPickupDelay(pickupDelay);
			entity.setVelocity(velocity);
			itemList.add(entity);

			playJutsuEffects(EffectPosition.PROJECTILE, entity);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, entity.getLocation(), caster, entity);
			
			taskId = MagicJutsus.scheduleRepeatingTask(this, tickInterval, tickInterval);

			MagicJutsus.scheduleDelayedTask(() -> {
				entity.setCustomName(itemName);
				entity.setCustomNameVisible(true);
			}, itemNameDelay);

			MagicJutsus.scheduleDelayedTask(this::stop, removeDelay);
		}
		
		@Override
		public void run() {
			if (entity == null || !entity.isValid() || entity.isDead()) {
				stop();
				return;
			}

			count++;

			currentLocation = entity.getLocation();
			currentLocation.setDirection(entity.getVelocity());
			if (specialEffectInterval > 0 && count % specialEffectInterval == 0) playJutsuEffects(EffectPosition.SPECIAL, currentLocation);

			if (count % jutsuInterval == 0 && jutsuOnTick != null && jutsuOnTick.isTargetedLocationJutsu()) {
				jutsuOnTick.castAtLocation(caster, currentLocation.clone(), power);
			}

			for (Entity e : entity.getNearbyEntities(hitRadius, vertHitRadius, hitRadius)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;

				JutsuTargetEvent event = new JutsuTargetEvent(ItemProjectileJutsu.this, caster, (LivingEntity) e, power);
				EventUtil.call(event);
				if (!event.isCancelled()) {
					if (jutsuOnHitEntity != null) jutsuOnHitEntity.castAtEntity(caster, (LivingEntity) e, event.getPower());
					stop();
					return;
				}
			}

			if (entity.isOnGround()) {
				if (jutsuOnHitGround != null && !groundJutsuCasted) {
					jutsuOnHitGround.castAtLocation(caster, entity.getLocation(), power);
					groundJutsuCasted = true;
				}
				if (stopOnHitGround) {
					stop();
					return;
				}
				if (!landed) MagicJutsus.scheduleDelayedTask(() -> {
					if (jutsuOnDelay != null) jutsuOnDelay.castAtLocation(caster, entity.getLocation(), power);
					stop();
				}, jutsuDelay);
				landed = true;
			}
		}

		private void stop() {
			itemList.remove(entity);
			entity.remove();
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
