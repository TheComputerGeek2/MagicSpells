package com.nisovin.magicspells.volatilecode;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SafetyCheckUtils;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.util.compat.EventUtil;

public abstract class VolatileCodeEnabledNMSBase implements VolatileCodeHandle {

    VolatileCodeDisabled fallback = new VolatileCodeDisabled();

    public Field craftItemStackHandleField = null;
    public Field entityFallingBlockHurtEntitiesField = null;
    public Field entityFallingBlockFallHurtAmountField = null;
    public Field entityFallingBlockFallHurtMaxField = null;
    public Field craftMetaSkullProfileField = null;
    public Class<?> craftMetaSkullClass;
    public Class<?> craftItemStackClass;
    public Class<?> nbtTagCompoundClass;
    public Class<?> nbtBaseClass;
    public Method asCraftCopy;
    public Method nbtSetByte;
    public String baseNMSPackage;
    public String baseCBPackage;

    public VolatileCodeEnabledNMSBase(MagicConfig config, String version) {
        this.baseNMSPackage = "net.minecraft.server.v" + version + ".";
        this.baseCBPackage = "org.bukkit.craftbukkit.v" + version + ".";
        try {
            Class<?> worldParticles = Class.forName(baseNMSPackage + "PacketPlayOutWorldParticles");
            for (int i = 0; i < 11; i++)
                packet63Fields[i] = worldParticles.getDeclaredField((char) ('a' + i) + "");
            AccessibleObject.setAccessible(this.packet63Fields, true);

            craftItemStackClass = Class.forName(baseCBPackage + "inventory.CraftItemStack");
            asCraftCopy = craftItemStackClass.getMethod("asCraftCopy", ItemStack.class);
            nbtTagCompoundClass = Class.forName(baseNMSPackage + "NBTTagCompound");
            nbtSetByte = nbtTagCompoundClass.getMethod("setByte", String.class, byte.class);
            nbtBaseClass = Class.forName(baseNMSPackage + "NBTBase");

            Class<?> entityFalllingBlockClass = Class.forName(baseNMSPackage + "EntityFallingBlock");
            this.entityFallingBlockHurtEntitiesField = entityFalllingBlockClass.getDeclaredField("hurtEntities");
            this.entityFallingBlockHurtEntitiesField.setAccessible(true);

            this.entityFallingBlockFallHurtAmountField = entityFalllingBlockClass.getDeclaredField("fallHurtAmount");
            this.entityFallingBlockFallHurtAmountField.setAccessible(true);

            this.entityFallingBlockFallHurtMaxField = entityFalllingBlockClass.getDeclaredField("fallHurtMax");
            this.entityFallingBlockFallHurtMaxField.setAccessible(true);

            this.craftMetaSkullClass = Class.forName(baseCBPackage + ".inventory.CraftMetaSkull");
            this.craftMetaSkullProfileField = this.craftMetaSkullClass.getDeclaredField("profile");
            this.craftMetaSkullProfileField.setAccessible(true);

            for (Object particle : Class.forName(baseNMSPackage + "EnumParticle").getEnumConstants()) {
                if (particle != null) {
                    this.particleMap.put(particle.getClass().getMethod("b").invoke(particle).toString(), particle);
                }
            }

        } catch (Exception e) {
            MagicSpells.error(
                    "THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR REFLECTION, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICSPELLS.");
            MagicSpells.handleException(e);
        }

    }

