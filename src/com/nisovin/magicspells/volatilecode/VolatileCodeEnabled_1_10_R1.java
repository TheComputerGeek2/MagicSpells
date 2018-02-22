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
import java.util.Set;
import java.util.UUID;

import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.util.compat.EventUtil;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.EntityEnderDragon;
import net.minecraft.server.v1_10_R1.EntityFallingBlock;
import net.minecraft.server.v1_10_R1.EntityFireworks;
import net.minecraft.server.v1_10_R1.EntityHuman;
import net.minecraft.server.v1_10_R1.EntityInsentient;
import net.minecraft.server.v1_10_R1.EntityLiving;
import net.minecraft.server.v1_10_R1.EntitySmallFireball;
import net.minecraft.server.v1_10_R1.EntityTNTPrimed;
import net.minecraft.server.v1_10_R1.EnumParticle;
import net.minecraft.server.v1_10_R1.Item;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.NBTTagList;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import net.minecraft.server.v1_10_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_10_R1.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_10_R1.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_10_R1.PacketPlayOutExperience;
import net.minecraft.server.v1_10_R1.PacketPlayOutExplosion;
import net.minecraft.server.v1_10_R1.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_10_R1.PacketPlayOutSetCooldown;
import net.minecraft.server.v1_10_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_10_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_10_R1.PacketPlayOutTitle;
import net.minecraft.server.v1_10_R1.PacketPlayOutTitle.EnumTitleAction;

