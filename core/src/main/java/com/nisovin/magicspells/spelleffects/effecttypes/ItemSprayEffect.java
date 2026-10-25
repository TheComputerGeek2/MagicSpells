package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.util.TriState;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.pdc.PersistentDataEntry;

@Name("itemspray")
public class ItemSprayEffect extends SpellEffect {

	public static final PersistentDataEntry<Byte, Boolean> MS_ITEM_SPRAY = new PersistentDataEntry<>(PersistentDataType.BOOLEAN, "ms_item_spray");

	private static final List<Item> items = new ArrayList<>();

	private ConfigData<Material> material;

	private ConfigData<Vector> velocity;

	private ConfigData<Double> force;

	private ConfigData<Integer> amount;
	private ConfigData<Integer> duration;

	private ConfigData<Boolean> gravity;
	private ConfigData<Boolean> removeItemFriction;
	private ConfigData<Boolean> resolveForcePerItem;
	private ConfigData<Boolean> resolveDurationPerItem;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		material = ConfigDataUtil.getMaterial(config, "type", null);

		velocity = ConfigDataUtil.getVector(config, "velocity", null);

		force = ConfigDataUtil.getDouble(config, "force", 1);

		amount = ConfigDataUtil.getInteger(config, "amount", 15);
		duration = ConfigDataUtil.getInteger(config, "duration", 10);

		gravity = ConfigDataUtil.getBoolean(config, "gravity", true);
		removeItemFriction = ConfigDataUtil.getBoolean(config, "remove-item-friction", false);
		resolveForcePerItem = ConfigDataUtil.getBoolean(config, "resolve-force-per-item", false);
		resolveDurationPerItem = ConfigDataUtil.getBoolean(config, "resolve-duration-per-item", false);
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		Material material = this.material.get(data);
		if (material == null) return null;
		ItemStack itemStack = new ItemStack(material);

		Location loc = location.clone().add(0, 1, 0);

		boolean resolveForcePerItem = this.resolveForcePerItem.get(data);
		double force = resolveForcePerItem ? 0 : this.force.get(data);

		boolean resolveDurationPerItem = this.resolveDurationPerItem.get(data);
		int duration = resolveDurationPerItem ? 0 : this.duration.get(data);

		boolean gravity = this.gravity.get(data);
		boolean removeItemFriction = this.removeItemFriction.get(data);

		int amount = this.amount.get(data);
		for (int i = 0; i < amount; i++) {
			Item dropped = loc.getWorld().dropItem(loc, itemStack, item -> {
				MS_ITEM_SPRAY.set(item.getPersistentDataContainer(), true);

				Vector velocity = this.velocity.get(data);
				if (velocity == null) velocity = new Vector(
						random.nextDouble() - 0.5,
						random.nextDouble() - 0.5,
						random.nextDouble() - 0.5
				);
				double f = resolveForcePerItem ? this.force.get(data) : force;
				item.setVelocity(velocity.clone().multiply(f));

				// Prevents merging too.
				item.setCanPlayerPickup(false);
				item.setCanMobPickup(false);
				item.setPersistent(false);
				item.setGravity(gravity);
				if (removeItemFriction) item.setFrictionState(TriState.FALSE);
			});

			int dur = resolveDurationPerItem ? this.duration.get(data) : duration;
			items.add(dropped);
			MagicSpells.scheduleDelayedTask(() -> {
				items.remove(dropped);
				dropped.remove();
			}, dur);
		}
		return null;
	}

	@Override
	public void turnOff() {
		items.forEach(Entity::remove);
		items.clear();
	}

}
