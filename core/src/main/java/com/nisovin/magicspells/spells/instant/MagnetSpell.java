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
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class MagnetSpell extends InstantSpell implements TargetedLocationSpell {

	private double radius;
	private double velocity;

	private boolean teleport;
	private boolean forcePickup;
	private boolean removeItemGravity;

	public MagnetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDouble("radius", 5);
		velocity = getConfigDouble("velocity", 1);

		teleport = getConfigBoolean("teleport-items", false);
		forcePickup = getConfigBoolean("force-pickup", false);
		removeItemGravity = getConfigBoolean("remove-item-gravity", false);

		if (radius > MagicSpells.getGlobalRadius()) radius = MagicSpells.getGlobalRadius();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Item> items = getNearbyItems(caster.getLocation(), radius * power);
			magnet(caster.getLocation(), items, power);

			playSpellEffects(EffectPosition.CASTER, caster);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		Collection<Item> targetItems = getNearbyItems(target, radius * power);
		magnet(target, targetItems, power);
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

	private List<Item> getNearbyItems(Location center, double radius) {
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

	private void magnet(Location location, Collection<Item> items, float power) {
		for (Item i : items) magnet(location, i, power);
	}

	private void magnet(Location origin, Item item, float power) {
		if (removeItemGravity) item.setGravity(false);
		if (teleport) item.teleport(origin);
		else item.setVelocity(origin.toVector().subtract(item.getLocation().toVector()).normalize().multiply(velocity * power));
		playSpellEffects(EffectPosition.PROJECTILE, item);
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getVelocity() {
		return velocity;
	}

	public void setVelocity(double velocity) {
		this.velocity = velocity;
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
