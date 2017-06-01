package com.nisovin.magicspells.util;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.ItemNameResolver.ItemTypeAndData;
import com.nisovin.magicspells.materials.MagicMaterial;
import de.slikey.effectlib.util.VectorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings({"deprecation", "TypeMayBeWeakened", "unused", "WeakerAccess", "PublicField", "PackageVisibleField"})
public final class Util {
    
    private static final Random random = new Random();
    private static final Map<String, String> uniqueIds = new HashMap<>();
    public static Map<String, ItemStack> predefinedItems = new HashMap<>();
    static Map<String, EntityType> entityTypeMap = new HashMap<>();
    
    static {
        for(final EntityType type : EntityType.values()) {
            if(type != null && type.getName() != null) {
                entityTypeMap.put(type.getName().toLowerCase(), type);
                entityTypeMap.put(type.name().toLowerCase(), type);
                entityTypeMap.put(type.name().toLowerCase().replace("_", ""), type);
            }
        }
        entityTypeMap.put("zombiepig", EntityType.PIG_ZOMBIE);
        entityTypeMap.put("mooshroom", EntityType.MUSHROOM_COW);
        entityTypeMap.put("cat", EntityType.OCELOT);
        entityTypeMap.put("golem", EntityType.IRON_GOLEM);
        entityTypeMap.put("snowgolem", EntityType.SNOWMAN);
        entityTypeMap.put("dragon", EntityType.ENDER_DRAGON);
        final Map<String, EntityType> toAdd = new HashMap<>();
        for(final Entry<String, EntityType> stringEntityTypeEntry : entityTypeMap.entrySet()) {
            toAdd.put(stringEntityTypeEntry.getKey() + 's', stringEntityTypeEntry.getValue());
        }
        entityTypeMap.putAll(toAdd);
        entityTypeMap.put("endermen", EntityType.ENDERMAN);
        entityTypeMap.put("wolves", EntityType.WOLF);
    }
    
    private Util() {
    }
    
    @SuppressWarnings("SameParameterValue")
    public static int getRandomInt(final int bound) {
        return random.nextInt(bound);
    }
    
    /**
     * Format is<br />
     * <p>
     * <code>itemID#color;enchant-level+enchant-level+enchant-level...|name|lore|lore...</code><p />
     * <p>
     * OR<p>
     * <p>
     * <code>predefined item key</code><br />
     *
     * @param string The string to resolve to an item
     *
     * @return the item stack represented by the string
     */
    public static ItemStack getItemStackFromString(final String string) {
        try {
            if(predefinedItems.containsKey(string)) {
                return predefinedItems.get(string).clone();
            }
            
            ItemStack item;
            String s = string;
            String name = null;
            String[] lore = null;
            Map<Enchantment, Integer> enchants = null;
            int color = -1;
            if(s.contains("|")) {
                final String[] temp = s.split("\\|");
                s = temp[0];
                if(temp.length == 1) {
                    name = "";
                } else {
                    name = ChatColor.translateAlternateColorCodes('&', temp[1].replace("__", " "));
                    if(temp.length > 2) {
                        lore = Arrays.copyOfRange(temp, 2, temp.length);
                        for(int i = 0; i < lore.length; i++) {
                            lore[i] = ChatColor.translateAlternateColorCodes('&', lore[i].replace("__", " "));
                        }
                    }
                }
            }
            if(s.contains(";")) {
                final String[] temp = s.split(";", 2);
                s = temp[0];
                enchants = new HashMap<>();
                if(!temp[1].isEmpty()) {
                    final String[] split = temp[1].split("\\+");
                    for(final String aSplit : split) {
                        final String[] enchantData = aSplit.split("-");
                        final Enchantment ench;
                        if(enchantData[0].matches("[0-9]+")) {
                            ench = Enchantment.getById(Integer.parseInt(enchantData[0]));
                        } else {
                            ench = Enchantment.getByName(enchantData[0].toUpperCase());
                        }
                        if(ench != null && enchantData[1].matches("[0-9]+")) {
                            enchants.put(ench, Integer.parseInt(enchantData[1]));
                        }
                    }
                }
            }
            if(s.contains("#")) {
                final String[] temp = s.split("#");
                s = temp[0];
                if(temp[1].matches("[0-9A-Fa-f]+")) {
                    color = Integer.parseInt(temp[1], 16);
                }
            }
            final ItemTypeAndData itemTypeAndData = MagicSpells.getItemNameResolver().resolve(s);
            if(itemTypeAndData != null) {
                item = new ItemStack(itemTypeAndData.id, 1, itemTypeAndData.data);
            } else {
                return null;
            }
            //noinspection ConstantConditions
            if(name != null || lore != null || color >= 0) {
                try {
                    final ItemMeta meta = item.getItemMeta();
                    if(name != null) {
                        meta.setDisplayName(name);
                    }
                    if(lore != null) {
                        meta.setLore(Arrays.asList(lore));
                    }
                    if(color >= 0 && meta instanceof LeatherArmorMeta) {
                        ((LeatherArmorMeta) meta).setColor(Color.fromRGB(color));
                    }
                    item.setItemMeta(meta);
                } catch(final Exception e) {
                    MagicSpells.error("Failed to process item meta for item: " + s);
                }
            }
            if(enchants != null) {
                if(!enchants.isEmpty()) {
                    item.addUnsafeEnchantments(enchants);
                } else {
                    item = MagicSpells.getVolatileCodeHandler().addFakeEnchantment(item);
                }
            }
            return item;
        } catch(final Exception e) {
            MagicSpells.handleException(e);
            return null;
        }
    }
    
