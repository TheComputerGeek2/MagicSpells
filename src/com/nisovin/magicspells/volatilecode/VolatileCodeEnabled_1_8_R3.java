package com.nisovin.magicspells.volatilecode;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.util.compat.EventUtil;
import net.minecraft.server.v1_8_R3.AttributeInstance;
import net.minecraft.server.v1_8_R3.AttributeModifier;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityEnderDragon;
import net.minecraft.server.v1_8_R3.EntityFallingBlock;
import net.minecraft.server.v1_8_R3.EntityFireworks;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityOcelot;
import net.minecraft.server.v1_8_R3.EntitySmallFireball;
import net.minecraft.server.v1_8_R3.EntityTNTPrimed;
import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.EntityWitch;
import net.minecraft.server.v1_8_R3.EntityWither;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import net.minecraft.server.v1_8_R3.IAttribute;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.PacketPlayOutExperience;
import net.minecraft.server.v1_8_R3.PacketPlayOutExplosion;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutSetSlot;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;

import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import net.minecraft.server.v1_8_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.EntityCreature;
import net.minecraft.server.v1_8_R3.PathEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class VolatileCodeEnabled_1_8_R3 implements VolatileCodeHandle {

	EntityInsentient bossBarEntity;
	VolatileCodeDisabled fallback = new VolatileCodeDisabled();
	
	private Class<?> craftMetaSkullClass = null;
	private Field craftMetaSkullProfileField = null;
	
	private static NBTTagCompound getTag(ItemStack item) {
		if (item instanceof CraftItemStack) {
			try {
				Field field = CraftItemStack.class.getDeclaredField("handle");
				field.setAccessible(true);
				return ((net.minecraft.server.v1_8_R3.ItemStack)field.get(item)).getTag();
			} catch (Exception e) {
				DebugHandler.debugGeneral(e);
			}
		}
		return null;
	}
	
	private static ItemStack setTag(ItemStack item, NBTTagCompound tag) {
		CraftItemStack craftItem;
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
		} else {
			craftItem = CraftItemStack.asCraftCopy(item);
		}
		
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = null;
		try {
			Field field = CraftItemStack.class.getDeclaredField("handle");
			field.setAccessible(true);
			nmsItem = (net.minecraft.server.v1_8_R3.ItemStack)field.get(item);
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
		if (nmsItem == null) {
			nmsItem = CraftItemStack.asNMSCopy(craftItem);
		}
		
		if (nmsItem != null) {
			nmsItem.setTag(tag);
			try {
				Field field = CraftItemStack.class.getDeclaredField("handle");
				field.setAccessible(true);
				field.set(craftItem, nmsItem);
			} catch (Exception e) {
				DebugHandler.debugGeneral(e);
			}
		}
		
		return craftItem;
	}
	
	public VolatileCodeEnabled_1_8_R3() {
		try {
			this.packet63Fields[0] = PacketPlayOutWorldParticles.class.getDeclaredField("a");
			this.packet63Fields[1] = PacketPlayOutWorldParticles.class.getDeclaredField("b");
			this.packet63Fields[2] = PacketPlayOutWorldParticles.class.getDeclaredField("c");
			this.packet63Fields[3] = PacketPlayOutWorldParticles.class.getDeclaredField("d");
			this.packet63Fields[4] = PacketPlayOutWorldParticles.class.getDeclaredField("e");
			this.packet63Fields[5] = PacketPlayOutWorldParticles.class.getDeclaredField("f");
			this.packet63Fields[6] = PacketPlayOutWorldParticles.class.getDeclaredField("g");
			this.packet63Fields[7] = PacketPlayOutWorldParticles.class.getDeclaredField("h");
			this.packet63Fields[8] = PacketPlayOutWorldParticles.class.getDeclaredField("i");
			this.packet63Fields[9] = PacketPlayOutWorldParticles.class.getDeclaredField("j");
			this.packet63Fields[10] = PacketPlayOutWorldParticles.class.getDeclaredField("k");
			AccessibleObject.setAccessible(this.packet63Fields, true);
			
			this.craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaSkull");
			this.craftMetaSkullProfileField = this.craftMetaSkullClass.getDeclaredField("profile");
			this.craftMetaSkullProfileField.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (EnumParticle particle : EnumParticle.values()) {
			if (particle != null) {
				this.particleMap.put(particle.b(), particle);
			}
		}
		
		this.bossBarEntity = new EntityWither(((CraftWorld)Bukkit.getWorlds().get(0)).getHandle());
		this.bossBarEntity.setCustomNameVisible(false);
		this.bossBarEntity.getDataWatcher().watch(0, (byte)0x20);
		this.bossBarEntity.getDataWatcher().watch(20, (Integer)0);
		
	}
	
	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(7, Integer.valueOf(color));
		
		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					int c = 0;
					if (!el.effects.isEmpty()) {
						c = net.minecraft.server.v1_8_R3.PotionBrewer.a(el.effects.values());
					}
					dw.watch(7, Integer.valueOf(c));
				}
			}, duration);
		}
	}

	@Override
	public void entityPathTo(LivingEntity creature, LivingEntity target) {
		//EntityCreature entity = ((CraftCreature)creature).getHandle();
		//entity.pathEntity = entity.world.findPath(entity, ((CraftLivingEntity)target).getHandle(), 16.0F, true, false, false, false);
	}

	@Override
	public void creaturePathToLoc(Creature creature, Location loc, float speed) {
		EntityCreature entity = ((CraftCreature)creature).getHandle();
		PathEntity pathEntity = entity.getNavigation().a(loc.getX(), loc.getY(), loc.getZ());
		entity.getNavigation().a(pathEntity, speed);
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		net.minecraft.server.v1_8_R3.ItemStack nmsItem;
		if (item != null) {
			nmsItem = CraftItemStack.asNMSCopy(item);
		} else {
			nmsItem = null;
		}
		PacketPlayOutSetSlot packet = new PacketPlayOutSetSlot(0, (short)slot + 36, nmsItem);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}

	@Override
	public void toggleLeverOrButton(Block block) {
		// TODO: fix this
		this.fallback.toggleLeverOrButton(block);
		//net.minecraft.server.v1_8_R3.Block.getById(block.getType().getId()).interact(((CraftWorld)block.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()), null, 0, 0, 0, 0);
	}

	@Override
	public void pressPressurePlate(Block block) {
		this.fallback.pressPressurePlate(block);
		// TODO: fix this
		//block.setData((byte) (block.getData() ^ 0x1));
		//net.minecraft.server.v1_8_R3.World w = ((CraftWorld)block.getWorld()).getHandle();
		//w.applyPhysics(block.getX(), block.getY(), block.getZ(), net.minecraft.server.v1_8_R3.Block.getById(block.getType().getId()));
		//w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), net.minecraft.server.v1_8_R3.Block.getById(block.getType().getId()));
	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
        EntityTNTPrimed e = new EntityTNTPrimed(((CraftWorld)target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ(), ((CraftLivingEntity)source).getHandle());
        CraftTNTPrimed c = new CraftTNTPrimed((CraftServer)Bukkit.getServer(), e);
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(c, explosionSize, fire);
		EventUtil.call(event);
        return event.isCancelled();
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks) {
		return !((CraftWorld)location.getWorld()).getHandle().createExplosion(((CraftPlayer)player).getHandle(), location.getX(), location.getY(), location.getZ(), size, fire, breakBlocks).wasCanceled;
	}

	@Override
	public void playExplosionEffect(Location location, float size) {
		PacketPlayOutExplosion packet = new PacketPlayOutExplosion(location.getX(), location.getY(), location.getZ(), size, new ArrayList(), null);
		for (Player player : location.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(location) < 50 * 50) {
				((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
			}
		}
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
		PacketPlayOutExperience packet = new PacketPlayOutExperience(percent, player.getTotalExperience(), level);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}

	@Override
	public Fireball shootSmallFireball(Player player) {
		net.minecraft.server.v1_8_R3.World w = ((CraftWorld)player.getWorld()).getHandle();
		Location playerLoc = player.getLocation();
		Vector loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(10));
		
		double d0 = loc.getX() - playerLoc.getX();
        double d1 = loc.getY() - (playerLoc.getY() + 1.5);
        double d2 = loc.getZ() - playerLoc.getZ();
		EntitySmallFireball entitysmallfireball = new EntitySmallFireball(w, ((CraftPlayer)player).getHandle(), d0, d1, d2);

        entitysmallfireball.locY = playerLoc.getY() + 1.5;
        w.addEntity(entitysmallfireball);
        
        return (Fireball)entitysmallfireball.getBukkitEntity();
	}

	@Override
	public void setTarget(LivingEntity entity, LivingEntity target) {
		if (entity instanceof Creature) {
			((Creature)entity).setTarget(target);
		} else {
			((EntityInsentient)((CraftLivingEntity)entity).getHandle()).setGoalTarget(((CraftLivingEntity)target).getHandle());
		}
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		((CraftWorld)location.getWorld()).getHandle().makeSound(location.getX(), location.getY(), location.getZ(), sound, volume, pitch);
	}

	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		Location loc = player.getLocation();
		PacketPlayOutNamedSoundEffect packet = new PacketPlayOutNamedSoundEffect(sound, loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}

	@Override
	public ItemStack addFakeEnchantment(ItemStack item) {
		if (!(item instanceof CraftItemStack)) item = CraftItemStack.asCraftCopy(item);
		NBTTagCompound tag = getTag(item);		
		if (tag == null) tag = new NBTTagCompound();
		if (!tag.hasKey("ench")) tag.set("ench", new NBTTagList());
		return setTag(item, tag);
	}

	@Override
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
		EntityFallingBlock efb = ((CraftFallingSand)block).getHandle();
		try {
			Field field = EntityFallingBlock.class.getDeclaredField("hurtEntities");
			field.setAccessible(true);
			field.setBoolean(efb, true);
			
			field = EntityFallingBlock.class.getDeclaredField("fallHurtAmount");
			field.setAccessible(true);
			field.setFloat(efb, damage);
			
			field = EntityFallingBlock.class.getDeclaredField("fallHurtMax");
			field.setAccessible(true);
			field.setInt(efb, max);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void playEntityAnimation(final Location location, final EntityType entityType, final int animationId, boolean instant) {
		final EntityLiving entity;
		if (entityType == EntityType.VILLAGER) {
			entity = new EntityVillager(((CraftWorld)location.getWorld()).getHandle());
		} else if (entityType == EntityType.WITCH) {
			entity = new EntityWitch(((CraftWorld)location.getWorld()).getHandle());
		} else if (entityType == EntityType.OCELOT) {
			entity = new EntityOcelot(((CraftWorld)location.getWorld()).getHandle());
		} else {
			entity = null;
		}
		if (entity == null) return;
		
		entity.setPosition(location.getX(), instant ? location.getY() : -5, location.getZ());
		((CraftWorld)location.getWorld()).getHandle().addEntity(entity);
		entity.addEffect(new MobEffect(14, 40));
		if (instant) {
			((CraftWorld)location.getWorld()).getHandle().broadcastEntityEffect(entity, (byte)animationId);
			entity.getBukkitEntity().remove();
		} else {
			entity.setPosition(location.getX(), location.getY(), location.getZ());
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					((CraftWorld)location.getWorld()).getHandle().broadcastEntityEffect(entity, (byte)animationId);
					entity.getBukkitEntity().remove();
				}
			}, 8);
		}
	}

	@Override
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration) {
		// Create item
		net.minecraft.server.v1_8_R3.ItemStack item = new net.minecraft.server.v1_8_R3.ItemStack(Item.getById(401), 1, 0);
		
		// Get tag
		NBTTagCompound tag = item.getTag();
		if (tag == null) tag = new NBTTagCompound();
		
		// Create explosion tag
		NBTTagCompound explTag = new NBTTagCompound();
		explTag.setByte("Flicker", flicker ? (byte)1 : (byte)0);
		explTag.setByte("Trail", trail ? (byte)1 : (byte)0);
		explTag.setByte("Type", (byte)type);
		explTag.setIntArray("Colors", colors);
		explTag.setIntArray("FadeColors", fadeColors);
		
		// Create fireworks tag
		NBTTagCompound fwTag = new NBTTagCompound();
		fwTag.setByte("Flight", (byte)flightDuration);
		NBTTagList explList = new NBTTagList();
		explList.add(explTag);
		fwTag.set("Explosions", explList);
		tag.set("Fireworks", fwTag);
		
		// Set tag
		item.setTag(tag);
		
		// Create fireworks entity
		EntityFireworks fireworks = new EntityFireworks(((CraftWorld)location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), item);
		((CraftWorld)location.getWorld()).getHandle().addEntity(fireworks);
		
		// Cause explosion
		if (flightDuration == 0) {
			((CraftWorld)location.getWorld()).getHandle().broadcastEntityEffect(fireworks, (byte)17);
			fireworks.die();
		}
	}
	
	Field[] packet63Fields = new Field[11];
	Map<String, EnumParticle> particleMap = new HashMap<>();
	
	@Override
	public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset) {
		playParticleEffect(location, name, spreadHoriz, spreadVert, spreadHoriz, speed, count, radius, yOffset);
	}
	
	@Override
	public void playParticleEffect(Location location, String name, float spreadX, float spreadY, float spreadZ, float speed, int count, int radius, float yOffset) {
		PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles();
		EnumParticle particle = this.particleMap.get(name);
		int[] data = null;
		if (name.contains("_")) {
			String[] split = name.split("_");
			name = split[0] + '_';
			particle = this.particleMap.get(name);
			if (split.length > 1) {
				String[] split2 = split[1].split(":");
				data = new int[split2.length];
				for (int i = 0; i < data.length; i++) {
					data[i] = Integer.parseInt(split2[i]);
				}
			}
		}
		if (particle == null) {
			MagicSpells.error("Invalid particle: " + name);
			return;
		}
		try {
			this.packet63Fields[0].set(packet, particle);
			this.packet63Fields[1].setFloat(packet, (float)location.getX());
			this.packet63Fields[2].setFloat(packet, (float)location.getY() + yOffset);
			this.packet63Fields[3].setFloat(packet, (float)location.getZ());
			this.packet63Fields[4].setFloat(packet, spreadX);
			this.packet63Fields[5].setFloat(packet, spreadY);
			this.packet63Fields[6].setFloat(packet, spreadZ);
			this.packet63Fields[7].setFloat(packet, speed);
			this.packet63Fields[8].setInt(packet, count);
			this.packet63Fields[9].setBoolean(packet, radius >= 30);
			if (data != null) {
				this.packet63Fields[10].set(packet,data);
			}
			int rSq = radius * radius;
			
			for (Player player : location.getWorld().getPlayers()) {
				if (player.getLocation().distanceSquared(location) <= rSq) {
					((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
				} else {
					//TODO
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void playDragonDeathEffect(Location location) {
		EntityEnderDragon dragon = new EntityEnderDragon(((CraftWorld)location.getWorld()).getHandle());
		dragon.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), 0F);
		
		PacketPlayOutSpawnEntityLiving packet24 = new PacketPlayOutSpawnEntityLiving(dragon);
		PacketPlayOutEntityStatus packet38 = new PacketPlayOutEntityStatus(dragon, (byte)3);
		final PacketPlayOutEntityDestroy packet29 = new PacketPlayOutEntityDestroy(dragon.getBukkitEntity().getEntityId());
		
		BoundingBox box = new BoundingBox(location, 64);
		final List<Player> players = new ArrayList<>();
		for (Player player : location.getWorld().getPlayers()) {
			if (box.contains(player)) {
				players.add(player);
				((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet24);
				((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet38);
			}
		}
		
		MagicSpells.scheduleDelayedTask(new Runnable() {
			@Override
			public void run() {
				for (Player player : players) {
					if (player.isValid()) {
						((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet29);
					}
				}
			}
		}, 250);
	}
	
	@Override
	public void setKiller(LivingEntity entity, Player killer) {
		((CraftLivingEntity)entity).getHandle().killer = ((CraftPlayer)killer).getHandle();
	}
	
	@Override
	public DisguiseManager getDisguiseManager(MagicConfig config) {
		if (CompatBasics.pluginEnabled("ProtocolLib")) return new DisguiseManager_1_8_R3(config);
		return new DisguiseManagerEmpty(config);
	}

	@Override
	public ItemStack addAttributes(ItemStack item, String[] names, String[] types, double[] amounts, int[] operations, String[] slots) {
		if (!(item instanceof CraftItemStack)) item = CraftItemStack.asCraftCopy(item);
		NBTTagCompound tag = getTag(item);
		
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < names.length; i++) {
			if (names[i] != null) {
				UUID uuid = UUID.randomUUID();
				NBTTagCompound attr = buildAttributeTag(names[i], types[i], amounts[i], operations[i], uuid, slots[i]);
				list.add(attr);
			}
		}
		
		tag.set("AttributeModifiers", list);
		
		setTag(item, tag);
		return item;
	}
	
	private NBTTagCompound buildAttributeTag(String name, String attributeName, double amount, int operation, UUID uuid, String slot) {
		NBTTagCompound tag = new NBTTagCompound();
		
		tag.setString("Name", name);
		tag.setString("AttributeName", attributeName);
		tag.setDouble("Amount", amount);
		tag.setInt("Operation", operation);
		tag.setLong("UUIDLeast", uuid.getLeastSignificantBits());
		tag.setLong("UUIDMost", uuid.getMostSignificantBits());
		if (slot != null) tag.setString("Slot", slot);
		
		return tag;
	}
	
	@Override
	public ItemStack hideTooltipCrap(ItemStack item) {
		if (!(item instanceof CraftItemStack)) item = CraftItemStack.asCraftCopy(item);
		NBTTagCompound tag = getTag(item);
		if (tag == null) tag = new NBTTagCompound();
		tag.setInt("HideFlags", 63);
		setTag(item, tag);
		return item;
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
		EntityLiving nmsEnt = ((CraftLivingEntity)entity).getHandle();
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
			EntityLiving e = ((CraftLivingEntity)entity).getHandle();
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
        	EntityInsentient ev = (EntityInsentient)((CraftLivingEntity)entity).getHandle();
               
            Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
            goalsField.setAccessible(true);
            PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
           
            Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
            listField.setAccessible(true);
            List list = (List)listField.get(goals);
            list.clear();
            listField = PathfinderGoalSelector.class.getDeclaredField("c");
            listField.setAccessible(true);
            list = (List)listField.get(goals);
            list.clear();

            goals.a(0, new PathfinderGoalFloat(ev));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	@Override
	public void setNoAIFlag(LivingEntity entity) {
		((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(15, Byte.valueOf((byte)1));
		((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(4, Byte.valueOf((byte)1));
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {
        try {
        	EntityInsentient ev = (EntityInsentient)((CraftLivingEntity)entity).getHandle();
               
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
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetDestroy);
		
		PacketPlayOutSpawnEntityLiving packetSpawn = new PacketPlayOutSpawnEntityLiving(this.bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetSpawn);
		
		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(this.bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetTeleport);
		
		//PacketPlayOutEntityVelocity packetVelocity = new PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);		
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}
	
	@Override
	public void updateBossBar(Player player, String title, double percent) {
		updateBossBarEntity(player, title, percent);
		
		if (title != null) {
			PacketPlayOutEntityMetadata packetData = new PacketPlayOutEntityMetadata(this.bossBarEntity.getId(), this.bossBarEntity.getDataWatcher(), true);
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetData);
		}
		
		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(this.bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetTeleport);
		
		//PacketPlayOutEntityVelocity packetVelocity = new PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}
	
	private void updateBossBarEntity(Player player, String title, double percent) {
		if (title != null) {
			if (percent <= 0.01) percent = 0.01D;
			this.bossBarEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', title));
			this.bossBarEntity.getDataWatcher().watch(6, (float)(percent * 300f));
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
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetDestroy);
	}
	
	@Override
	public void saveSkinData(Player player, String name) {
		GameProfile profile = ((CraftPlayer)player).getHandle().getProfile();
		Collection<Property> props = profile.getProperties().get("textures");
		for (Property prop : props) {
			String skin = prop.getValue();
			String sig = prop.getSignature();
			
			File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
			if (!folder.exists()) folder.mkdir();
			File skinFile = new File(folder, name + ".skin.txt");
			File sigFile = new File(folder, name + ".sig.txt");
			try {
				FileWriter writer = new FileWriter(skinFile);
				writer.write(skin);
				writer.flush();
				writer.close();
				writer = new FileWriter(sigFile);
				writer.write(sig);
				writer.flush();
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			break;
		}
	}

	@Override
	public ItemStack setUnbreakable(ItemStack item) {
		if (!(item instanceof CraftItemStack)) item = CraftItemStack.asCraftCopy(item);
		NBTTagCompound tag = getTag(item);
		if (tag == null) tag = new NBTTagCompound();
		tag.setByte("Unbreakable", (byte)1);
		return setTag(item, tag);
	}
		
	@Override
	public void setArrowsStuck(LivingEntity entity, int count) {
		// TODO: fix this
		//((CraftLivingEntity)entity).getHandle().a(count);
	}

	@Override
	public void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		PlayerConnection conn = ((CraftPlayer)player).getHandle().playerConnection;
		PacketPlayOutTitle packet = new PacketPlayOutTitle(EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
		conn.sendPacket(packet);
		if (title != null) {
			packet = new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(title));
			conn.sendPacket(packet);
		}
		if (subtitle != null) {
			packet = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle));
			conn.sendPacket(packet);
		}
	}

	@Override
	public void sendActionBarMessage(Player player, String message) {
		PlayerConnection conn = ((CraftPlayer)player).getHandle().playerConnection;
		PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(message), (byte)2);
		conn.sendPacket(packet);
	}

	@Override
	public void setTabMenuHeaderFooter(Player player, String header, String footer) {
		PlayerConnection conn = ((CraftPlayer)player).getHandle().playerConnection;
		PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
		try {
			Field field1 = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("a");
			Field field2 = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b");
			field1.setAccessible(true);
			field1.set(packet, new ChatComponentText(header));
			field2.setAccessible(true);
			field2.set(packet, new ChatComponentText(footer));
			conn.sendPacket(packet);
		} catch (Exception e) {
			MagicSpells.handleException(e);
		}
	}
	
	@Override
	public void setClientVelocity(Player player, Vector velocity) {
		PacketPlayOutEntityVelocity packet = new PacketPlayOutEntityVelocity(player.getEntityId(), velocity.getX(), velocity.getY(), velocity.getZ());
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}
	
	@Override
	public double getAbsorptionHearts(LivingEntity entity) {
		return ((CraftLivingEntity)entity).getHandle().getAbsorptionHearts();
	}

	@Override
	public void showItemCooldown(Player player, ItemStack item, int duration) {
		// No op
	}

	@Override
	public boolean hasGravity(Entity entity) {
		// Doesn't exist in this version of minecraft
		return false;
	}

	@Override
	public void setGravity(Entity entity, boolean gravity) {
		// Doesn't exist in this version of minecraft
	}

	@Override
	public void setTexture(SkullMeta meta, String texture, String signature) {
		try {
			GameProfile profile = (GameProfile) this.craftMetaSkullProfileField.get(meta);
			setTexture(profile, texture, signature);
			this.craftMetaSkullProfileField.set(meta, profile);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			MagicSpells.handleException(e);
		}
	}
	
	@Override
	public void setSkin(Player player, String skin, String signature) {
		CraftPlayer craftPlayer = (CraftPlayer)player;
		setTexture(craftPlayer.getProfile(), skin, signature);
	}
	
	private GameProfile setTexture(GameProfile profile, String texture, String signature) {
		if (signature == null || signature.isEmpty()) {
			profile.getProperties().put("textures", new Property("textures", texture));
		} else {
			profile.getProperties().put("textures", new Property("textures", texture, signature));
		}
		return profile;
	}
	
	@Override
	public void setTexture(SkullMeta meta, String texture, String signature, String uuid, String name) {
		try {
			GameProfile profile = new GameProfile(uuid != null ? UUID.fromString(uuid) : null, name);
			setTexture(profile, texture, signature);
			this.craftMetaSkullProfileField.set(meta, profile);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			MagicSpells.handleException(e);
		}
	}

}
