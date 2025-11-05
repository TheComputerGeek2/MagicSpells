package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.util.Vector;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class VolleySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private static final Map<UUID, VolleyData> ARROW_DATA = new HashMap<>();

	private static ArrowListener arrowListener;

	private final ConfigData<Integer> fire;
	private final ConfigData<Integer> arrows;
	private final ConfigData<Integer> pierceLevel;
	private final ConfigData<Integer> removeDelay;
	private final ConfigData<Integer> shootInterval;

	private final ConfigData<Float> speed;
	private final ConfigData<Float> spread;

	private final ConfigData<Double> damage;
	private final ConfigData<Double> yOffset;

	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> critical;
	private final ConfigData<Boolean> noTarget;
	private final ConfigData<Boolean> powerAffectsSpeed;
	private final ConfigData<Boolean> powerAffectsArrowCount;
	private final ConfigData<Boolean> resolveOptionsPerArrow;

	private final ItemStack weapon;
	private final ItemStack arrowItem;
	private final ConfigData<Color> color;
	private final ConfigData<PotionType> potionType;
	private final List<ConfigData<PotionEffect>> potionEffects;

	public VolleySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fire = getConfigDataInt("fire", 0);
		arrows = getConfigDataInt("arrows", 10);
		pierceLevel = getConfigDataInt("pierce-level", 0);
		removeDelay = getConfigDataInt("remove-delay", 0);
		shootInterval = getConfigDataInt("shoot-interval", 0);

		speed = getConfigDataFloat("speed", 20);
		spread = getConfigDataFloat("spread", 150);

		damage = getConfigDataDouble("damage", 4);
		yOffset = getConfigDataDouble("y-offset", 3);

		gravity = getConfigDataBoolean("gravity", true);
		critical = getConfigDataBoolean("critical", false);
		noTarget = getConfigDataBoolean("no-target", false);
		powerAffectsSpeed = getConfigDataBoolean("power-affects-speed", false);
		powerAffectsArrowCount = getConfigDataBoolean("power-affects-arrow-count", true);
		resolveOptionsPerArrow = getConfigDataBoolean("resolve-options-per-arrow", false);

		MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString("weapon", null));
		weapon = magicItem == null ? null : magicItem.getItemStack().clone();

		magicItem = MagicItems.getMagicItemFromString(getConfigString("arrow-item", null));
		arrowItem = magicItem == null ? null : magicItem.getItemStack().clone();

		color = getConfigDataColor("color", null);
		potionType = getConfigDataRegistryEntry("potion-type", Registry.POTION, null);
		potionEffects = Util.getPotionEffects(getConfigList("potion-effects", null), internalName);

		if (arrowListener == null) {
			arrowListener = new ArrowListener();
			registerEvents(arrowListener);
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		if (noTarget.get(data)) {
			new ArrowShooter(data.caster().getLocation(), null, data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data, false);
		data = info.spellData();

		new ArrowShooter(data.caster().getLocation(), info.target(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster() || noTarget.get(data)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		new ArrowShooter(data.caster().getLocation(), data.location(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		if (noTarget.get(data)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		new ArrowShooter(data.location(), data.target().getLocation(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public void turnOff() {
		arrowListener = null;
		ARROW_DATA.clear();
	}

	private static class ArrowListener implements Listener {

		@EventHandler
		public void onArrowHit(EntityDamageByEntityEvent event) {
			if (event.getCause() != DamageCause.PROJECTILE || !(event.getEntity() instanceof LivingEntity target)) return;

			Entity damagerEntity = event.getDamager();
			if (!(damagerEntity instanceof Arrow arrow)) return;

			VolleyData data = ARROW_DATA.get(arrow.getUniqueId());
			if (data == null) return;

			event.setDamage(data.damage);

			SpellPreImpactEvent preImpactEvent = new SpellPreImpactEvent(data.spell, data.spell, (LivingEntity) arrow.getShooter(), target, 1);
			preImpactEvent.callEvent();
			if (!preImpactEvent.getRedirected()) return;

			event.setCancelled(true);
			arrow.setVelocity(arrow.getVelocity().multiply(-1));
			arrow.teleportAsync(arrow.getLocation().add(arrow.getVelocity()));
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onRemove(EntityRemoveEvent event) {
			ARROW_DATA.remove(event.getEntity().getUniqueId());
		}

	}

	private class ArrowShooter implements Runnable {

		private final SpellData data;

		private final Location from;
		private final Location target;

		private final int arrows;
		private final int taskId;
		private final boolean resolveOptionsPerArrow;

		private boolean gravity;
		private boolean critical;

		private int fire;
		private int count;
		private int pierceLevel;
		private int removeDelay;

		private float speed;
		private float spread;

		private double damage;

		private Color color;
		private PotionType potionType;
		private List<PotionEffect> potionEffects;

		private Vector dir;
		private Location spawn;

		public ArrowShooter(Location from, Location target, SpellData data) {
			this.data = data;
			this.from = from;
			this.target = target;

			if (target == null) dir = from.getDirection();

			resolveOptionsPerArrow = VolleySpell.this.resolveOptionsPerArrow.get(data);

			int arrows = VolleySpell.this.arrows.get(data);
			if (powerAffectsArrowCount.get(data)) arrows = Math.round(arrows * data.power());
			this.arrows = arrows;

			if (!resolveOptionsPerArrow) resolveOptions();

			int shootInterval = VolleySpell.this.shootInterval.get(data);
			if (shootInterval <= 0) {
				for (int i = 0; i < arrows; i++) run();
				taskId = -1;
			} else taskId = MagicSpells.scheduleRepeatingTask(this, 0, shootInterval);

			playSpellEffects(data);
		}

		public void resolveOptions() {
			spawn = from.clone().add(0, yOffset.get(data), 0);
			if (target != null) dir = target.toVector().subtract(spawn.toVector());

			SpellData subData = data.location(spawn);

			gravity = VolleySpell.this.gravity.get(subData);
			critical = VolleySpell.this.critical.get(subData);

			fire = VolleySpell.this.fire.get(subData);
			pierceLevel = VolleySpell.this.pierceLevel.get(subData);
			removeDelay = VolleySpell.this.removeDelay.get(subData);

			color = VolleySpell.this.color.get(subData);
			potionType = VolleySpell.this.potionType.get(subData);

			if (VolleySpell.this.potionEffects != null) {
				potionEffects = new ArrayList<>();

				for (ConfigData<PotionEffect> effectData : VolleySpell.this.potionEffects)
					potionEffects.add(effectData.get(subData));
			}

			speed = VolleySpell.this.speed.get(subData) / 10;
			if (powerAffectsSpeed.get(subData)) speed *= subData.power();
			spread = VolleySpell.this.spread.get(subData) / 10;

			damage = VolleySpell.this.damage.get(subData);
		}

		@Override
		public void run() {
			if (resolveOptionsPerArrow) resolveOptions();

			Arrow arrow = spawn.getWorld().spawnArrow(spawn, dir, speed, spread);
			if (arrowItem != null) arrow.setItemStack(arrowItem);
			if (weapon != null) arrow.setWeapon(weapon);
			arrow.setPierceLevel(pierceLevel);
			arrow.setCritical(critical);
			arrow.setGravity(gravity);

			if (color != null) arrow.setColor(color);
			if (potionType != null) arrow.setBasePotionType(potionType);
			if (potionEffects != null)
				for (PotionEffect potionEffectData : potionEffects)
					arrow.addCustomEffect(potionEffectData, false);

			arrow.setDamage(damage);
			ARROW_DATA.put(arrow.getUniqueId(), new VolleyData(VolleySpell.this, damage));

			if (fire > 0) arrow.setFireTicks(fire);
			if (data.hasCaster()) arrow.setShooter(data.caster(), false);
			if (removeDelay > 0) MagicSpells.scheduleDelayedTask(arrow::remove, removeDelay);

			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

			playSpellEffects(EffectPosition.PROJECTILE, arrow, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, arrow.getLocation(), data.caster(), arrow, data);

			if (taskId != -1) {
				count++;
				if (count >= arrows) MagicSpells.cancelTask(taskId);
			}
		}

	}

	private record VolleyData(VolleySpell spell, double damage) {
	}

}
