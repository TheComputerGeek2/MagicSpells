package com.nisovin.magicspells.volatilecode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.compat.CompatBasics;

import net.minecraft.server.v1_8_R3.AttributeInstance;
import net.minecraft.server.v1_8_R3.AttributeModifier;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityOcelot;
import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.EntityWitch;
import net.minecraft.server.v1_8_R3.EntityWither;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import net.minecraft.server.v1_8_R3.IAttribute;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;

public class VolatileCodeEnabled_1_8_R3 extends VolatileCodeEnabledNMSBase {

	EntityInsentient bossBarEntity;
	public VolatileCodeEnabled_1_8_R3(MagicConfig config) {
		super(config, "1_8_R3");
		this.bossBarEntity = new EntityWither(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle());
		this.bossBarEntity.setCustomNameVisible(false);
		this.bossBarEntity.getDataWatcher().watch(0, (byte) 0x20);
		this.bossBarEntity.getDataWatcher().watch(20, (Integer) 0);

	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		final EntityLiving el = ((CraftLivingEntity) entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(7, Integer.valueOf(color));

		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				int c = 0;
				if (!el.effects.isEmpty()) {
					c = net.minecraft.server.v1_8_R3.PotionBrewer.a(el.effects.values());
				}
				dw.watch(7, Integer.valueOf(c));

			}, duration);
		}
	}

	@Override
	public void playEntityAnimation(final Location location, final EntityType entityType, final int animationId,
			boolean instant) {
		final EntityLiving entity;
		if (entityType == EntityType.VILLAGER) {
			entity = new EntityVillager(((CraftWorld) location.getWorld()).getHandle());
		} else if (entityType == EntityType.WITCH) {
			entity = new EntityWitch(((CraftWorld) location.getWorld()).getHandle());
		} else if (entityType == EntityType.OCELOT) {
			entity = new EntityOcelot(((CraftWorld) location.getWorld()).getHandle());
		} else {
			entity = null;
		}
		if (entity == null)
			return;

		entity.setPosition(location.getX(), instant ? location.getY() : -5, location.getZ());
		((CraftWorld) location.getWorld()).getHandle().addEntity(entity);
		entity.addEffect(new MobEffect(14, 40));
		if (instant) {
			((CraftWorld) location.getWorld()).getHandle().broadcastEntityEffect(entity, (byte) animationId);
			entity.getBukkitEntity().remove();
		} else {
			entity.setPosition(location.getX(), location.getY(), location.getZ());
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					((CraftWorld) location.getWorld()).getHandle().broadcastEntityEffect(entity, (byte) animationId);
					entity.getBukkitEntity().remove();
				}
			}, 8);
		}
	}

	@Override
	public DisguiseManager getDisguiseManager(MagicConfig config) {
		if (CompatBasics.pluginEnabled("ProtocolLib"))
			return new DisguiseManager_1_8_R3(config);
		return new DisguiseManagerEmpty(config);
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
		EntityLiving nmsEnt = ((CraftLivingEntity) entity).getHandle();
		IAttribute attr = null;
		switch (attribute) {
		case "generic.maxHealth":
			attr = GenericAttributes.maxHealth;
			break;
		case "generic.followRange":
			attr = GenericAttributes.FOLLOW_RANGE;
			break;
		case "generic.knockbackResistance":
			attr = GenericAttributes.c;
			break;
		case "generic.movementSpeed":
			attr = GenericAttributes.MOVEMENT_SPEED;
			break;
		case "generic.attackDamage":
			attr = GenericAttributes.ATTACK_DAMAGE;
			break;
		}

		if (attr != null) {
			AttributeInstance attributes = nmsEnt.getAttributeInstance(attr);
			attributes.b(new AttributeModifier("MagicSpells " + attribute, amount, operation));
		}
	}

	@Override
	public void resetEntityAttributes(LivingEntity entity) {
		try {
			EntityLiving e = ((CraftLivingEntity) entity).getHandle();
			Field field = EntityLiving.class.getDeclaredField("c");
			field.setAccessible(true);
			field.set(e, null);
			e.getAttributeMap();
			Method method = null;
			Class<?> clazz = e.getClass();
			while (clazz != null) {
				try {
					method = clazz.getDeclaredMethod("aW");
					break;
				} catch (NoSuchMethodException e1) {
					clazz = clazz.getSuperclass();
				}
			}
			if (method != null) {
				method.setAccessible(true);
				method.invoke(e);
			} else {
				throw new Exception("No method aW found on " + e.getClass().getName());
			}
		} catch (Exception e) {
			MagicSpells.handleException(e);
		}
	}

	@Override
	public void removeAI(LivingEntity entity) {
		try {
			EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();

			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {
		try {
			EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();

			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, range, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setBossBar(Player player, String title, double percent) {
		updateBossBarEntity(player, title, percent);

		PacketPlayOutEntityDestroy packetDestroy = new PacketPlayOutEntityDestroy(this.bossBarEntity.getId());
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetDestroy);

		PacketPlayOutSpawnEntityLiving packetSpawn = new PacketPlayOutSpawnEntityLiving(this.bossBarEntity);
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSpawn);

		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(this.bossBarEntity);
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetTeleport);

		// PacketPlayOutEntityVelocity packetVelocity = new
		// PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);
		// ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}

	@Override
	public void updateBossBar(Player player, String title, double percent) {
		updateBossBarEntity(player, title, percent);

		if (title != null) {
			PacketPlayOutEntityMetadata packetData = new PacketPlayOutEntityMetadata(this.bossBarEntity.getId(),
					this.bossBarEntity.getDataWatcher(), true);
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetData);
		}

		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(this.bossBarEntity);
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetTeleport);

		// PacketPlayOutEntityVelocity packetVelocity = new
		// PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);
		// ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}

	private void updateBossBarEntity(Player player, String title, double percent) {
		if (title != null) {
			if (percent <= 0.01)
				percent = 0.01D;
			this.bossBarEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', title));
			this.bossBarEntity.getDataWatcher().watch(6, (float) (percent * 300f));
		}

		Location l = player.getLocation();
		l.setPitch(l.getPitch() + 10);
		Vector v = l.getDirection().multiply(20);
		Util.rotateVector(v, 15);
		l.add(v);
		this.bossBarEntity.setLocation(l.getX(), l.getY(), l.getZ(), 0, 0);
	}

	@Override
	public void removeBossBar(Player player) {
		PacketPlayOutEntityDestroy packetDestroy = new PacketPlayOutEntityDestroy(bossBarEntity.getId());
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetDestroy);
	}
}