    /**
     * <strong>Global Options</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>All Items</li>
     * </ul>
     * <p/>
     * <p>
     * <code>type</code>: &lt;String&gt;<br />
     * Description: The name of the material type.
     * <p/>
     * <p>
     * <code>name</code>: &lt;String&gt;<br />
     * Description: The custom name for the item.
     * <p/>
     * <p>
     * <code>lore</code>: &lt;String or String List&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <code>enchants</code>: &lt;String List&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <code>hide-tooltip</code>: &lt;Boolean&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <code>unbreakable</code>: &lt;Boolean&gt;<br />
     * Description: If true, the item will not take durability damage by vanilla behavior.
     * <p/>
     * <p>
     * <code>attributes</code>: &lt;Configuration Section&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>LeatherArmorMeta</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>Leather Armor</li>
     * </ul>
     * <p/>
     * <p>
     * <code>color</code>: &lt;String&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>PotionMeta</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>Potions</li>
     * <li>Tipped Arrows</li>
     * </ul>
     * <p/>
     * <p>
     * <code>potioneffects</code>: &lt;String List&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>SkullMeta</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>Skulls</li>
     * </ul>
     * <p/>
     * <p>
     * <code>skullowner</code>: &lt;String&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>Repairable</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>TODO</li>
     * </ul>
     * <p/>
     * <p>
     * <code>repaircost</code>: &lt;Integer&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>BookMeta</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>Written Books</li>
     * </ul>
     * <p/>
     * <p>
     * <code>title</code>: &lt;String&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <code>author</code>: &lt;String&gt;<br />
     * Description: the name to use as the author of the book.
     * <p/>
     * <p>
     * <code>pages</code>: &lt;String List&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <hr>
     * <p>
     * <strong>BannerMeta</strong><br />
     * Currently Applies to:
     * <ul>
     * <li>Banners</li>
     * </ul>
     * <p/>
     * <p>
     * <code>color</code>: &lt;String&gt;<br />
     * //TODO explain
     * <p/>
     * <p>
     * <code>patterns</code>: &lt;String List&gt;<br />
     * //TODO explain
     */
    public static ItemStack getItemStackFromConfig(final ConfigurationSection config) {
        try {
            if(!config.contains("type")) {
                return null;
            }
            
            // basic item
            final MagicMaterial material = MagicSpells.getItemNameResolver().resolveItem(config.getString("type"));
            if(material == null) {
                return null;
            }
            ItemStack item = material.toItemStack();
            final ItemMeta meta = item.getItemMeta();
            
            // name and lore
            if(config.contains("name") && config.isString("name")) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("name")));
            }
            if(config.contains("lore")) {
                if(config.isList("lore")) {
                    final List<String> lore = config.getStringList("lore");
                    for(int i = 0; i < lore.size(); i++) {
                        lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                    }
                    meta.setLore(lore);
                } else if(config.isString("lore")) {
                    final List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', config.getString("lore")));
                    meta.setLore(lore);
                }
            }
            
            // enchants
            boolean emptyEnchants = false;
            if(config.contains("enchants") && config.isList("enchants")) {
                final List<String> enchants = config.getStringList("enchants");
                for(final String enchant : enchants) {
                    final String[] data = enchant.split(" ");
                    Enchantment e;
                    try {
                        final int id = Integer.parseInt(data[0]);
                        e = Enchantment.getById(id);
                    } catch(final NumberFormatException ex) {
                        e = Enchantment.getByName(data[0].toUpperCase());
                    }
                    if(e == null) {
                        MagicSpells.error('\'' + data[0] + "' could not be connected to an enchantment");
                    }
                    if(e != null) {
                        int level = 0;
                        if(data.length > 1) {
                            try {
                                level = Integer.parseInt(data[1]);
                            } catch(final NumberFormatException ex) {
                                DebugHandler.debugNumberFormat(ex);
                            }
                        }
                        if(meta instanceof EnchantmentStorageMeta) {
                            ((EnchantmentStorageMeta) meta).addStoredEnchant(e, level, true);
                        } else {
                            meta.addEnchant(e, level, true);
                        }
                    }
                }
                if(enchants.isEmpty()) {
                    emptyEnchants = true;
                }
            }
            
            // armor color
            if(config.contains("color") && config.isString("color") && meta instanceof LeatherArmorMeta) {
                try {
                    final int color = Integer.parseInt(config.getString("color").replace("#", ""), 16);
                    ((LeatherArmorMeta) meta).setColor(Color.fromRGB(color));
                } catch(final NumberFormatException e) {
                    //TODO try processing by rgb
                    //TODO try processing by name if rgb fails
                    DebugHandler.debugNumberFormat(e);
                }
            }
            
            // potion effects
            if(config.contains("potioneffects") && config.isList("potioneffects") && meta instanceof PotionMeta) {
                ((PotionMeta) meta).clearCustomEffects();
                final List<String> potionEffects = config.getStringList("potioneffects");
                for(final String potionEffect : potionEffects) {
                    final String[] data = potionEffect.split(" ");
                    PotionEffectType t;
                    try {
                        final int id = Integer.parseInt(data[0]);
                        t = PotionEffectType.getById(id);
                    } catch(final NumberFormatException e) {
                        t = PotionEffectType.getByName(data[0].toUpperCase());
                    }
                    if(t == null) {
                        MagicSpells.error('\'' + data[0] + "' could not be connected to a potion effect type");
                    }
                    if(t != null) {
                        int level = 0;
                        if(data.length > 1) {
                            try {
                                level = Integer.parseInt(data[1]);
                            } catch(final NumberFormatException ex) {
                                DebugHandler.debugNumberFormat(ex);
                            }
                        }
                        int duration = 600;
                        if(data.length > 2) {
                            try {
                                duration = Integer.parseInt(data[2]);
                            } catch(final NumberFormatException ex) {
                                DebugHandler.debugNumberFormat(ex);
                            }
                        }
                        boolean ambient = false;
                        if(data.length > 3 && (data[3].equalsIgnoreCase("true") || data[3].equalsIgnoreCase("yes") || data[3].equalsIgnoreCase("ambient"))) {
                            ambient = true;
                        }
                        ((PotionMeta) meta).addCustomEffect(new PotionEffect(t, duration, level, ambient), true);
                    }
                }
            }
            
            // skull owner
            if(meta instanceof SkullMeta) {
                if(config.contains("skullowner") && config.isString("skullowner")) {
                    ((SkullMeta) meta).setOwner(config.getString("skullowner"));
                }
                
                String uuid = null;
                if(config.contains("uuid") && config.isString("uuid")) {
                    uuid = config.getString("uuid");
                }
                
                String texture = null;
                if(config.contains("texture") && config.isString("texture")) {
                    texture = config.getString("texture");
                }
                
                String signature = null;
                if(config.contains("signature") && config.isString("signature")) {
                    signature = config.getString("signature");
                }
                if(texture != null) {
                    MagicSpells.getVolatileCodeHandler().setTexture((SkullMeta) meta, texture, signature, uuid, ((SkullMeta) meta).getOwner());
                }
            }
            // flower pot
            /*if (config.contains("flower") && item.getType() == Material.FLOWER_POT && meta instanceof BlockStateMeta) {
                MagicMaterial flower = MagicSpells.getItemNameResolver().resolveBlock(config.getString("flower"));
				BlockState state = ((BlockStateMeta)meta).getBlockState();
				MaterialData data = state.getData();
				if (data instanceof FlowerPot) {
					((FlowerPot)data).setContents(new MaterialData(flower.getMaterial()));
				}
				state.setData(data);
				((BlockStateMeta)meta).setBlockState(state);
			}*/
            
            // repair cost
            if(config.contains("repaircost") && config.isInt("repaircost") && meta instanceof Repairable) {
                ((Repairable) meta).setRepairCost(config.getInt("repaircost"));
            }
            
            // written book
            if(meta instanceof BookMeta) {
                if(config.contains("title") && config.isString("title")) {
                    ((BookMeta) meta).setTitle(ChatColor.translateAlternateColorCodes('&', config.getString("title")));
                }
                if(config.contains("author") && config.isString("author")) {
                    ((BookMeta) meta).setAuthor(ChatColor.translateAlternateColorCodes('&', config.getString("author")));
                }
                if(config.contains("pages") && config.isList("pages")) {
                    final List<String> pages = config.getStringList("pages");
                    for(int i = 0; i < pages.size(); i++) {
                        pages.set(i, ChatColor.translateAlternateColorCodes('&', pages.get(i)));
                    }
                    ((BookMeta) meta).setPages(pages);
                }
            }
            
            // banner
            if(meta instanceof BannerMeta) {
                if(config.contains("color") && config.isString("color")) {
                    final String s = config.getString("color").toLowerCase();
                    for(final DyeColor c : DyeColor.values()) {
                        if(c != null && c.name().replace("_", "").toLowerCase().equals(s)) {
                            ((BannerMeta) meta).setBaseColor(c);
                            break;
                        }
                    }
                }
                if(config.contains("patterns") && config.isList("patterns")) {
                    final List<String> patterns = config.getStringList("patterns");
                    for(final String patternData : patterns) {
                        if(patternData.contains(" ")) {
                            final String[] split = patternData.split(" ");
                            DyeColor color = null;
                            for(final DyeColor c : DyeColor.values()) {
                                if(c != null && c.name().replace("_", "").toLowerCase().equals(split[0].toLowerCase())) {
                                    color = c;
                                    break;
                                }
                            }
                            PatternType pattern = PatternType.getByIdentifier(split[1]);
                            if(pattern == null) {
                                for(final PatternType p : PatternType.values()) {
                                    if(p != null && p.name().equalsIgnoreCase(split[1])) {
                                        pattern = p;
                                        break;
                                    }
                                }
                            }
                            if(color != null && pattern != null) {
                                ((BannerMeta) meta).addPattern(new Pattern(color, pattern));
                            }
                        }
                    }
                }
            }
            
            // set meta
            item.setItemMeta(meta);
            
            // hide tooltip
            if(config.getBoolean("hide-tooltip", MagicSpells.hidePredefinedItemTooltips())) {
                item = MagicSpells.getVolatileCodeHandler().hideTooltipCrap(item);
            }
            
            // unbreakable
            if(config.getBoolean("unbreakable", false)) {
                item = MagicSpells.getVolatileCodeHandler().setUnbreakable(item);
            }
            
            // empty enchant
            if(emptyEnchants) {
                item = MagicSpells.getVolatileCodeHandler().addFakeEnchantment(item);
            }
            
            // attributes
            if(config.contains("attributes")) {
                final Set<String> attrs = config.getConfigurationSection("attributes").getKeys(false);
                final String[] attrNames = new String[attrs.size()];
                final String[] attrTypes = new String[attrs.size()];
                final double[] attrAmounts = new double[attrs.size()];
                final int[] attrOperations = new int[attrs.size()];
                int i = 0;
                for(final String attrName : attrs) {
                    final String[] attrData = config.getString("attributes." + attrName).split(" ");
                    final String attrType = attrData[0];
                    double attrAmt = 1;
                    try {
                        attrAmt = Double.parseDouble(attrData[1]);
                    } catch(final NumberFormatException e) {
                        DebugHandler.debugNumberFormat(e);
                    }
                    int attrOp = 0; // add number
                    if(attrData.length > 2) {
                        if(attrData[2].toLowerCase().startsWith("mult")) {
                            attrOp = 1; // multiply percent
                        } else if(attrData[2].toLowerCase().contains("add") && attrData[2].toLowerCase().contains("perc")) {
                            attrOp = 2; // add percent
                        }
                    }
                    if(attrType != null) {
                        attrNames[i] = attrName;
                        attrTypes[i] = attrType;
                        attrAmounts[i] = attrAmt;
                        attrOperations[i] = attrOp;
                    }
                    i++;
                }
                item = MagicSpells.getVolatileCodeHandler().addAttributes(item, attrNames, attrTypes, attrAmounts, attrOperations);
            }
            
            return item;
        } catch(final Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void setLoreData(final ItemStack item, final String data) {
        final ItemMeta meta = item.getItemMeta();
        final List<String> lore;
        if(meta.hasLore()) {
            lore = meta.getLore();
            if(!lore.isEmpty()) {
                for(int i = 0; i < lore.size(); i++) {
                    final String s = ChatColor.stripColor(lore.get(i));
                    if(s.startsWith("MS$:")) {
                        lore.remove(i);
                        break;
                    }
                }
            }
        } else {
            lore = new ArrayList<>();
        }
        lore.add(ChatColor.BLACK + "" + ChatColor.MAGIC + "MS$:" + data);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    public static String getLoreData(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if(meta != null && meta.hasLore()) {
            final List<String> lore = meta.getLore();
            if(!lore.isEmpty()) {
                for(int i = 0; i < lore.size(); i++) {
                    final String s = ChatColor.stripColor(lore.get(lore.size() - 1));
                    if(s.startsWith("MS$:")) {
                        return s.substring(4);
                    }
                }
            }
        }
        return null;
    }
    
    public static void removeLoreData(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        final List<String> lore;
        if(meta.hasLore()) {
            lore = meta.getLore();
            if(!lore.isEmpty()) {
                boolean removed = false;
                for(int i = 0; i < lore.size(); i++) {
                    final String s = ChatColor.stripColor(lore.get(i));
                    if(s.startsWith("MS$:")) {
                        lore.remove(i);
                        removed = true;
                        break;
                    }
                }
                if(removed) {
                    if(!lore.isEmpty()) {
                        meta.setLore(lore);
                    } else {
                        meta.setLore(null);
                    }
                    item.setItemMeta(meta);
                }
            }
        }
    }
    
    public static EntityType getEntityType(final String type) {
        if(type.equalsIgnoreCase("player")) {
            return EntityType.PLAYER;
        }
        return entityTypeMap.get(type.toLowerCase());
    }
    
    public static PotionEffectType getPotionEffectType(final String type) {
        if(type.matches("^[0-9]+$")) {
            return PotionEffectType.getById(Integer.parseInt(type));
        } else {
            return PotionEffectType.getByName(type);
        }
    }
    
    public static Enchantment getEnchantmentType(final String type) {
        if(type.matches("^[0-9]+$")) {
            return Enchantment.getById(Integer.parseInt(type));
        } else {
            return Enchantment.getByName(type.toUpperCase());
        }
    }
    
    public static void sendFakeBlockChange(final Player player, final Block block, final MagicMaterial mat) {
        player.sendBlockChange(block.getLocation(), mat.getMaterial(), mat.getMaterialData().getData());
    }
    
    public static void restoreFakeBlockChange(final Player player, final Block block) {
        player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
    }
    
    public static void setFacing(final Player player, final Vector vector) {
        final Location loc = player.getLocation();
        setLocationFacingFromVector(loc, vector);
        player.teleport(loc);
    }
    
    public static void setLocationFacingFromVector(final Location location, final Vector vector) {
        final double yaw = getYawOfVector(vector);
        final double pitch = Math.toDegrees(-Math.asin(vector.getY()));
        location.setYaw((float) yaw);
        location.setPitch((float) pitch);
    }
    
    public static double getYawOfVector(final Vector vector) {
        return Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()));
    }
    
    public static boolean arrayContains(final int[] array, final int value) {
        for(final int i : array) {
            if(i == value) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean arrayContains(final String[] array, final String value) {
        for(final String i : array) {
            if(i.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean arrayContains(final Object[] array, final Object value) {
        for(final Object i : array) {
            if(i != null && i.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    public static String arrayJoin(final String[] array, final char with) {
        if(array == null || array.length == 0) {
            return "";
        }
        final int len = array.length;
        final StringBuilder sb = new StringBuilder(16 + len * 8);
        sb.append(array[0]);
        for(int i = 1; i < len; i++) {
            sb.append(with);
            sb.append(array[i]);
        }
        return sb.toString();
    }
    
    public static String listJoin(final List<String> list) {
        if(list == null || list.isEmpty()) {
            return "";
        }
        final int len = list.size();
        final StringBuilder sb = new StringBuilder(len * 12);
        sb.append(list.get(0));
        for(int i = 1; i < len; i++) {
            sb.append(' ');
            sb.append(list.get(i));
        }
        return sb.toString();
    }
    
    public static String[] splitParams(final String string, final int max) {
        final String[] words = string.trim().split(" ");
        if(words.length <= 1) {
            return words;
        }
        final ArrayList<String> list = new ArrayList<>();
        char quote = ' ';
        String building = "";
        
        for(final String word : words) {
            if(word.isEmpty()) {
                continue;
            }
            if(max > 0 && list.size() == max - 1) {
                if(!building.isEmpty()) {
                    building += " ";
                }
                building += word;
            } else if(quote == ' ') {
                if(word.length() == 1 || word.charAt(0) != '"' && word.charAt(0) != '\'') {
                    list.add(word);
                } else {
                    quote = word.charAt(0);
                    if(quote == word.charAt(word.length() - 1)) {
                        quote = ' ';
                        list.add(word.substring(1, word.length() - 1));
                    } else {
                        building = word.substring(1);
                    }
                }
            } else {
                if(word.charAt(word.length() - 1) == quote) {
                    list.add(building + ' ' + word.substring(0, word.length() - 1));
                    building = "";
                    quote = ' ';
                } else {
                    building += ' ' + word;
                }
            }
        }
        if(!building.isEmpty()) {
            list.add(building);
        }
        return list.toArray(new String[list.size()]);
    }
    
    public static String[] splitParams(final String string) {
        return splitParams(string, 0);
    }
    
    public static String[] splitParams(final String[] split, final int max) {
        return splitParams(arrayJoin(split, ' '), max);
    }
    
    public static String[] splitParams(final String[] split) {
        return splitParams(arrayJoin(split, ' '), 0);
    }
    
    public static List<String> tabCompleteSpellName(final CommandSender sender, final String partial) {
        final List<String> matches = new ArrayList<>();
        if(sender instanceof Player) {
            final Spellbook spellbook = MagicSpells.getSpellbook((Player) sender);
            for(final Spell spell : spellbook.getSpells()) {
                if(spellbook.canTeach(spell)) {
                    if(spell.getName().toLowerCase().startsWith(partial)) {
                        matches.add(spell.getName());
                    } else {
                        final String[] aliases = spell.getAliases();
                        if(aliases != null && aliases.length > 0) {
                            for(final String alias : aliases) {
                                if(alias.toLowerCase().startsWith(partial)) {
                                    matches.add(alias);
                                }
                            }
                        }
                    }
                }
            }
        } else if(sender.isOp()) {
            for(final Spell spell : MagicSpells.spells()) {
                if(spell.getName().toLowerCase().startsWith(partial)) {
                    matches.add(spell.getName());
                } else {
                    final String[] aliases = spell.getAliases();
                    if(aliases != null && aliases.length > 0) {
                        for(final String alias : aliases) {
                            if(alias.toLowerCase().startsWith(partial)) {
                                matches.add(alias);
                            }
                        }
                    }
                }
            }
        }
        if(!matches.isEmpty()) {
            return matches;
        }
        return null;
    }
    
    public static boolean removeFromInventory(final Inventory inventory, final ItemStack item) {
        int amt = item.getAmount();
        final ItemStack[] items = inventory.getContents();
        for(int i = 0; i < 36; i++) {
            if(items[i] != null && item.isSimilar(items[i])) {
                if(items[i].getAmount() > amt) {
                    items[i].setAmount(items[i].getAmount() - amt);
                    amt = 0;
                    break;
                } else if(items[i].getAmount() == amt) {
                    items[i] = null;
                    amt = 0;
                    break;
                } else {
                    amt -= items[i].getAmount();
                    items[i] = null;
                }
            }
        }
        if(amt == 0) {
            inventory.setContents(items);
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean addToInventory(final Inventory inventory, final ItemStack item, final boolean stackExisting, final boolean ignoreMaxStack) {
        int amt = item.getAmount();
        final ItemStack[] items = inventory.getContents();
        if(stackExisting) {
            for(int i = 0; i < 36; i++) {
                if(items[i] != null && item.isSimilar(items[i])) {
                    if(items[i].getAmount() + amt <= items[i].getMaxStackSize()) {
                        items[i].setAmount(items[i].getAmount() + amt);
                        amt = 0;
                        break;
                    } else {
                        final int diff = items[i].getMaxStackSize() - items[i].getAmount();
                        items[i].setAmount(items[i].getMaxStackSize());
                        amt -= diff;
                    }
                }
            }
        }
        if(amt > 0) {
            for(int i = 0; i < 36; i++) {
                if(items[i] == null) {
                    if(amt > item.getMaxStackSize() && !ignoreMaxStack) {
                        items[i] = item.clone();
                        items[i].setAmount(item.getMaxStackSize());
                        amt -= item.getMaxStackSize();
                    } else {
                        items[i] = item.clone();
                        items[i].setAmount(amt);
                        amt = 0;
                        break;
                    }
                }
            }
        }
        if(amt == 0) {
            inventory.setContents(items);
            return true;
        } else {
            return false;
        }
    }
    
    public static void rotateVector(final Vector v, final float degrees) {
        final double rad = Math.toRadians(degrees);
        final double sin = Math.sin(rad);
        final double cos = Math.cos(rad);
        final double x = v.getX() * cos - v.getZ() * sin;
        final double z = v.getX() * sin + v.getZ() * cos;
        v.setX(x);
        v.setZ(z);
    }
    
    public static Location applyRelativeOffset(final Location loc, final Vector relativeOffset) {
        return loc.add(VectorUtils.rotateVector(relativeOffset, loc));
    }
    
    public static Location applyAbsoluteOffset(final Location loc, final Vector offset) {
        return loc.add(offset);
    }
    
    public static Location applyOffsets(final Location loc, final Vector relativeOffset, final Vector absoluteOffset) {
        return applyAbsoluteOffset(applyRelativeOffset(loc, relativeOffset), absoluteOffset);
    }
    
    public static Location faceTarget(final Location origin, final Location target) {
        return origin.setDirection(getVectorToTarget(origin, target));
    }
    
    public static Vector getVectorToTarget(final Location origin, final Location target) {
        return target.toVector().subtract(origin.toVector());
    }
    
    public static boolean downloadFile(final String url, final File file) {
        try {
            final URL website = new URL(url);
            final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            final FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
            return true;
        } catch(final Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static void createFire(final Block block, final byte d) {
        block.setTypeIdAndData(Material.FIRE.getId(), d, false);
    }
    
    public static ItemStack getEggItemForEntityType(final EntityType type) {
        if(type == EntityType.LLAMA) {
            System.out.println("LLAMA");
            return new SpawnEgg(EntityType.LLAMA).toItemStack(1);
        }
        return new ItemStack(Material.MONSTER_EGG, 1, type.getTypeId());
    }
    
    public static String getStringNumber(final double number, final int places) {
        if(places < 0) {
            return number + "";
        }
        if(places == 0) {
            return (int) Math.round(number) + "";
        }
        final int x = (int) Math.pow(10, places);
        return (double) Math.round(number * x) / x + "";
    }
    
    public static String getUniqueId(final Player player) {
        final String uid = player.getUniqueId().toString().replace("-", "");
        uniqueIds.put(player.getName(), uid);
        return uid;
    }
    
    public static String getUniqueId(final String playerName) {
        if(uniqueIds.containsKey(playerName)) {
            return uniqueIds.get(playerName);
        }
        final Player player = Bukkit.getPlayerExact(playerName);
        if(player != null) {
            return getUniqueId(player);
        }
        return null;
    }
}
