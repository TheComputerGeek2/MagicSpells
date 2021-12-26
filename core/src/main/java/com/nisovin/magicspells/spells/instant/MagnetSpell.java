package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class MagnetSpell extends InstantSpell implements TargetedLocationSpell {

	private ConfigData<Double> radius;
	private ConfigData<Double> velocity;

	private boolean teleport;
	private boolean forcePickup;
	private boolean removeItemGravity;
	private boolean powerAffectsRadius;
	private boolean powerAffectsVelocity;
	private boolean resolveVelocityPerItem;

	public MagnetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 5);
		velocity = getConfigDataDouble("velocity", 1);

		teleport = getConfigBoolean("teleport-items", false);
		forcePickup = getConfigBoolean("force-pickup", false);
		removeItemGravity = getConfigBoolean("remove-item-gravity", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		resolveVelocityPerItem = getConfigBoolean("resolve-velocity-per-item", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location location = caster.getLocation();

			List<Item> items = getNearbyItems(caster, location, power, args);
			magnet(caster, location, items, power, args);

			playSpellEffects(EffectPosition.CASTER, caster);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		Collection<Item> targetItems = getNearbyItems(caster, target, power, args);
		magnet(caster, target, targetItems, power, args);

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

	private List<Item> getNearbyItems(LivingEntity caster, Location center, float power, String[] args) {
		double radius = this.radius.get(caster, null, power, args);
		if (powerAffectsRadius) radius *= power;
		radius = Math.min(radius, MagicSpells.getGlobalRadius());

		Collection<Entity> entities = center.getWorld().getNearbyEntities(center, radius, radius, radius);
		List<Item> ret = new ArrayList<>();
		for (Entity e : entities) {
			if (!(e instanceof Item i)) continue;
			ItemStack stack = i.getItemStack();
			if (InventoryUtil.isNothing(stack)) continue;
			if (i.isDead()) continue;

			if (forcePickup) {
				i.setPickupDelay(0);
				ret.add(i);
			} else if (i.getPickupDelay() < i.getTicksLived()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private void magnet(LivingEntity caster, Location location, Collection<Item> items, float power, String[] args) {
		double velocity = 0;
		if (!resolveVelocityPerItem) {
			velocity = this.velocity.get(caster, null, power, args);
			if (powerAffectsVelocity) velocity *= power;
		}

		for (Item i : items) magnet(caster, location, i, power, args, velocity);
	}

	private void magnet(LivingEntity caster, Location origin, Item item, float power, String[] args, double velocity) {
		if (removeItemGravity) item.setGravity(false);
		if (teleport) item.teleport(origin);
		else {
			if (resolveVelocityPerItem) {
				velocity = this.velocity.get(caster, null, power, args);
				if (powerAffectsVelocity) velocity *= power;
			}

			item.setVelocity(origin.toVector().subtract(item.getLocation().toVector()).normalize().multiply(velocity));
		}
		playSpellEffects(EffectPosition.PROJECTILE, item);
	}

	public boolean shouldTeleport() {
		return teleport;
	}

	public void setTeleport(boolean teleport) {
		this.teleport = teleport;
	}

	public boolean shouldForcePickup() {
		return forcePickup;
	}

	public void setForcePickup(boolean forcePickup) {
		this.forcePickup = forcePickup;
	}

	public boolean shouldRemoveItemGravity() {
		return removeItemGravity;
	}

	public void setRemoveItemGravity(boolean removeItemGravity) {
		this.removeItemGravity = removeItemGravity;
	}

}
