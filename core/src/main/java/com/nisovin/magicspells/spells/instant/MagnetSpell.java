package com.nisovin.magicspells.spells.instant;

import java.util.UUID;
import java.util.Collection;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spelleffects.effecttypes.ItemSprayEffect;

import net.kyori.adventure.util.TriState;

public class MagnetSpell extends InstantSpell implements TargetedLocationSpell {

	private final ConfigData<Double> radius;
	private final ConfigData<Double> velocity;

	private final ConfigData<Boolean> teleport;
	private final ConfigData<Boolean> forcePickup;
	private final ConfigData<Boolean> removeItemGravity;
	private final ConfigData<Boolean> removeItemFriction;
	private final ConfigData<Boolean> powerAffectsRadius;
	private final ConfigData<Boolean> powerAffectsVelocity;
	private final ConfigData<Boolean> resolveVelocityPerItem;

	public MagnetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 5);
		velocity = getConfigDataDouble("velocity", 1);

		teleport = getConfigDataBoolean("teleport-items", false);
		forcePickup = getConfigDataBoolean("force-pickup", false);
		removeItemGravity = getConfigDataBoolean("remove-item-gravity", false);
		removeItemFriction = getConfigDataBoolean("remove-item-friction", false);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", true);
		powerAffectsVelocity = getConfigDataBoolean("power-affects-velocity", true);
		resolveVelocityPerItem = getConfigDataBoolean("resolve-velocity-per-item", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		return castAtLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location location = data.location();

		double radius = this.radius.get(data);
		if (powerAffectsRadius.get(data)) radius *= data.power();
		radius = Math.min(radius, MagicSpells.getGlobalRadius());

		boolean teleport = this.teleport.get(data);
		boolean forcePickup = this.forcePickup.get(data);
		boolean removeItemGravity = this.removeItemGravity.get(data);
		boolean removeItemFriction = this.removeItemFriction.get(data);
		boolean powerAffectsVelocity = this.powerAffectsVelocity.get(data);
		boolean resolveVelocityPerItem = this.resolveVelocityPerItem.get(data);

		Collection<Item> items = location.getNearbyEntitiesByType(Item.class, radius, item -> {
			if (!item.isValid() || item.getItemStack().isEmpty()) return false;
			if (item.getPersistentDataContainer().has(ItemSprayEffect.MS_ITEM_SPRAY)) return false;

			if (data.hasCaster()) {
				LivingEntity caster = data.caster();
				if (caster instanceof Mob && !item.canMobPickup()) return false;
				if (caster instanceof Player player && (!item.canPlayerPickup() || !player.canSee(item))) return false;

				UUID owner = item.getOwner();
				if (owner != null && !owner.equals(caster.getUniqueId())) return false;
			}

			if (forcePickup) {
				item.setPickupDelay(0);
				return true;
			}

			return item.getPickupDelay() <= 0;
		});

		double velocity = 0;
		if (!resolveVelocityPerItem) {
			velocity = this.velocity.get(data);
			if (powerAffectsVelocity) velocity *= data.power();
		}

		for (Item item : items) {
			if (removeItemGravity) item.setGravity(false);
			if (removeItemFriction) item.setFrictionState(TriState.FALSE);

			playSpellEffects(EffectPosition.PROJECTILE, item, data);

			if (teleport) item.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
			else {
				if (resolveVelocityPerItem) {
					velocity = this.velocity.get(data);
					if (powerAffectsVelocity) velocity *= data.power();
				}

				Vector v = location.toVector().subtract(item.getLocation().toVector());
				if (!v.isZero()) v.normalize().multiply(velocity);

				item.setVelocity(v);
			}
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