    public Object getTag(Object item) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftItemStackHandleField)) {
            return null;
        }

        if (craftItemStackClass.isInstance(item)) {
            try {
                Object obj = this.craftItemStackHandleField.get(item);
                return obj.getClass().getMethod("getTag").invoke(obj);
            } catch (Exception e) {
                // No op currently
            }
        }
        return null;
    }

    public ItemStack setTag(Object item, Object tag) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        Object craftItem = item;
        if (!craftItemStackClass.isInstance(item))
            craftItem = asCraftCopy.invoke(item);

        Object nmsItem = null;
        try {
            nmsItem = this.craftItemStackHandleField.get(item);
        } catch (Exception e) {
            // No op currently
        }
        if (nmsItem == null) {
            nmsItem = craftItemStackClass.getMethod("asNMSCopy").invoke(null, craftItem);
        }

        if (nmsItem != null) {
            nmsItem.getClass().getMethod("setTag", nbtTagCompoundClass).invoke(nmsItem, tag);
            try {
                this.craftItemStackHandleField.set(craftItem, nmsItem);
            } catch (Exception e) {
                // No op currently
            }
        }

        return (ItemStack) craftItem;
    }

    @Override
    public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
    }

    @Override
    public void entityPathTo(LivingEntity creature, LivingEntity target) {
    }

    @Override
    public void creaturePathToLoc(Creature creature, Location loc, float speed) {
        try {
            Object entity = getHandle(creature);
            Object navigation = entity.getClass().getMethod("getNavigation").invoke(entity);
            navigation.getClass().getMethod("a", Class.forName(baseNMSPackage + "PathEntity"), float.class)
                    .invoke(navigation.getClass().getMethod("a", double.class, double.class, double.class)
                            .invoke(loc.getX(), loc.getY(), loc.getZ()), speed);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
        try {
            Object nmsItem;
            if (item != null) {
                nmsItem = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            } else {
                nmsItem = null;
            }
            sendPacket(player,
                    Class.forName("PacketPlayOutSetSlot")
                            .getConstructor(int.class, short.class, Class.forName(baseNMSPackage + "ItemStack"))
                            .newInstance(0, (short) slot + 36, nmsItem));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void toggleLeverOrButton(Block block) {
        this.fallback.toggleLeverOrButton(block);
    }

    @Override
    public void pressPressurePlate(Block block) {
        this.fallback.pressPressurePlate(block);
    }

    @Override
    public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
        try {
            Class<?> entityTNTPrimedClass = Class.forName(baseNMSPackage + "EntityTNTPrimed");
            Object e = entityTNTPrimedClass.getConstructor(Class.forName(baseNMSPackage + "World"), double.class,
                    double.class, double.class, Class.forName(baseNMSPackage + "EntityLiving"))
                    .newInstance(getHandle(target.getWorld()), target.getX(), target.getY(), target.getZ(),
                            getHandle(source));
            Class<?> craftServer = Class.forName(baseCBPackage + ".CraftServer");
            Object c = Class.forName(baseCBPackage + ".entity.CraftTNTPrimed").getConstructor(craftServer, e.getClass())
                    .newInstance(Bukkit.getServer(), e);
            ExplosionPrimeEvent event = new ExplosionPrimeEvent((Entity) c, explosionSize, fire);
            EventUtil.call(event);
            return event.isCancelled();
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return false;
    }

    @Override
    public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire,
                                           boolean breakBlocks) {
        try {
            Object handle = getHandle(location.getWorld());
            Object explosion = handle.getClass()
                    .getMethod("createExplosion", Class.forName(baseNMSPackage + "Entity"), double.class, double.class,
                            double.class, float.class, boolean.class, boolean.class)
                    .invoke(handle, getHandle(player), location.getX(), location.getY(), location.getZ(), size, fire,
                            breakBlocks);
            return explosion.getClass().getField("wasCanceled").getBoolean(explosion);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return false;
    }

    @Override
    public void playExplosionEffect(Location location, float size) {
        try {
            Object packet = Class.forName(baseNMSPackage + "PacketPlayOutExplosion")
                    .getConstructor(double.class, double.class, double.class, float.class, List.class,
                            Class.forName(baseNMSPackage + "Vec3D"))
                    .newInstance(location.getX(), location.getY(), location.getZ(), size, new ArrayList<>(), null);
            for (Player player : location.getWorld().getPlayers()) {
                if (LocationUtil.distanceGreaterThan(player, location, 50)) {
                    sendPacket(player, packet);
                }
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void setExperienceBar(Player player, int level, float percent) {
        try {
            sendPacket(player,
                    Class.forName(baseNMSPackage + "PacketPlayOutExperience")
                            .getConstructor(float.class, int.class, int.class)
                            .newInstance(percent, player.getTotalExperience(), level));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public Fireball shootSmallFireball(Player player) {
        try {
            Object nmsWorld = getHandle(player.getWorld());
            Location playerLoc = player.getLocation();
            Vector loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(10));

            double d0 = loc.getX() - playerLoc.getX();
            double d1 = loc.getY() - (playerLoc.getY() + 1.5);
            double d2 = loc.getZ() - playerLoc.getZ();
            Object fireball = Class
                    .forName(baseNMSPackage + "EntitySmallFireball").getConstructor(nmsWorld.getClass(),
                            getHandle(player).getClass(), double.class, double.class, double.class)
                    .newInstance(nmsWorld, getHandle(player), d0, d1, d2);

            fireball.getClass().getField("locY").set(fireball, playerLoc.getY() + 1.5);
            nmsWorld.getClass().getMethod("addEntity", Class.forName(baseNMSPackage + "Entity")).invoke(nmsWorld,
                    fireball);

            return (Fireball) (fireball.getClass().getMethod("getBukkitEntity").invoke(null));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return null;
    }

    @Override
    public void setTarget(LivingEntity entity, LivingEntity target) {
        try {
            if (entity instanceof Creature) {
                ((Creature) entity).setTarget(target);
            } else {
                Object handle = getHandle(entity);
                Class<?> craftLivingEntity = Class.forName(baseCBPackage + "entity.CraftLivingEntity");
                handle.getClass().getMethod("setGoalTarget", craftLivingEntity, TargetReason.class, boolean.class)
                        .invoke(handle, getHandle(target), TargetReason.CUSTOM, true);
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public ItemStack addFakeEnchantment(ItemStack item) {
        try {
            if (!craftItemStackClass.isInstance(item)) {
                item = (ItemStack) asCraftCopy.invoke(null, item);
            }
            Object tag = getTag(item);
            if (tag == null) {
                tag = nbtTagCompoundClass.newInstance();
            }
            if (!(boolean) nbtTagCompoundClass.getMethod("hasKey", String.class).invoke(tag, "ench")) {
                Class<?> nbtTagList = Class.forName(baseNMSPackage + "NBTTagList");
                nbtTagCompoundClass.getMethod("set", String.class, nbtTagList).invoke(tag, "ench",
                        nbtTagList.newInstance());
            }
            return setTag(item, tag);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return item;
    }

    @Override
    public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
        try {
            Object efb = getHandle(block);
            this.entityFallingBlockHurtEntitiesField.setBoolean(efb, true);
            this.entityFallingBlockFallHurtAmountField.setFloat(efb, damage);
            this.entityFallingBlockFallHurtMaxField.setInt(efb, max);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void playEntityAnimation(final Location location, final EntityType entityType, final int animationId,
                                    boolean instant) {
    }

    @Override
    public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors,
                                         int[] fadeColors, int flightDuration) {
        try {
            // Create item
            Class<?> itemClass = Class.forName(baseNMSPackage + "Item");
            Object item = Class.forName(baseNMSPackage + "ItemStack").getConstructor(itemClass, int.class, int.class)
                    .newInstance(itemClass.getMethod("getById", int.class).invoke(null, 401), 1, 0);
            Object tag = getTag(item);
            if (tag == null) {
                tag = nbtTagCompoundClass.newInstance();
            }
            // Create explosion tag
            Object explTag = nbtTagCompoundClass.newInstance();
            Class<?> intArray = Class.forName("[I");
            nbtSetByte.invoke(explTag, "Flicker", flicker ? (byte) 1 : (byte) 0);
            nbtSetByte.invoke(explTag, "Trail", trail ? (byte) 1 : (byte) 0);
            nbtSetByte.invoke(explTag, "Type", (byte) type);
            explTag.getClass().getMethod("setIntArray", String.class, intArray).invoke(explTag, "Colors", colors);
            explTag.getClass().getMethod("setIntArray", String.class, intArray).invoke(explTag, "FadeColors",
                    fadeColors);

            // Create fireworks tag
            Object fwTag = nbtTagCompoundClass.newInstance();
            fwTag.getClass().getMethod("setByte", String.class, byte.class).invoke(fwTag, "Flight",
                    (byte) flightDuration);
            Object explList = Class.forName(baseNMSPackage + "NBTTagList");
            explList.getClass().getMethod("add", nbtBaseClass).invoke(explList, explTag);
            fwTag.getClass().getMethod("set", String.class, nbtBaseClass).invoke(fwTag, "Explosions", explList);
            tag.getClass().getMethod("set", String.class, nbtBaseClass).invoke(tag, "Fireworks", fwTag);

            // Set tag
            item = setTag(item, tag);

            // Create fireworks entity
            Object worldHandle = getHandle(location.getWorld());
            Object fireworks = Class.forName(baseNMSPackage + "EntityFireworks")
                    .getConstructor(worldHandle.getClass(), double.class, double.class, double.class, item.getClass())
                    .newInstance(worldHandle, location.getX(), location.getY(), location.getZ(), item);
            worldHandle.getClass().getMethod("addEntity", Class.forName(baseNMSPackage + "Entity")).invoke(worldHandle,
                    fireworks);

            // Cause explosion
            if (flightDuration == 0) {
                worldHandle.getClass()
                        .getMethod("broadcastEntityEffect", Class.forName(baseNMSPackage + "Entity"), byte.class)
                        .invoke(worldHandle, fireworks, (byte) 17);
                fireworks.getClass().getMethod("die").invoke(fireworks);
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    Field[] packet63Fields = new Field[11];
    Map<String, Object> particleMap = new HashMap<>();

    @Override
    public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed,
                                   int count, int radius, float yOffset) {
        playParticleEffect(location, name, spreadHoriz, spreadVert, spreadHoriz, speed, count, radius, yOffset);
    }

    @Override
    public void playParticleEffect(Location location, String name, float spreadX, float spreadY, float spreadZ,
                                   float speed, int count, int radius, float yOffset) {
        try {
            Object packet = Class.forName("PacketPlayOutWorldParticles").newInstance();
            Object particle = this.particleMap.get(name);
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
            this.packet63Fields[0].set(packet, particle);
            this.packet63Fields[1].setFloat(packet, (float) location.getX());
            this.packet63Fields[2].setFloat(packet, (float) location.getY() + yOffset);
            this.packet63Fields[3].setFloat(packet, (float) location.getZ());
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
                    sendPacket(player, packet);
                } else {
                    // No op yet
                }
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void playDragonDeathEffect(Location location) {
        try {
            Object worldHandle = getHandle(location.getWorld());
            Object dragon = Class.forName(baseNMSPackage + "EntityEnderDragon").getConstructor(worldHandle.getClass())
                    .newInstance(worldHandle);
            dragon.getClass()
                    .getMethod("setPositionRotation", double.class, double.class, double.class, float.class,
                            float.class)
                    .invoke(dragon, location.getX(), location.getY(), location.getZ(), location.getYaw(), 0F);
            Object packet24 = Class.forName(baseNMSPackage + "PacketPlayOutSpawnEntityLiving")
                    .getConstructor(Class.forName(baseNMSPackage + "Entity")).newInstance(dragon);
            Object packet38 = Class.forName(baseNMSPackage + "PacketPlayOutEntityStatus")
                    .getConstructor(Class.forName(baseNMSPackage + "Entity"), byte.class).newInstance(dragon, (byte) 3);
            Object packet29 = Class.forName(baseNMSPackage + "PacketPlayOutEntityDestroy").getConstructor(int.class)
                    .newInstance(
                            ((Entity) dragon.getClass().getMethod("getBukkitEntity").invoke(dragon)).getEntityId());

            BoundingBox box = new BoundingBox(location, 64);
            final List<Player> players = new ArrayList<>();
            for (Player player : location.getWorld().getPlayers()) {
                if (!box.contains(player)) {
                    continue;
                }
                players.add(player);
                sendPacket(player, packet24);
                sendPacket(player, packet38);
            }

            MagicSpells.scheduleDelayedTask(() -> {
                for (Player player : players) {
                    if (player.isValid()) {
                        try {
                            sendPacket(player, packet29);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                                | NoSuchMethodException | SecurityException | ClassNotFoundException
                                | NoSuchFieldException e) {
                            MagicSpells.handleException(e);
                        }
                    }

                }
            }, 250);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void setKiller(LivingEntity entity, Player killer) {
        try {
            Object handle = getHandle(entity);
            handle.getClass().getField("killer").set(handle, getHandle(killer));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public IDisguiseManager getDisguiseManager(MagicConfig config) {
        if (CompatBasics.pluginEnabled("LibsDisguises")) {
            try {
                return new DisguiseManagerLibsDisguises(config);
            } catch (Exception e) {
            }
        }
        return new DisguiseManagerEmpty(config);
    }

    @Override
    public ItemStack addAttributes(ItemStack item, String[] names, String[] types, double[] amounts, int[] operations,
                                   String[] slots) {
        try {
            if (!craftItemStackClass.isInstance(item)) {
                item = (ItemStack) asCraftCopy.invoke(null, item);
            }
            Object tag = getTag(item);

            Object list = Class.forName(baseNMSPackage + "NBTTagList").newInstance();
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null) {
                    continue;
                }
                Object attr = buildAttributeTag(names[i], types[i], amounts[i], operations[i], UUID.randomUUID(),
                        slots[i]);
                list.getClass().getMethod("add", nbtBaseClass).invoke(list, attr);
            }

            tag.getClass().getMethod("set", String.class, nbtBaseClass).invoke(list, "AttributeModifiers", list);
            setTag(item, tag);
            return item;
        } catch (Exception e) {
            MagicSpells.handleException(e);
            return item;
        }
    }

    public Object buildAttributeTag(String name, String attributeName, double amount, int operation, UUID uuid,
                                    String slot) {
        try {
            Object tag = nbtTagCompoundClass.newInstance();
            tag.getClass().getMethod("setString", String.class, String.class).invoke(tag, "Name", name);
            tag.getClass().getMethod("setString", String.class, String.class).invoke(tag, "AttributeName",
                    attributeName);
            tag.getClass().getMethod("setDouble", String.class, double.class).invoke(tag, "Amount", amount);
            tag.getClass().getMethod("setInt", String.class, int.class).invoke(tag, "Operation", operation);
            tag.getClass().getMethod("setLong", String.class, long.class).invoke(tag, "UUIDLeast",
                    uuid.getLeastSignificantBits());
            tag.getClass().getMethod("setLong", String.class, long.class).invoke(tag, "UUIDMost",
                    uuid.getMostSignificantBits());
            if (slot != null) {
                tag.getClass().getMethod("setString", String.class, String.class).invoke(tag, "Slot", slot);
            }

            return tag;
        } catch (Exception e) {
            MagicSpells.handleException(e);
            return null;
        }
    }

    @Override
    public ItemStack hideTooltipCrap(ItemStack item) {
        try {
            if (!craftItemStackClass.isInstance(item)) {
                item = (ItemStack) asCraftCopy.invoke(null, item);
            }
            Object tag = getTag(item);
            if (tag == null) {
                tag = nbtTagCompoundClass.newInstance();
            }
            tag.getClass().getMethod("setInt", String.class, int.class).invoke(tag, "HideFlags", 63);
            setTag(item, tag);
            return item;
        } catch (Exception e) {
            MagicSpells.handleException(e);
            return item;
        }
    }

    @Override
    public void setBossBar(Player player, String title, double percent) {
    }

    @Override
    public void updateBossBar(Player player, String title, double percent) {
    }

    @Override
    public void removeBossBar(Player player) {
    }

    @Override
    public void saveSkinData(Player player, String name) {
        try {
            Object handle = getHandle(player);
            GameProfile profile = (GameProfile) handle.getClass().getMethod("getProfile").invoke(handle);
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
                    MagicSpells.handleException(e);
                }

                break;
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public ItemStack setUnbreakable(ItemStack item) {
        try {
            if (!craftItemStackClass.isInstance(item)) {
                item = (ItemStack) asCraftCopy.invoke(null, item);
            }
            Object tag = getTag(item);
            if (tag == null) {
                tag = nbtTagCompoundClass.newInstance();
            }
            tag.getClass().getMethod("setByte", String.class, byte.class).invoke(tag, "Unbreakable", (byte) 1);
            return setTag(item, tag);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return item;
    }

    @Override
    public void setArrowsStuck(LivingEntity entity, int count) {
    }

    @Override
    public void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> titleAction = Class.forName(baseNMSPackage + "EnumTitleAction");
            Class<?> packetClass = Class.forName(baseNMSPackage + "PacketPlayOutTitle");
            Class<?> iChatThing = Class.forName(baseNMSPackage + "IChatBaseComponent");
            Object packet = packetClass.getConstructor(titleAction, packetClass, int.class, int.class, int.class)
                    .newInstance(titleAction.getField("TIMES").get(null), null, fadeIn, stay, fadeOut);
            sendPacket(player, packet);
            if (title != null) {
                packet = Class.forName(baseNMSPackage + "PacketPlayOutTitle").getConstructor(titleAction, iChatThing)
                        .newInstance(titleAction.getField("TITLE").get(null),
                                Class.forName(baseNMSPackage + "ChatComponentText").getConstructor(String.class)
                                        .newInstance(title));
                sendPacket(player, packet);
            }
            if (subtitle != null) {
                packet = Class.forName(baseNMSPackage + "PacketPlayOutTitle").getConstructor(titleAction, iChatThing)
                        .newInstance(titleAction.getField("SUBTITLE").get(null),
                                Class.forName(baseNMSPackage + "ChatComponentText").getConstructor(String.class)
                                        .newInstance(title));
                sendPacket(player, packet);
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        try {
            Object chatComponent = Class.forName(baseNMSPackage + "ChatComponentText").getConstructor(String.class)
                    .newInstance(message);
            sendPacket(player, Class.forName(baseNMSPackage + "PacketPlayOutChat")
                    .getConstructor(chatComponent.getClass(), byte.class).newInstance(chatComponent, 2));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void setTabMenuHeaderFooter(Player player, String header, String footer) {
        try {
            Object packet = Class.forName(baseNMSPackage + "PacketPlayOutPlayerListHeaderFooter").newInstance();
            try {
                Field field1 = packet.getClass().getDeclaredField("a");
                Field field2 = packet.getClass().getDeclaredField("b");
                field1.setAccessible(true);
                field1.set(packet, Class.forName(baseNMSPackage + "ChatComponentText").getConstructor(String.class)
                        .newInstance(header));
                field2.setAccessible(true);
                field2.set(packet, Class.forName(baseNMSPackage + "ChatComponentText").getConstructor(String.class)
                        .newInstance(footer));
                sendPacket(player, packet);
            } catch (Exception e) {
                MagicSpells.handleException(e);
            }
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void setClientVelocity(Player player, Vector velocity) {
        try {
            sendPacket(player,
                    Class.forName(baseNMSPackage + "PacketPlayOutEntityVelocity")
                            .getConstructor(int.class, double.class, double.class, double.class)
                            .newInstance(player.getEntityId(), velocity.getX(), velocity.getY(), velocity.getZ()));
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public double getAbsorptionHearts(LivingEntity entity) {
        try {
            Object handle = getHandle(entity);
            return (double) handle.getClass().getMethod("getAbsorptionHearts").invoke(handle);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
        return 0;
    }

    @Override
    public boolean hasGravity(Entity entity) {
        try {
            return (boolean) entity.getClass().getMethod("hasGravity").invoke(entity);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void setGravity(Entity entity, boolean gravity) {
        try {
            entity.getClass().getMethod("setGravity", boolean.class).invoke(entity, gravity);
        } catch (Exception e) {
        }
    }

    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {
        try {
            Object handle = getHandle(location.getWorld());
            handle.getClass()
                    .getMethod("makeSound", double.class, double.class, double.class, String.class, float.class,
                            float.class)
                    .invoke(handle, location.getX(), location.getY(), location.getZ(), sound, volume, pitch);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void playSound(Player player, String sound, float volume, float pitch) {
        Location loc = player.getLocation();
        try {
            Object packet = Class.forName(baseNMSPackage + "PacketPlayOutNamedSoundEffect")
                    .getConstructor(String.class, double.class, double.class, double.class, float.class, float.class)
                    .newInstance(sound, loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
            sendPacket(player, packet);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    @Override
    public void showItemCooldown(Player player, ItemStack item, int duration) {
        try {
            Class<?> itemClass = Class.forName(baseNMSPackage + "Item");
            Object packet = Class.forName(baseNMSPackage + "PacketPlayOutSetCooldown")
                    .getConstructor(itemClass, int.class)
                    .newInstance(itemClass.getMethod("getById").invoke(null, item.getTypeId()), duration);
            sendPacket(player, packet);
        } catch (Exception e) {
        }
    }

    @Override
    public void setNoAIFlag(LivingEntity entity) {
        try {
            entity.getClass().getMethod("setAI", boolean.class).invoke(entity, false);
        } catch (Exception e) {
        }
    }

    @Override
    public void setTexture(SkullMeta meta, String texture, String signature) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField))
            return;

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
        try {
            setTexture((SkullMeta) player.getClass().getMethod("getProfile").invoke(player), skin, signature);
        } catch (Exception e) {
            MagicSpells.handleException(e);
        }
    }

    public GameProfile setTexture(GameProfile profile, String texture, String signature) {
        if (signature == null || signature.isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", texture));
        } else {
            profile.getProperties().put("textures", new Property("textures", texture, signature));
        }
        return profile;
    }

    @Override
    public void setTexture(SkullMeta meta, String texture, String signature, String uuid, String name) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField))
            return;

        try {
            GameProfile profile = new GameProfile(uuid != null ? UUID.fromString(uuid) : null, name);
            setTexture(profile, texture, signature);
            this.craftMetaSkullProfileField.set(meta, profile);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            MagicSpells.handleException(e);
        }
    }

    public Object getHandle(Object e) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        e.getClass().getMethod("getHandle").setAccessible(true);
        return e.getClass().getMethod("getHandle").invoke(e);
    }

    public Object getConnection(Player player) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
        return getConnection(getHandle(player));
    }

    public Object getConnection(Object handle)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        return handle.getClass().getField("playerConnection").get(handle);
    }

    public Method getSendPacket(Object connection)
            throws NoSuchMethodException, SecurityException, ClassNotFoundException {
        return connection.getClass().getMethod("sendPacket", Class.forName(baseNMSPackage + "Packet"));
    }

    public void sendPacket(Player player, Object packet)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
            SecurityException, ClassNotFoundException, NoSuchFieldException {
        Object connection = getConnection(player);
        getSendPacket(connection).invoke(connection, packet);
    }
}
