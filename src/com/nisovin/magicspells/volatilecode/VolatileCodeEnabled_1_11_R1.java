package com.nisovin.magicspells.volatilecode;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import net.minecraft.server.v1_11_R1.*;
import net.minecraft.server.v1_11_R1.Item;
import net.minecraft.server.v1_11_R1.PacketPlayOutTitle.EnumTitleAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftFallingBlock;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class VolatileCodeEnabled_1_11_R1 implements VolatileCodeHandle {
    
    VolatileCodeHandle fallback = new VolatileCodeDisabled();
    Field[] packet63Fields = new Field[11];
    Map<String, EnumParticle> particleMap = new HashMap<>();
    private Field craftItemStackHandleField;
    private Field entityFallingBlockHurtEntitiesField;
    private Field entityFallingBlockFallHurtAmountField;
    private Field entityFallingBlockFallHurtMaxField;
    @SuppressWarnings("FieldCanBeLocal")
    private Class<?> craftMetaSkullClass;
    private Field craftMetaSkullProfileField;
    
    public VolatileCodeEnabled_1_11_R1(final MagicConfig config) {
        try {
            packet63Fields[0] = PacketPlayOutWorldParticles.class.getDeclaredField("a");
            packet63Fields[1] = PacketPlayOutWorldParticles.class.getDeclaredField("b");
            packet63Fields[2] = PacketPlayOutWorldParticles.class.getDeclaredField("c");
            packet63Fields[3] = PacketPlayOutWorldParticles.class.getDeclaredField("d");
            packet63Fields[4] = PacketPlayOutWorldParticles.class.getDeclaredField("e");
            packet63Fields[5] = PacketPlayOutWorldParticles.class.getDeclaredField("f");
            packet63Fields[6] = PacketPlayOutWorldParticles.class.getDeclaredField("g");
            packet63Fields[7] = PacketPlayOutWorldParticles.class.getDeclaredField("h");
            packet63Fields[8] = PacketPlayOutWorldParticles.class.getDeclaredField("i");
            packet63Fields[9] = PacketPlayOutWorldParticles.class.getDeclaredField("j");
            packet63Fields[10] = PacketPlayOutWorldParticles.class.getDeclaredField("k");
            for(int i = 0; i <= 10; i++) {
                packet63Fields[i].setAccessible(true);
            }
            
            craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
            craftItemStackHandleField.setAccessible(true);
            
            entityFallingBlockHurtEntitiesField = EntityFallingBlock.class.getDeclaredField("hurtEntities");
            entityFallingBlockHurtEntitiesField.setAccessible(true);
            
            entityFallingBlockFallHurtAmountField = EntityFallingBlock.class.getDeclaredField("fallHurtAmount");
            entityFallingBlockFallHurtAmountField.setAccessible(true);
            
            entityFallingBlockFallHurtMaxField = EntityFallingBlock.class.getDeclaredField("fallHurtMax");
            entityFallingBlockFallHurtMaxField.setAccessible(true);
            
            craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.v1_11_R1.inventory.CraftMetaSkull");
            craftMetaSkullProfileField = craftMetaSkullClass.getDeclaredField("profile");
            craftMetaSkullProfileField.setAccessible(true);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        
        for(final EnumParticle particle : EnumParticle.values()) {
            if(particle != null) {
                particleMap.put(particle.b(), particle);
            }
        }
    }
    
    private NBTTagCompound getTag(final ItemStack item) {
        if(item instanceof CraftItemStack) {
            try {
                return ((net.minecraft.server.v1_11_R1.ItemStack) craftItemStackHandleField.get(item)).getTag();
            } catch(final Exception e) {
                //no op currently
            }
        }
        return null;
    }
    
    private ItemStack setTag(final ItemStack item, final NBTTagCompound tag) {
        final CraftItemStack craftItem;
        if(item instanceof CraftItemStack) {
            craftItem = (CraftItemStack) item;
        } else {
            craftItem = CraftItemStack.asCraftCopy(item);
        }
        
        net.minecraft.server.v1_11_R1.ItemStack nmsItem = null;
        try {
            nmsItem = (net.minecraft.server.v1_11_R1.ItemStack) craftItemStackHandleField.get(item);
        } catch(final Exception e) {
            //no op currently
        }
        if(nmsItem == null) {
            nmsItem = CraftItemStack.asNMSCopy(craftItem);
        }
        
        if(nmsItem != null) {
            nmsItem.setTag(tag);
            try {
                craftItemStackHandleField.set(craftItem, nmsItem);
            } catch(final Exception e) {
                //no op currently
            }
        }
        
        return craftItem;
    }
    
    @Override
    public void addPotionGraphicalEffect(final LivingEntity entity, final int color, final int duration) {
        /*final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
        final DataWatcher dw = el.getDataWatcher();
		dw.watch(7, Integer.valueOf(color));
		
		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					int c = 0;
					if (!el.effects.isEmpty()) {
						c = net.minecraft.server.v1_11_R1.PotionBrewer.a(el.effects.values());
					}
					dw.watch(7, Integer.valueOf(c));
				}
			}, duration);
		}*/
    }
    
    @Override
    public void entityPathTo(final LivingEntity creature, final LivingEntity target) {
        //EntityCreature entity = ((CraftCreature)creature).getHandle();
        //entity.pathEntity = entity.world.findPath(entity, ((CraftLivingEntity)target).getHandle(), 16.0F, true, false, false, false);
    }
    
    @Override
    public void sendFakeSlotUpdate(final Player player, final int slot, final ItemStack item) {
        final net.minecraft.server.v1_11_R1.ItemStack nmsItem;
        if(item != null) {
            nmsItem = CraftItemStack.asNMSCopy(item);
        } else {
            nmsItem = null;
        }
        final Packet packet = new PacketPlayOutSetSlot(0, (short) slot + 36, nmsItem);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
    
    @Override
    public void toggleLeverOrButton(final Block block) {
        fallback.toggleLeverOrButton(block);
        //net.minecraft.server.v1_11_R1.Block.getById(block.getType().getId()).interact(((CraftWorld)block.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()), null, 0, 0, 0, 0);
    }
    
    @Override
    public void pressPressurePlate(final Block block) {
        fallback.pressPressurePlate(block);
        //block.setData((byte) (block.getData() ^ 0x1));
        //net.minecraft.server.v1_11_R1.World w = ((CraftWorld)block.getWorld()).getHandle();
        //w.applyPhysics(block.getX(), block.getY(), block.getZ(), net.minecraft.server.v1_11_R1.Block.getById(block.getType().getId()));
        //w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), net.minecraft.server.v1_11_R1.Block.getById(block.getType().getId()));
    }
    
    @Override
    public boolean simulateTnt(final Location target, final LivingEntity source, final float explosionSize, final boolean fire) {
        final EntityTNTPrimed e = new EntityTNTPrimed(((CraftWorld) target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ(), ((CraftLivingEntity) source).getHandle());
        final CraftTNTPrimed c = new CraftTNTPrimed((CraftServer) Bukkit.getServer(), e);
        final ExplosionPrimeEvent event = new ExplosionPrimeEvent(c, explosionSize, fire);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }
    
    @Override
    public boolean createExplosionByPlayer(final Player player, final Location location, final float size, final boolean fire, final boolean breakBlocks) {
        return !((CraftWorld) location.getWorld()).getHandle().createExplosion(((CraftPlayer) player).getHandle(), location.getX(), location.getY(), location.getZ(), size, fire, breakBlocks).wasCanceled;
    }
    
    @Override
    public void playExplosionEffect(final Location location, final float size) {
        @SuppressWarnings({"rawtypes", "unchecked"}) final Packet packet = new PacketPlayOutExplosion(location.getX(), location.getY(), location.getZ(), size, new ArrayList(), null);
        for(final Player player : location.getWorld().getPlayers()) {
            if(player.getLocation().distanceSquared(location) < 50 * 50) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }
    
    @Override
    public void setExperienceBar(final Player player, final int level, final float percent) {
        final Packet packet = new PacketPlayOutExperience(percent, player.getTotalExperience(), level);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
    
    @Override
    public Fireball shootSmallFireball(final Player player) {
        final World w = ((CraftWorld) player.getWorld()).getHandle();
        final Location playerLoc = player.getLocation();
        final Vector loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(10));
        
        final double d0 = loc.getX() - playerLoc.getX();
        final double d1 = loc.getY() - (playerLoc.getY() + 1.5);
        final double d2 = loc.getZ() - playerLoc.getZ();
        final EntitySmallFireball entitysmallfireball = new EntitySmallFireball(w, ((CraftPlayer) player).getHandle(), d0, d1, d2);
        
        entitysmallfireball.locY = playerLoc.getY() + 1.5;
        w.addEntity(entitysmallfireball);
        
        return (Fireball) entitysmallfireball.getBukkitEntity();
    }
    
    @Override
    public void setTarget(final LivingEntity entity, final LivingEntity target) {
        if(entity instanceof Creature) {
            ((Creature) entity).setTarget(target);
        } else {
            ((EntityInsentient) ((CraftLivingEntity) entity).getHandle()).setGoalTarget(((CraftLivingEntity) target).getHandle(), TargetReason.CUSTOM, true);
        }
    }
    
    @Override
    public void playSound(final Location location, final String sound, final float volume, final float pitch) {
        for(final Player player : location.getWorld().getPlayers()) {
            playSound(player, location, sound, volume, pitch);
        }
    }
    
    @Override
    public void playSound(final Player player, final String sound, final float volume, final float pitch) {
        playSound(player, player.getLocation(), sound, volume, pitch);
    }
    
    private void playSound(final Player player, final Location loc, final String sound, final float volume, final float pitch) {
        player.playSound(loc, sound, volume, pitch);
        //PacketPlayOutCustomSoundEffect packet = new PacketPlayOutCustomSoundEffect(sound, SoundCategory.MASTER, loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
        //((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }
    
    @Override
    public ItemStack addFakeEnchantment(ItemStack item) {
        if(!(item instanceof CraftItemStack)) {
            item = CraftItemStack.asCraftCopy(item);
        }
        NBTTagCompound tag = getTag(item);
        if(tag == null) {
            tag = new NBTTagCompound();
        }
        if(!tag.hasKey("ench")) {
            tag.set("ench", new NBTTagList());
        }
        return setTag(item, tag);
    }
    
    @Override
    public void setFallingBlockHurtEntities(final FallingBlock block, final float damage, final int max) {
        final EntityFallingBlock efb = ((CraftFallingBlock) block).getHandle();
        try {
            final Field field = null;
            entityFallingBlockHurtEntitiesField.setBoolean(efb, true);
            
            entityFallingBlockFallHurtAmountField.setFloat(efb, damage);
            
            entityFallingBlockFallHurtMaxField.setInt(efb, max);
        } catch(final Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void playEntityAnimation(final Location location, final EntityType entityType, final int animationId, final boolean instant) {
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
    public void createFireworksExplosion(final Location location, final boolean flicker, final boolean trail, final int type, final int[] colors, final int[] fadeColors, final int flightDuration) {
        // create item
        final net.minecraft.server.v1_11_R1.ItemStack item = new net.minecraft.server.v1_11_R1.ItemStack(Item.getById(401), 1, 0);
        
        // get tag
        NBTTagCompound tag = item.getTag();
        if(tag == null) {
            tag = new NBTTagCompound();
        }
        
        // create explosion tag
        final NBTTagCompound explTag = new NBTTagCompound();
        explTag.setByte("Flicker", flicker ? (byte) 1 : (byte) 0);
        explTag.setByte("Trail", trail ? (byte) 1 : (byte) 0);
        explTag.setByte("Type", (byte) type);
        explTag.setIntArray("Colors", colors);
        explTag.setIntArray("FadeColors", fadeColors);
        
        // create fireworks tag
        final NBTTagCompound fwTag = new NBTTagCompound();
        fwTag.setByte("Flight", (byte) flightDuration);
        final NBTTagList explList = new NBTTagList();
        explList.add(explTag);
        fwTag.set("Explosions", explList);
        tag.set("Fireworks", fwTag);
        
        // set tag
        item.setTag(tag);
        
        // create fireworks entity
        final EntityFireworks fireworks = new EntityFireworks(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), item);
        ((CraftWorld) location.getWorld()).getHandle().addEntity(fireworks);
        
        // cause explosion
        if(flightDuration == 0) {
            ((CraftWorld) location.getWorld()).getHandle().broadcastEntityEffect(fireworks, (byte) 17);
            fireworks.die();
        }
    }
    
    @Override
    public void playParticleEffect(final Location location, final String name, final float spreadHoriz, final float spreadVert, final float speed, final int count, final int radius, final float yOffset) {
        playParticleEffect(location, name, spreadHoriz, spreadVert, spreadHoriz, speed, count, radius, yOffset);
    }
    
    @Override
    public void playParticleEffect(final Location location, String name, final float spreadX, final float spreadY, final float spreadZ, final float speed, final int count, final int radius, final float yOffset) {
        //location.getWorld().spawnParticle(null, location.getX(), location.getY() + yOffset, location.getZ(), count, spreadX, spreadY, spreadZ, speed);
        final Packet packet = new PacketPlayOutWorldParticles();
        EnumParticle particle = particleMap.get(name);
        int[] data = null;
        if(name.contains("_")) {
            final String[] split = name.split("_");
            name = split[0] + '_';
            particle = particleMap.get(name);
            if(split.length > 1) {
                final String[] split2 = split[1].split(":");
                data = new int[split2.length];
                for(int i = 0; i < data.length; i++) {
                    data[i] = Integer.parseInt(split2[i]);
                }
            }
        }
        if(particle == null) {
            MagicSpells.error("Invalid particle: " + name);
            return;
        }
        try {
            packet63Fields[0].set(packet, particle);
            packet63Fields[1].setFloat(packet, (float) location.getX());
            packet63Fields[2].setFloat(packet, (float) location.getY() + yOffset);
            packet63Fields[3].setFloat(packet, (float) location.getZ());
            packet63Fields[4].setFloat(packet, spreadX);
            packet63Fields[5].setFloat(packet, spreadY);
            packet63Fields[6].setFloat(packet, spreadZ);
            packet63Fields[7].setFloat(packet, speed);
            packet63Fields[8].setInt(packet, count);
            packet63Fields[9].setBoolean(packet, radius >= 30);
            if(data != null) {
                packet63Fields[10].set(packet, data);
            }
            final int rSq = radius * radius;
            
            for(final Player player : location.getWorld().getPlayers()) {
                //noinspection StatementWithEmptyBody
                if(player.getLocation().distanceSquared(location) <= rSq) {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                } else {
                    //no op yet
                }
            }
        } catch(final Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void playDragonDeathEffect(final Location location) {
        final EntityEnderDragon dragon = new EntityEnderDragon(((CraftWorld) location.getWorld()).getHandle());
        dragon.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), 0F);
        
        final Packet packet24 = new PacketPlayOutSpawnEntityLiving(dragon);
        final Packet packet38 = new PacketPlayOutEntityStatus(dragon, (byte) 3);
        final Packet packet29 = new PacketPlayOutEntityDestroy(dragon.getBukkitEntity().getEntityId());
        
        final BoundingBox box = new BoundingBox(location, 64);
        final Collection<Player> players = new ArrayList<>();
        for(final Player player : location.getWorld().getPlayers()) {
            if(box.contains(player)) {
                players.add(player);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet24);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet38);
            }
        }
        
        MagicSpells.scheduleDelayedTask(() -> {
            for(final Player player : players) {
                if(player.isValid()) {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet29);
                }
            }
        }, 250);
    }
    
    @Override
    public void setKiller(final LivingEntity entity, final Player killer) {
        ((CraftLivingEntity) entity).getHandle().killer = ((CraftPlayer) killer).getHandle();
    }
    
    @Override
    public IDisguiseManager getDisguiseManager(final MagicConfig config) {
        if(Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
            try {
                return new DisguiseManagerLibsDisguises(config);
            } catch(final Exception e) {
                return new DisguiseManagerEmpty(config);
            }
        } else {
            return new DisguiseManagerEmpty(config);
        }
    }
    
    @Override
    public ItemStack addAttributes(ItemStack item, final String[] names, final String[] types, final double[] amounts, final int[] operations) {
        if(!(item instanceof CraftItemStack)) {
            item = CraftItemStack.asCraftCopy(item);
        }
        final NBTTagCompound tag = getTag(item);
        
        final NBTTagList list = new NBTTagList();
        for(int i = 0; i < names.length; i++) {
            if(names[i] != null) {
                final NBTTagCompound attr = new NBTTagCompound();
                attr.setString("Name", names[i]);
                attr.setString("AttributeName", types[i]);
                attr.setDouble("Amount", amounts[i]);
                attr.setInt("Operation", operations[i]);
                final UUID uuid = UUID.randomUUID();
                attr.setLong("UUIDLeast", uuid.getLeastSignificantBits());
                attr.setLong("UUIDMost", uuid.getMostSignificantBits());
                list.add(attr);
            }
        }
        
        tag.set("AttributeModifiers", list);
        
        setTag(item, tag);
        return item;
    }
    
    @Override
    public ItemStack hideTooltipCrap(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
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
    public void addEntityAttribute(final LivingEntity entity, final String attribute, final double amount, final int operation) {
        Attribute attr = null;
        switch(attribute) {
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
        if(operation == 0) {
            oper = Operation.ADD_NUMBER;
        } else if(operation == 1) {
            oper = Operation.MULTIPLY_SCALAR_1;
        } else if(operation == 2) {
            oper = Operation.ADD_SCALAR;
        }
        if(attr != null && oper != null) {
            entity.getAttribute(attr).addModifier(new AttributeModifier("MagicSpells " + attribute, amount, oper));
        }
    }
    
    @Override
    public void resetEntityAttributes(final LivingEntity entity) {
        try {
            final EntityLiving e = ((CraftLivingEntity) entity).getHandle();
            final Field field = EntityLiving.class.getDeclaredField("bp");
            field.setAccessible(true);
            field.set(e, null);
            e.getAttributeMap();
            Method method = null;
            Class<?> clazz = e.getClass();
            while(clazz != null) {
                try {
                    method = clazz.getDeclaredMethod("initAttributes");
                    break;
                } catch(final NoSuchMethodException e1) {
                    clazz = clazz.getSuperclass();
                }
            }
            if(method != null) {
                method.setAccessible(true);
                method.invoke(e);
            } else {
                throw new Exception("No method initAttributes found on " + e.getClass().getName());
            }
        } catch(final Exception e) {
            MagicSpells.handleException(e);
        }
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void removeAI(final LivingEntity entity) {
        try {
            final EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();
            
            final Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
            goalsField.setAccessible(true);
            final PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
            
            Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
            listField.setAccessible(true);
            Set list = (Set) listField.get(goals);
            list.clear();
            listField = PathfinderGoalSelector.class.getDeclaredField("c");
            listField.setAccessible(true);
            list = (Set) listField.get(goals);
            list.clear();
            
            goals.a(0, new PathfinderGoalFloat(ev));
        } catch(final Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void addAILookAtPlayer(final LivingEntity entity, final int range) {
        try {
            final EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();
            
            final Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
            goalsField.setAccessible(true);
            final PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
            
            goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, range, 1.0F));
        } catch(final Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void setBossBar(final Player player, final String title, final double percent) {
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
    public void updateBossBar(final Player player, final String title, final double percent) {
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
    public void removeBossBar(final Player player) {
        //PacketPlayOutEntityDestroy packetDestroy = new PacketPlayOutEntityDestroy(bossBarEntity.getId());
        //((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetDestroy);
    }
    
    @Override
    public void saveSkinData(final Player player, final String name) {
        final GameProfile profile = ((CraftPlayer) player).getHandle().getProfile();
        final Collection<Property> props = profile.getProperties().get("textures");
        for(final Property prop : props) {
            final String skin = prop.getValue();
            final String sig = prop.getSignature();
            
            final File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
            if(!folder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                folder.mkdir();
            }
            final File skinFile = new File(folder, name + ".skin.txt");
            final File sigFile = new File(folder, name + ".sig.txt");
            try {
                FileWriter writer = new FileWriter(skinFile);
                writer.write(skin);
                writer.flush();
                writer.close();
                writer = new FileWriter(sigFile);
                writer.write(sig);
                writer.flush();
                writer.close();
            } catch(final Exception e) {
                e.printStackTrace();
            }
            
            // TODO: ??????????
            break;
        }
    }
    
    @Override
    public ItemStack setUnbreakable(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void setArrowsStuck(final LivingEntity entity, final int count) {
        //((CraftLivingEntity)entity).getHandle().set
    }
    
    @Override
    public void sendTitleToPlayer(final Player player, final String title, final String subtitle, final int fadeIn, final int stay, final int fadeOut) {
        final PlayerConnection conn = ((CraftPlayer) player).getHandle().playerConnection;
        PacketPlayOutTitle packet = new PacketPlayOutTitle(EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
        conn.sendPacket(packet);
        if(title != null) {
            packet = new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(title));
            conn.sendPacket(packet);
        }
        if(subtitle != null) {
            packet = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle));
            conn.sendPacket(packet);
        }
    }
    
    @Override
    public void sendActionBarMessage(final Player player, final String message) {
        final PlayerConnection conn = ((CraftPlayer) player).getHandle().playerConnection;
        final Packet packet = new PacketPlayOutChat(new ChatComponentText(message), (byte) 2);
        conn.sendPacket(packet);
    }
    
    @Override
    public void setTabMenuHeaderFooter(final Player player, final String header, final String footer) {
        final PlayerConnection conn = ((CraftPlayer) player).getHandle().playerConnection;
        final Packet packet = new PacketPlayOutPlayerListHeaderFooter();
        try {
            final Field field1 = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("a");
            final Field field2 = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b");
            field1.setAccessible(true);
            field1.set(packet, new ChatComponentText(header));
            field2.setAccessible(true);
            field2.set(packet, new ChatComponentText(footer));
            conn.sendPacket(packet);
        } catch(final Exception e) {
            MagicSpells.handleException(e);
        }
    }
    
    @Override
    public void setNoAIFlag(final LivingEntity entity) {
        //no op yet
    }
    
    @Override
    public void setClientVelocity(final Player player, final Vector velocity) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityVelocity(player.getEntityId(), velocity.getX(), velocity.getY(), velocity.getZ()));
    }
    
    @Override
    public double getAbsorptionHearts(final LivingEntity entity) {
        return ((CraftLivingEntity) entity).getHandle().getAbsorptionHearts();
    }
    
    @Override
    public void setOffhand(final Player player, final ItemStack item) {
        player.getInventory().setItemInOffHand(item);
    }
    
    @Override
    public ItemStack getOffhand(final Player player) {
        return player.getInventory().getItemInOffHand();
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void showItemCooldown(final Player player, final ItemStack item, final int duration) {
        final Packet packet = new PacketPlayOutSetCooldown(Item.getById(item.getTypeId()), duration);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
    
    @Override
    public void setItemInMainHand(final Player player, final ItemStack item) {
        player.getInventory().setItemInMainHand(item);
    }
    
    @Override
    public ItemStack getItemInMainHand(final Player player) {
        return player.getInventory().getItemInMainHand();
    }
    
    @Override
    public ItemStack getItemInMainHand(final EntityEquipment equip) {
        return equip.getItemInMainHand();
    }
    
    @Override
    public boolean hasGravity(final Entity entity) {
        return entity.hasGravity();
    }
    
    @Override
    public void setGravity(final Entity entity, final boolean gravity) {
        entity.setGravity(gravity);
    }
    
    @Override
    public void setTexture(final SkullMeta meta, final String texture, final String signature) {
        try {
            final GameProfile profile = (GameProfile) craftMetaSkullProfileField.get(meta);
            setTexture(profile, texture, signature);
            craftMetaSkullProfileField.set(meta, profile);
        } catch(final SecurityException | IllegalAccessException | IllegalArgumentException e) {
            MagicSpells.handleException(e);
        }
    }
    
    @Override
    public void setSkin(final Player player, final String skin, final String signature) {
        final CraftPlayer craftPlayer = (CraftPlayer) player;
        setTexture(craftPlayer.getProfile(), skin, signature);
    }
    
    private GameProfile setTexture(final GameProfile profile, final String texture, final String signature) {
        if(signature == null || signature.equalsIgnoreCase("")) {
            profile.getProperties().put("textures", new Property("textures", texture));
        } else {
            profile.getProperties().put("textures", new Property("textures", texture, signature));
        }
        return profile;
    }
    
    @Override
    public void setTexture(final SkullMeta meta, final String texture, final String signature,
                           final String uuid, final String name) {
        try {
            final GameProfile profile = new GameProfile(uuid != null ? UUID.fromString(uuid) : null, name);
            setTexture(profile, texture, signature);
            craftMetaSkullProfileField.set(meta, profile);
        } catch(final SecurityException | IllegalAccessException | IllegalArgumentException e) {
            MagicSpells.handleException(e);
        }
    }
}