import net.minecraft.server.v1_10_R1.PacketPlayOutWorldParticles;
import net.minecraft.server.v1_10_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_10_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_10_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_10_R1.PlayerConnection;
import net.minecraft.server.v1_10_R1.EntityCreature;
import net.minecraft.server.v1_10_R1.PathEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_10_R1.CraftServer;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public class VolatileCodeEnabled_1_10_R1 implements VolatileCodeHandle {

	VolatileCodeDisabled fallback = new VolatileCodeDisabled();
	
	private Field craftItemStackHandleField = null;
	private Field entityFallingBlockHurtEntitiesField = null;
	private Field entityFallingBlockFallHurtAmountField = null;
	private Field entityFallingBlockFallHurtMaxField = null;
	private Class<?> craftMetaSkullClass = null;
	private Field craftMetaSkullProfileField = null;
	
	public VolatileCodeEnabled_1_10_R1(MagicConfig config) {
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
			
			this.craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
			this.craftItemStackHandleField.setAccessible(true);
			
			this.entityFallingBlockHurtEntitiesField = EntityFallingBlock.class.getDeclaredField("hurtEntities");
			this.entityFallingBlockHurtEntitiesField.setAccessible(true);
			
			this.entityFallingBlockFallHurtAmountField = EntityFallingBlock.class.getDeclaredField("fallHurtAmount");
			this.entityFallingBlockFallHurtAmountField.setAccessible(true);
			
			this.entityFallingBlockFallHurtMaxField = EntityFallingBlock.class.getDeclaredField("fallHurtMax");
			this.entityFallingBlockFallHurtMaxField.setAccessible(true);
			
			this.craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.v1_10_R1.inventory.CraftMetaSkull");
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
	}
	
	private NBTTagCompound getTag(ItemStack item) {
		if (item instanceof CraftItemStack) {
			try {
				return ((net.minecraft.server.v1_10_R1.ItemStack)this.craftItemStackHandleField.get(item)).getTag();
			} catch (Exception e) {
				// No op currently
			}
		}
		return null;
	}
	
	private ItemStack setTag(ItemStack item, NBTTagCompound tag) {
		CraftItemStack craftItem;
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
		} else {
			craftItem = CraftItemStack.asCraftCopy(item);
		}
		
		net.minecraft.server.v1_10_R1.ItemStack nmsItem = null;
		try {
			nmsItem = (net.minecraft.server.v1_10_R1.ItemStack)this.craftItemStackHandleField.get(item);
		} catch (Exception e) {
			// No op currently
		}
		if (nmsItem == null) {
			nmsItem = CraftItemStack.asNMSCopy(craftItem);
		}
		
		if (nmsItem != null) {
			nmsItem.setTag(tag);
			try {
				this.craftItemStackHandleField.set(craftItem, nmsItem);
			} catch (Exception e) {
				// No op currently
			}
		}
		
		return craftItem;
	}
	
	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		/*final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(7, Integer.valueOf(color));
		
		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					int c = 0;
					if (!el.effects.isEmpty()) {
						c = net.minecraft.server.v1_10_R1.PotionBrewer.a(el.effects.values());
					}
					dw.watch(7, Integer.valueOf(c));
				}
			}, duration);
		}*/
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
		net.minecraft.server.v1_10_R1.ItemStack nmsItem;
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
		this.fallback.toggleLeverOrButton(block);
		//net.minecraft.server.v1_10_R1.Block.getById(block.getType().getId()).interact(((CraftWorld)block.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()), null, 0, 0, 0, 0);
	}

	@Override
	public void pressPressurePlate(Block block) {
		this.fallback.pressPressurePlate(block);
		//block.setData((byte) (block.getData() ^ 0x1));
		//net.minecraft.server.v1_10_R1.World w = ((CraftWorld)block.getWorld()).getHandle();
		//w.applyPhysics(block.getX(), block.getY(), block.getZ(), net.minecraft.server.v1_10_R1.Block.getById(block.getType().getId()));
		//w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), net.minecraft.server.v1_10_R1.Block.getById(block.getType().getId()));
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
		PacketPlayOutExplosion packet = new PacketPlayOutExplosion(location.getX(), location.getY(), location.getZ(), size, new ArrayList<>(), null);
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
		net.minecraft.server.v1_10_R1.World w = ((CraftWorld)player.getWorld()).getHandle();
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
			((EntityInsentient)((CraftLivingEntity)entity).getHandle()).setGoalTarget(((CraftLivingEntity)target).getHandle(), TargetReason.CUSTOM, true);
		}
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		for (Player player : location.getWorld().getPlayers()) {
			playSound(player, location, sound, volume, pitch);
		}
	}

	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		playSound(player, player.getLocation(), sound, volume, pitch);
	}
	
	private void playSound(Player player, Location loc, String sound, float volume, float pitch) {
		player.playSound(loc, sound, volume, pitch);
		//PacketPlayOutCustomSoundEffect packet = new PacketPlayOutCustomSoundEffect(sound, SoundCategory.MASTER, loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
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
			this.entityFallingBlockHurtEntitiesField.setBoolean(efb, true);
			this.entityFallingBlockFallHurtAmountField.setFloat(efb, damage);
			this.entityFallingBlockFallHurtMaxField.setInt(efb, max);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void playEntityAnimation(final Location location, final EntityType entityType, final int animationId, boolean instant) {
		/*final EntityLiving entity;
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
				public void run() {
					((CraftWorld)location.getWorld()).getHandle().broadcastEntityEffect(entity, (byte)animationId);
					entity.getBukkitEntity().remove();
				}
			}, 8);
		}*/
	}

	@Override
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration) {
		// Create item
		net.minecraft.server.v1_10_R1.ItemStack item = new net.minecraft.server.v1_10_R1.ItemStack(Item.getById(401), 1, 0);
		
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
		//location.getWorld().spawnParticle(null, location.getX(), location.getY() + yOffset, location.getZ(), count, spreadX, spreadY, spreadZ, speed);
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
				this.packet63Fields[10].set(packet, data);
			}
			int rSq = radius * radius;
			
			for (Player player : location.getWorld().getPlayers()) {
				if (player.getLocation().distanceSquared(location) <= rSq) {
					((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
				} else {
					// No op yet
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
	public IDisguiseManager getDisguiseManager(MagicConfig config) {
		if (CompatBasics.pluginEnabled("LibsDisguises")) {
			try {
				return new DisguiseManagerLibsDisguises(config);
			} catch (Exception e) {
				return new DisguiseManagerEmpty(config);
			}
		}
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
		NBTTagCompound attr = new NBTTagCompound();
		attr.setString("Name", name);
		attr.setString("AttributeName", attributeName);
		attr.setDouble("Amount", amount);
		attr.setInt("Operation", operation);
		attr.setLong("UUIDLeast", uuid.getLeastSignificantBits());
		attr.setLong("UUIDMost", uuid.getMostSignificantBits());
		if (slot != null) attr.setString("Slot", slot);
		return attr;
	}
	
	@Override
	public ItemStack hideTooltipCrap(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
		meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
		Attribute attr = null;
		switch (attribute) {
			case "generic.maxHealth":
				attr = Attribute.GENERIC_MAX_HEALTH;
				break;
			case "generic.followRange":
				attr = Attribute.GENERIC_MAX_HEALTH;
				break;
			case "generic.knockbackResistance":
				attr = Attribute.GENERIC_KNOCKBACK_RESISTANCE;
				break;
			case "generic.movementSpeed":
				attr = Attribute.GENERIC_MOVEMENT_SPEED;
				break;
			case "generic.attackDamage":
				attr = Attribute.GENERIC_ATTACK_DAMAGE;
				break;
			case "generic.attackSpeed":
				attr = Attribute.GENERIC_ATTACK_SPEED;
				break;
			case "generic.armor":
				attr = Attribute.GENERIC_ARMOR;
				break;
			case "generic.luck":
				attr = Attribute.GENERIC_LUCK;
				break;
		}
		Operation oper = null;
		if (operation == 0) {
			oper = Operation.ADD_NUMBER;
		} else if (operation == 1) {
			oper = Operation.MULTIPLY_SCALAR_1;
		} else if (operation == 2) {
			oper = Operation.ADD_SCALAR;
		}
		if (attr != null && oper != null) entity.getAttribute(attr).addModifier(new AttributeModifier("MagicSpells " + attribute, amount, oper));
	}

	@Override
	public void resetEntityAttributes(LivingEntity entity) {
		try {
			EntityLiving e = ((CraftLivingEntity)entity).getHandle();
			Field field = EntityLiving.class.getDeclaredField("bp");
			field.setAccessible(true);
			field.set(e, null);
			e.getAttributeMap();
			Method method = null;
			Class<?> clazz = e.getClass();
			while (clazz != null) {
				try {
					method = clazz.getDeclaredMethod("initAttributes");
					break;
				} catch (NoSuchMethodException e1) {
				    clazz = clazz.getSuperclass();
				}
			}
			if (method != null) {
				method.setAccessible(true);
				method.invoke(e);
			} else {
				throw new Exception("No method initAttributes found on " + e.getClass().getName());
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
            Set list = (Set)listField.get(goals);
            list.clear();
            listField = PathfinderGoalSelector.class.getDeclaredField("c");
            listField.setAccessible(true);
            list = (Set)listField.get(goals);
            list.clear();

            goals.a(0, new PathfinderGoalFloat(ev));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
		/*updateBossBarEntity(player, title, percent);
		
		PacketPlayOutEntityDestroy packetDestroy = new PacketPlayOutEntityDestroy(bossBarEntity.getId());
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetDestroy);
		
		PacketPlayOutSpawnEntityLiving packetSpawn = new PacketPlayOutSpawnEntityLiving(bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetSpawn);
		
		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetTeleport);*/
		
		//PacketPlayOutEntityVelocity packetVelocity = new PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);		
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}
	
	@Override
	public void updateBossBar(Player player, String title, double percent) {
		/*updateBossBarEntity(player, title, percent);
		
		if (title != null) {
			PacketPlayOutEntityMetadata packetData = new PacketPlayOutEntityMetadata(bossBarEntity.getId(), bossBarEntity.getDataWatcher(), true);
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetData);
		}
		
		PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport(bossBarEntity);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetTeleport);*/
		
		//PacketPlayOutEntityVelocity packetVelocity = new PacketPlayOutEntityVelocity(bossBarEntity.getId(), 1, 0, 1);
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetVelocity);
	}
	
	/*private void updateBossBarEntity(Player player, String title, double percent) {
		if (title != null) {
			if (percent <= 0.01) percent = 0.01D;
			bossBarEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', title));
			bossBarEntity.getDataWatcher().watch(6, (float)(percent * 300f));
		}
		
		Location l = player.getLocation();
		l.setPitch(l.getPitch() + 10);
		Vector v = l.getDirection().multiply(20);
		Util.rotateVector(v, 15);
		l.add(v);
		bossBarEntity.setLocation(l.getX(), l.getY(), l.getZ(), 0, 0);
	}*/
	
	@Override
	public void removeBossBar(Player player) {
		//PacketPlayOutEntityDestroy packetDestroy = new PacketPlayOutEntityDestroy(bossBarEntity.getId());
		//((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetDestroy);
	}
	
	@Override
	public void saveSkinData(Player player, String name) {
		GameProfile profile = ((CraftPlayer)player).getHandle().getProfile();
		Collection<Property> props = profile.getProperties().get("textures");
		for (Property prop : props) {
			String skin = prop.getValue();
			String sig = prop.getSignature();
			
			File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
			if (!folder.exists()) {
				folder.mkdir();
			}
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
		ItemMeta meta = item.getItemMeta();
		meta.spigot().setUnbreakable(true);
		item.setItemMeta(meta);
		return item;
	}
		
	@Override
	public void setArrowsStuck(LivingEntity entity, int count) {
		//((CraftLivingEntity)entity).getHandle().set
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
	public void setNoAIFlag(LivingEntity entity) {
		// No op yet
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
		PacketPlayOutSetCooldown packet = new PacketPlayOutSetCooldown(Item.getById(item.getTypeId()), duration);
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}

	@Override
	public boolean hasGravity(Entity entity) {
		return entity.hasGravity();
	}

	@Override
	public void setGravity(Entity entity, boolean gravity) {
		entity.setGravity(gravity);
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
