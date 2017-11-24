package com.nisovin.magicspells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TxtUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.ExperienceUtils;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.IntMap;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MoneyHandler;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.variables.VariableManager;

import de.slikey.effectlib.Effect;

/**
 * Spell<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *         <th>
 *             Default
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>debug</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>false</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>name</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             (internal name)
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>aliases</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>helper-spell</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>false</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>always-granted</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>false</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>permission-name</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             (internal name)
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>incantations</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>description</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>cast-item</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>cast-items</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>right-click-cast-item</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>right-click-cast-items</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>consume-cast-item</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>consume-cast-items</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>left-click-cast-item</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>right-click-cast-item</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>bindable</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>true</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>bindable-items</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>experience</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>min-range</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>range</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>beneficial</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>false</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>target-damage-cause</code>
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>modifiers</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>target-modifiers</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>prerequisites</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>replaces</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>precludes</code>
 *         </td>
 *         <td>
 *             String List
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 * </table>
 */
public abstract class Spell implements Comparable<Spell>, Listener {

	private MagicConfig config;
	
	private boolean debug;
	protected String internalName;
	
	protected String name;
	
	protected String profilingKey;
	
	protected String[] aliases;
	
	protected boolean helperSpell;
	
	protected boolean alwaysGranted;
	
	protected String permName;
	
	protected List<String> incantations;
	
	protected String description;
	
	protected CastItem[] castItems;
	
	protected CastItem[] rightClickCastItems;
	
	protected CastItem[] consumeCastItems;
	
	protected boolean castWithLeftClick;
	
	protected boolean castWithRightClick;
	
	protected String danceCastSequence;
	
	protected boolean requireCastItemOnCommand;
	
	protected boolean bindable;
	
	protected HashSet<CastItem> bindableItems;
	
	protected ItemStack spellIcon;
	
	protected int broadcastRange;
	
	protected int experience;
	
	protected EnumMap<EffectPosition, List<SpellEffect>> effects;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;
	
	protected int minRange;
	
	protected int range;
	
	protected boolean spellPowerAffectsRange;
	
	protected boolean obeyLos;
	
	protected ValidTargetList validTargetList;
	
	protected boolean beneficial;
	
	private DamageCause targetDamageCause;
	
	private double targetDamageAmount;
	
	protected HashSet<Material> losTransparentBlocks;

	protected int castTime;
	
	protected boolean interruptOnMove;
	
	protected boolean interruptOnTeleport;
	
	protected boolean interruptOnDamage;
	
	protected boolean interruptOnCast;
	
	protected String spellNameOnInterrupt;
	
	protected Spell spellOnInterrupt;
	
	protected SpellReagents reagents;
	
	protected float cooldown;
	protected float serverCooldown;
	protected List<String> rawSharedCooldowns;
	protected HashMap<Spell, Float> sharedCooldowns;
	
	protected boolean ignoreGlobalCooldown;
	
	protected int charges;
	
	protected String rechargeSound;

	private List<String> modifierStrings;
	
	private List<String> targetModifierStrings;
	
	protected ModifierSet modifiers;
	protected ModifierSet targetModifiers;
	
	protected List<String> prerequisites;
	
	protected List<String> replaces;
	
	protected List<String> precludes;
	
	protected Map<String, Integer> xpGranted;
	
	protected Map<String, Integer> xpRequired;
	
	protected List<String> worldRestrictions;
	
	protected Map<String, VariableMod> variableModsCast;
	
	protected Map<String, VariableMod> variableModsCasted;
	
	protected Map<String, VariableMod> variableModsTarget;
	
	protected String soundOnCooldown;
	protected String soundMissingReagents;
	
	protected String strCost;
	
	protected String strCastSelf;
	
	protected String strCastOthers;
	
	protected String strOnCooldown;
	
	protected String strMissingReagents;
	
	protected String strCantCast;
	
	protected String strCantBind;
	
	protected String strWrongWorld;
	
	protected String strWrongCastItem;
	
	protected String strCastStart;
	
	protected String strInterrupted;
	
	protected String strModifierFailed;
	
	protected String strXpAutoLearned;
	
	private HashMap<String, Long> nextCast;
	IntMap<String> chargesConsumed;
	private long nextCastServer;
	
	private Set<String> tags;
	
	public Spell(MagicConfig config, String spellName) {
		this.config = config;
		
		this.internalName = spellName;
		callbacks = new HashMap<>();
		loadConfigData(config, spellName, "spells");
	}
	
	protected void loadConfigData(MagicConfig config, String spellName, String section) {
		this.debug = config.getBoolean(section + '.' + spellName + ".debug", false);
		this.profilingKey = "Spell:" + this.getClass().getName().replace("com.nisovin.magicspells.spells.", "") + '-' + spellName;
		this.name = config.getString(section + '.' + spellName + ".name", spellName);
		List<String> temp = config.getStringList(section + '.' + spellName + ".aliases", null);
		if (temp != null) {
			aliases = new String[temp.size()];
			aliases = temp.toArray(aliases);
		}
		this.helperSpell = config.getBoolean(section + '.' + spellName + ".helper-spell", false);
		this.alwaysGranted = config.getBoolean(section + '.' + spellName + ".always-granted", false);
		this.permName = config.getString(section + '.' + spellName + ".permission-name", spellName);
		this.incantations = config.getStringList(section + '.' + spellName + ".incantations", null);
		
		// General options
		this.description = config.getString(section + '.' + spellName + ".description", "");
		if (config.contains(section + '.' + spellName + ".cast-item")) {
			String[] sItems = config.getString(section + '.' + spellName + ".cast-item", "-5").trim().replace(" ", "").split(",");
			this.castItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				this.castItems[i] = new CastItem(is);
			}
		} else if (config.contains(section + '.' + spellName + ".cast-items")) {
			List<String> sItems = config.getStringList(section + '.' + spellName + ".cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			this.castItems = new CastItem[sItems.size()];
			for (int i = 0; i < this.castItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				this.castItems[i] = new CastItem(is);
			}
		} else {
			this.castItems = new CastItem[0];
		}
		if (config.contains(section + '.' + spellName + ".right-click-cast-item")) {
			String[] sItems = config.getString(section + '.' + spellName + ".right-click-cast-item", "-5").trim().replace(" ", "").split(",");
			this.rightClickCastItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				this.rightClickCastItems[i] = new CastItem(is);
			}
		} else if (config.contains(section + '.' + spellName + ".right-click-cast-items")) {
			List<String> sItems = config.getStringList(section + '.' + spellName + ".right-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			this.rightClickCastItems = new CastItem[sItems.size()];
			for (int i = 0; i < rightClickCastItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				this.rightClickCastItems[i] = new CastItem(is);
			}
		} else {
			this.rightClickCastItems = new CastItem[0];
		}
		if (config.contains(section + '.' + spellName + ".consume-cast-item")) {
			String[] sItems = config.getString(section + '.' + spellName + ".consume-cast-item", "-5").trim().replace(" ", "").split(",");
			this.consumeCastItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				this.consumeCastItems[i] = new CastItem(is);
			}
		} else if (config.contains(section + '.' + spellName + ".consume-cast-items")) {
			List<String> sItems = config.getStringList(section + '.' + spellName + ".consume-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			this.consumeCastItems = new CastItem[sItems.size()];
			for (int i = 0; i < this.consumeCastItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				this.consumeCastItems[i] = new CastItem(is);
			}
		} else {
			this.consumeCastItems = new CastItem[0];
		}
		this.castWithLeftClick = config.getBoolean(section + '.' + spellName + ".cast-with-left-click", MagicSpells.plugin.castWithLeftClick);
		this.castWithRightClick = config.getBoolean(section + '.' + spellName + ".cast-with-right-click", MagicSpells.plugin.castWithRightClick);
		this.danceCastSequence = config.getString(section + '.' + spellName + ".dance-cast-sequence", null);
		this.requireCastItemOnCommand = config.getBoolean(section + '.' + spellName + ".require-cast-item-on-command", false);
		this.bindable = config.getBoolean(section + '.' + spellName + ".bindable", true);
		List<String> bindables = config.getStringList(section + '.' + spellName + ".bindable-items", null);
		if (bindables != null) {
			this.bindableItems = new HashSet<>();
			for (String s : bindables) {
				ItemStack is = Util.getItemStackFromString(s);
				if (is == null) continue;
				this.bindableItems.add(new CastItem(is));
			}
		}
		String icontemp = config.getString(section + '.' + spellName + ".spell-icon", null);
		if (icontemp == null) {
			this.spellIcon = null;
		} else {
			this.spellIcon = Util.getItemStackFromString(icontemp);
			if (this.spellIcon != null && this.spellIcon.getType() != Material.AIR) {
				this.spellIcon.setAmount(0);
				if (!icontemp.contains("|")) {
					ItemMeta iconMeta = this.spellIcon.getItemMeta();
					iconMeta.setDisplayName(MagicSpells.getTextColor() + this.name);
					this.spellIcon.setItemMeta(iconMeta);
				}
			}
		}
		this.broadcastRange = config.getInt(section + '.' + spellName + ".broadcast-range", MagicSpells.plugin.broadcastRange);
		this.experience = config.getInt(section + '.' + spellName + ".experience", 0);

		// Cast time
		this.castTime = config.getInt(section + '.' + spellName + ".cast-time", 0);
		this.interruptOnMove = config.getBoolean(section + '.' + spellName + ".interrupt-on-move", true);
		this.interruptOnTeleport = config.getBoolean(section + '.' + spellName + ".interrupt-on-teleport", true);
		this.interruptOnDamage = config.getBoolean(section + '.' + spellName + ".interrupt-on-damage", false);
		this.interruptOnCast = config.getBoolean(section + '.' + spellName + ".interrupt-on-cast", true);
		this.spellNameOnInterrupt = config.getString(section + '.' + spellName + ".spell-on-interrupt", null);
		
		// Targeting
		this.minRange = config.getInt(section + '.' + spellName + ".min-range", 0);
		this.range = config.getInt(section + '.' + spellName + ".range", 20);
		this.spellPowerAffectsRange = config.getBoolean(section + '.' + spellName + ".spell-power-affects-range", false);
		this.obeyLos = config.getBoolean(section + '.' + spellName + ".obey-los", true);
		if (config.contains(section + '.' + spellName + ".can-target")) {
			if (config.isList(section + '.' + spellName + ".can-target")) {
				this.validTargetList = new ValidTargetList(this, config.getStringList(section + '.' + spellName + ".can-target", null));
			} else {
				this.validTargetList = new ValidTargetList(this, config.getString(section + '.' + spellName + ".can-target", ""));
			}
		} else {
			boolean targetPlayers = config.getBoolean(section + '.' + spellName + ".target-players", true);
			boolean targetNonPlayers = config.getBoolean(section + '.' + spellName + ".target-non-players", true);
			this.validTargetList = new ValidTargetList(targetPlayers, targetNonPlayers);
		}
		this.beneficial = config.getBoolean(section + '.' + spellName + ".beneficial", isBeneficialDefault());
		this.targetDamageCause = null;
		String causeStr = config.getString(section + '.' + spellName + ".target-damage-cause", null);
		if (causeStr != null) {
			for (DamageCause cause : DamageCause.values()) {
				if (cause.name().equalsIgnoreCase(causeStr)) {
					this.targetDamageCause = cause;
					break;
				}
			}
		}
		this.targetDamageAmount = config.getDouble(section + '.' + spellName + ".target-damage-amount", 0);
		this.losTransparentBlocks = MagicSpells.getTransparentBlocks();
		if (config.contains(section + '.' + spellName + ".los-transparent-blocks")) {
			this.losTransparentBlocks = Util.getMaterialList(config.getStringList(section + '.' + spellName + ".los-transparent-blocks", Collections.emptyList()), HashSet::new);
			this.losTransparentBlocks.add(Material.AIR);
		}
		
		// Graphical effects
		if (config.contains(section + '.' + spellName + ".effects")) {
			this.effects = new EnumMap<>(EffectPosition.class);
			if (config.isList(section + '.' + spellName + ".effects")) {
				List<String> effectsList = config.getStringList(section + '.' + spellName + ".effects", null);
				if (effectsList != null) {
					for (Object obj : effectsList) {
						if (obj instanceof String) {
							String eff = (String)obj;
							String[] data = eff.split(" ", 3);
							EffectPosition pos = EffectPosition.getPositionFromString(data[0]);
							if (pos != null) {
								SpellEffect effect = SpellEffect.createNewEffectByName(data[1]);
								if (effect != null) {
									effect.loadFromString(data.length > 2 ? data[2] : null);
									List<SpellEffect> e = this.effects.computeIfAbsent(pos, p -> new ArrayList<>());
									e.add(effect);
								}
							}
						}
					}
				}
			} else if (config.isSection(section + '.' + spellName + ".effects")) {
				for (String key : config.getKeys(section + '.' + spellName + ".effects")) {
					ConfigurationSection effConf = config.getSection(section + '.' + spellName + ".effects." + key);
					EffectPosition pos = EffectPosition.getPositionFromString(effConf.getString("position", ""));
					if (pos != null) {
						SpellEffect effect = SpellEffect.createNewEffectByName(effConf.getString("effect", ""));
						if (effect != null) {
							effect.loadFromConfiguration(effConf);
							List<SpellEffect> e = this.effects.computeIfAbsent(pos, p -> new ArrayList<>());
							e.add(effect);
						}
					}
				}
			}
		}
		
		//TODO load the fast mapping for effects here
		
		// Cost
		this.reagents = getConfigReagents("cost");
		if (this.reagents == null) this.reagents = new SpellReagents();
		
		// Cooldowns
		this.cooldown = (float)config.getDouble(section + '.' + spellName + ".cooldown", 0);
		this.serverCooldown = (float)config.getDouble(section + '.' + spellName + ".server-cooldown", 0);
		this.rawSharedCooldowns = config.getStringList(section + '.' + spellName + ".shared-cooldowns", null);
		this.ignoreGlobalCooldown = config.getBoolean(section + '.' + spellName + ".ignore-global-cooldown", false);
		this.charges = config.getInt(section + '.' + spellName + ".charges", 0);
		this.rechargeSound = config.getString(section + '.' + spellName + ".recharge-sound", "");
		this.nextCast = new HashMap<>();
		this.chargesConsumed = new IntMap<>();
		this.nextCastServer = 0;

		// Modifiers
		this.modifierStrings = config.getStringList(section + '.' + spellName + ".modifiers", null);
		this.targetModifierStrings = config.getStringList(section + '.' + spellName + ".target-modifiers", null);
		
		// Hierarchy options
		this.prerequisites = config.getStringList(section + '.' + spellName + ".prerequisites", null);
		this.replaces = config.getStringList(section + '.' + spellName + ".replaces", null);
		this.precludes = config.getStringList(section + '.' + spellName + ".precludes", null);
		this.worldRestrictions = config.getStringList(section + '.' + spellName + ".restrict-to-worlds", null);
		List<String> sXpGranted = config.getStringList(section + '.' + spellName + ".xp-granted", null);
		List<String> sXpRequired = config.getStringList(section + '.' + spellName + ".xp-required", null);
		if (sXpGranted != null) {
			this.xpGranted = new LinkedHashMap<>();
			for (String s : sXpGranted) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					this.xpGranted.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-granted entry for spell '" + this.internalName + "': " + s);
				}
			}
		}
		if (sXpRequired != null) {
			this.xpRequired = new LinkedHashMap<>();
			for (String s : sXpRequired) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					this.xpRequired.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-required entry for spell '" + this.internalName + "': " + s);
				}
			}
		}
		
		// Variable options
		List<String> varModsCast = config.getStringList(section + '.' + spellName + ".variable-mods-cast", null);
		if (varModsCast != null && !varModsCast.isEmpty()) {
			this.variableModsCast = new HashMap<>();
			for (String s : varModsCast) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					this.variableModsCast.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-cast option for spell '" + spellName + "': " + s);
				}
			}
		}
		List<String> varModsCasted = config.getStringList(section + '.' + spellName + ".variable-mods-casted", null);
		if (varModsCasted != null && !varModsCasted.isEmpty()) {
			this.variableModsCasted = new HashMap<>();
			for (String s : varModsCasted) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					this.variableModsCasted.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-casted option for spell '" + spellName + "': " + s);
				}
			}
		}
		List<String> varModsTarget = config.getStringList(section + '.' + spellName + ".variable-mods-target", null);
		if (varModsTarget != null && !varModsTarget.isEmpty()) {
			this.variableModsTarget = new HashMap<>();
			for (String s : varModsTarget) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					this.variableModsTarget.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-target option for spell '" + spellName + "': " + s);
				}
			}
		}

		this.soundOnCooldown = config.getString(section + '.' + spellName + ".sound-on-cooldown", MagicSpells.plugin.soundFailOnCooldown);
		if (this.soundOnCooldown != null && this.soundOnCooldown.isEmpty()) this.soundOnCooldown = null;
		this.soundMissingReagents = config.getString(section + '.' + spellName + ".sound-missing-reagents", MagicSpells.plugin.soundFailMissingReagents);
		if (this.soundMissingReagents != null && this.soundMissingReagents.isEmpty()) this.soundMissingReagents = null;
		
		// Strings
		this.strCost = config.getString(section + '.' + spellName + ".str-cost", null);
		this.strCastSelf = config.getString(section + '.' + spellName + ".str-cast-self", null);
		this.strCastOthers = config.getString(section + '.' + spellName + ".str-cast-others", null);
		this.strOnCooldown = config.getString(section + '.' + spellName + ".str-on-cooldown", MagicSpells.plugin.strOnCooldown);
		this.strMissingReagents = config.getString(section + '.' + spellName + ".str-missing-reagents", MagicSpells.plugin.strMissingReagents);
		this.strCantCast = config.getString(section + '.' + spellName + ".str-cant-cast", MagicSpells.plugin.strCantCast);
		this.strCantBind = config.getString(section + '.' + spellName + ".str-cant-bind", null);
		this.strWrongWorld = config.getString(section + '.' + spellName + ".str-wrong-world", MagicSpells.plugin.strWrongWorld);
		this.strWrongCastItem = config.getString(section + '.' + spellName + ".str-wrong-cast-item", this.strCantCast);
		this.strCastStart = config.getString(section + '.' + spellName + ".str-cast-start", null);
		this.strInterrupted = config.getString(section + '.' + spellName + ".str-interrupted", null);
		this.strModifierFailed = config.getString(section + '.' + spellName + ".str-modifier-failed", null);
		this.strXpAutoLearned = config.getString(section + '.' + spellName + ".str-xp-auto-learned", MagicSpells.plugin.strXpAutoLearned);
		if (this.strXpAutoLearned != null) this.strXpAutoLearned = this.strXpAutoLearned.replace("%s", this.name);
		
		this.tags = new HashSet<>(config.getStringList(section + '.' + spellName + ".tags", new ArrayList<>()));
		this.tags.add("spell-class:" + this.getClass().getCanonicalName());
		this.tags.add("spell-package:" + this.getClass().getPackage().getName());
	}
	
	public Set<String> getTags() {
		return Collections.unmodifiableSet(this.tags);
	}
	
	public boolean hasTag(String tag) {
		return this.tags.contains(tag);
	}
	
	public String getLoggingSpellPrefix() {
		return '[' + this.internalName + ']';
	}
	
	protected SpellReagents getConfigReagents(String option) {
		SpellReagents reagents = null;
		List<String> costList = config.getStringList("spells." + this.internalName + '.' + option, null);
		if (costList != null && !costList.isEmpty()) {
			reagents = new SpellReagents();
			String[] data;
			for (int i = 0; i < costList.size(); i++) {
				String costVal = costList.get(i);
				
				try {
					// Parse cost data
					data = costVal.split(" ");
					if (data[0].equalsIgnoreCase("health")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setHealth(amt);
					} else if (data[0].equalsIgnoreCase("mana")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setMana(amt);
					} else if (data[0].equalsIgnoreCase("hunger")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setHunger(amt);
					} else if (data[0].equalsIgnoreCase("experience")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setExperience(amt);
					} else if (data[0].equalsIgnoreCase("levels")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setLevels(amt);
					} else if (data[0].equalsIgnoreCase("durability")) {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setDurability(amt);
					} else if (data[0].equalsIgnoreCase("money")) {
						float amt = 1;
						if (data.length > 1) amt = Float.parseFloat(data[1]);
						reagents.setMoney(amt);
					} else if (data[0].equalsIgnoreCase("variable")) {
						reagents.addVariable(data[1], Double.parseDouble(data[2]));
					} else {
						int amt = 1;
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						ItemStack is = Util.getItemStackFromString(data[0]);
						if (is != null) {
							is.setAmount(amt);
							reagents.addItem(is);
						} else {
							MagicSpells.error("Failed to process cost value for " + this.internalName + " spell: " + costVal);
						}
					}
				} catch (Exception e) {
					// FIXME this should not be a means of breaking
					MagicSpells.error("Failed to process cost value for " + this.internalName + " spell: " + costVal);
				}
			}
		}
		return reagents;
	}
	
	// DEBUG INFO: level 2, adding modifiers to internalname
	// DEBUG INFO: level 2, adding target modifiers to internalname
	/**
	 * This method is called immediately after all spells have been loaded.
	 */
	protected void initialize() {
		// Modifiers
		if (this.modifierStrings != null && !this.modifierStrings.isEmpty()) {
			debug(2, "Adding modifiers to " + this.internalName + " spell");
			this.modifiers = new ModifierSet(this.modifierStrings);
			this.modifierStrings = null;
		}
		if (this.targetModifierStrings != null && !this.targetModifierStrings.isEmpty()) {
			debug(2, "Adding target modifiers to " + this.internalName + " spell");
			this.targetModifiers = new ModifierSet(this.targetModifierStrings);
			this.targetModifierStrings = null;
		}
		
		// Process shared cooldowns
		if (this.rawSharedCooldowns != null) {
			this.sharedCooldowns = new HashMap<>();
			for (String s : this.rawSharedCooldowns) {
				String[] data = s.split(" ");
				Spell spell = MagicSpells.getSpellByInternalName(data[0]);
				float cd = Float.parseFloat(data[1]);
				if (spell != null) {
					this.sharedCooldowns.put(spell, cd);
				}
			}
			this.rawSharedCooldowns.clear();
			this.rawSharedCooldowns = null;
		}
		
		// Register events
		registerEvents();
		
		// Other processing
		if (this.spellNameOnInterrupt != null && !this.spellNameOnInterrupt.isEmpty()) {
			this.spellOnInterrupt = MagicSpells.getSpellByInternalName(this.spellNameOnInterrupt);
		}
	}
	
	protected boolean configKeyExists(String key) {
		return this.config.contains("spells." + this.internalName + '.' + key);
	}
	
	/**
	 * Access an integer config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int defaultValue) {
		return this.config.getInt("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	/**
	 * Access a long config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected long getConfigLong(String key, long defaultValue) {
		return this.config.getLong("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	/**
	 * Access a boolean config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean defaultValue) {
		return this.config.getBoolean("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	/**
	 * Access a String config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String defaultValue) {
		return this.config.getString("spells." + this.internalName + '.' + key, defaultValue);
	}
		
	/**
	 * Access a Vector config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected Vector getConfigVector(String key, String defaultValue) {
		String[] vecStrings = getConfigString(key, defaultValue).split(",");
		return new Vector(Double.parseDouble(vecStrings[0]), Double.parseDouble(vecStrings[1]), Double.parseDouble(vecStrings[2]));
	}
	
	/**
	 * Access a float config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float defaultValue) {
		return (float)this.config.getDouble("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	/**
	 * Access a double config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected double getConfigDouble(String key, double defaultValue) {
		return this.config.getDouble("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	protected List<Integer> getConfigIntList(String key, List<Integer> defaultValue) {
		return this.config.getIntList("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	protected List<String> getConfigStringList(String key, List<String> defaultValue) {
		return this.config.getStringList("spells." + this.internalName + '.' + key, defaultValue);
	}
	
	protected Set<String> getConfigKeys(String key) {
		return this.config.getKeys("spells." + this.internalName + '.' + key);
	}
	
	protected ConfigurationSection getConfigSection(String key) {
		return this.config.getSection("spells." + this.internalName + '.' + key);
	}
	
	protected boolean isConfigString(String key) {
		return this.config.isString("spells." + this.internalName + '.' + key);
	}
	
	protected boolean isConfigSection(String key) {
		return this.config.isSection("spells." + this.internalName + '.' + key);
	}

	public final SpellCastResult cast(Player player) {
		return cast(player, 1.0F, null);
	}
	
	// TODO can this safely be made varargs?
	public final SpellCastResult cast(Player player, String[] args) {
		return cast(player, 1.0F, args);
	}
	
	// TODO can this safely be made varargs?
	public final SpellCastResult cast(Player player, float power, String[] args) {
		SpellCastEvent spellCast = preCast(player, power, args);
		if (spellCast == null) return new SpellCastResult(SpellCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY);
		PostCastAction action;
		int castTime = spellCast.getCastTime();
		if (castTime <= 0 || spellCast.getSpellCastState() != SpellCastState.NORMAL) {
			action = handleCast(spellCast);
		} else if (!preCastTimeCheck(player, args)) {
			action = PostCastAction.ALREADY_HANDLED;
		} else {
			action = PostCastAction.DELAYED;
			sendMessage(this.strCastStart, player, args);
			playSpellEffects(EffectPosition.START_CAST, player);
			if (MagicSpells.plugin.useExpBarAsCastTimeBar) {
				new DelayedSpellCastWithBar(spellCast);
			} else {
				new DelayedSpellCast(spellCast);
			}
		}
		return new SpellCastResult(spellCast.getSpellCastState(), action);
	}
	
	protected SpellCastState getCastState(Player player) {
		if (!MagicSpells.getSpellbook(player).canCast(this)) return SpellCastState.CANT_CAST;
		if (this.worldRestrictions != null && !this.worldRestrictions.contains(player.getWorld().getName())) return SpellCastState.WRONG_WORLD;
		if (MagicSpells.plugin.noMagicZones != null && MagicSpells.plugin.noMagicZones.willFizzle(player, this)) return SpellCastState.NO_MAGIC_ZONE;
		if (onCooldown(player)) return SpellCastState.ON_COOLDOWN;
		if (!hasReagents(player)) return SpellCastState.MISSING_REAGENTS;
		return SpellCastState.NORMAL;
	}
	
	// TODO can this safely be made varargs?
	// DEBUG INFO: level 2, spell cast state
	// DEBUG INFO: level 2, spell canceled
	// DEBUG INFO: level 2, spell cast state changed
	protected SpellCastEvent preCast(Player player, float power, String[] args) {
		// Get spell state
		SpellCastState state = getCastState(player);
		debug(2, "    Spell cast state: " + state);
		
		// Call events
		SpellCastEvent event = new SpellCastEvent(this, player, state, power, args, this.cooldown, this.reagents.clone(), this.castTime);
		EventUtil.call(event);
		if (event.isCancelled()) {
			debug(2, "    Spell canceled");
			return null;
		} else {
			if (event.haveReagentsChanged()) {
				boolean hasReagents = hasReagents(player, event.getReagents());
				if (!hasReagents && state != SpellCastState.MISSING_REAGENTS) {
					event.setSpellCastState(SpellCastState.MISSING_REAGENTS);
					debug(2, "    Spell cast state changed: " + state);
				} else if (hasReagents && state == SpellCastState.MISSING_REAGENTS) {
					event.setSpellCastState(state = SpellCastState.NORMAL);
					debug(2, "    Spell cast state changed: " + state);
				}
			}
			if (event.hasSpellCastStateChanged()) {
				debug(2, "    Spell cast state changed: " + state);
			}
		}
		if (Perm.NOCASTTIME.has(player)) {
			event.setCastTime(0);
		}
		
		return event;
	}
	
	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	PostCastAction handleCast(SpellCastEvent spellCast) {
		long start = System.nanoTime();
		Player player = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		String[] args = spellCast.getSpellArgs();
		float power = spellCast.getPower();
		debug(3, "    Power: " + power);
		debug(3, "    Cooldown: " + this.cooldown);
		if (MagicSpells.plugin.debug && args != null && args.length > 0) {
			debug(3, "    Args: {" + Util.arrayJoin(args, ',') + '}');
		}
		PostCastAction action = castSpell(player, state, power, args);
		if (MagicSpells.plugin.enableProfiling) {
        	Long total = MagicSpells.plugin.profilingTotalTime.get(this.profilingKey);
        	if (total == null) total = (long)0;
        	total += System.nanoTime() - start;
        	MagicSpells.plugin.profilingTotalTime.put(this.profilingKey, total);
        	Integer runs = MagicSpells.plugin.profilingRuns.get(this.profilingKey);
        	if (runs == null) runs = 0;
        	runs += 1;
        	MagicSpells.plugin.profilingRuns.put(this.profilingKey, runs);
		}
		postCast(spellCast, action);
		return action;
	}
	
	// FIXME save the results of the redundant calculations or be cleaner about it
	// DEBUG INFO: level 3, post cast action actionName
	protected void postCast(SpellCastEvent spellCast, PostCastAction action) {
		debug(3, "    Post-cast action: " + action);
		Player player = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		if (action != null && action != PostCastAction.ALREADY_HANDLED) {
			if (state == SpellCastState.NORMAL) {
				if (action.setCooldown()) setCooldown(player, spellCast.getCooldown());
				if (action.chargeReagents()) removeReagents(player, spellCast.getReagents());
				if (action.sendMessages()) sendMessages(player, spellCast.getSpellArgs());
				if (this.experience > 0) player.giveExp(this.experience);
			} else if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(player)) + ""), player, spellCast.getSpellArgs());
				if (this.soundOnCooldown != null) MagicSpells.getVolatileCodeHandler().playSound(player, this.soundOnCooldown, 1F, 1F);
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(strMissingReagents, player, spellCast.getSpellArgs());
				if (MagicSpells.plugin.showStrCostOnMissingReagents && this.strCost != null && !this.strCost.isEmpty()) MagicSpells.sendMessage("    (" + this.strCost + ')', player, spellCast.getSpellArgs());
				if (this.soundMissingReagents != null) MagicSpells.getVolatileCodeHandler().playSound(player, this.soundMissingReagents, 1F, 1F);
			} else if (state == SpellCastState.CANT_CAST) {
				MagicSpells.sendMessage(strCantCast, player, spellCast.getSpellArgs());
			} else if (state == SpellCastState.NO_MAGIC_ZONE) {
				MagicSpells.plugin.noMagicZones.sendNoMagicMessage(player, this);
			} else if (state == SpellCastState.WRONG_WORLD) {
				MagicSpells.sendMessage(this.strWrongWorld, player, spellCast.getSpellArgs());
			}
		}
		SpellCastedEvent event = new SpellCastedEvent(this, player, state, spellCast.getPower(), spellCast.getSpellArgs(), this.cooldown, this.reagents, action);
		EventUtil.call(event);
	}
	
	// TODO can this safely be made varargs?
	public void sendMessages(Player player, String[] args) {
		sendMessage(formatMessage(this.strCastSelf, "%a", player.getDisplayName()), player, args);
		sendMessageNear(player, formatMessage(this.strCastOthers, "%a", player.getDisplayName()));
	}
	
	// TODO can this safely be made varargs?
	protected boolean preCastTimeCheck(Player player, String[] args) {
		return true;
	}
	
	// TODO can this safely be made varargs?
	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 * @param player the player casting the spell
	 * @param state the state of the spell cast (normal, on cooldown, missing reagents, etc)
	 * @param power the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	public abstract PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args);
		
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	protected List<String> tabCompletePlayerName(CommandSender sender, String partial) {
		ArrayList<String> matches = new ArrayList<>();
		partial = partial.toLowerCase();
		// TODO stream this
		for (Player p : Bukkit.getOnlinePlayers()) {
			String name = p.getName();
			if (!name.toLowerCase().startsWith(partial)) continue;
			if (sender.isOp() || !(sender instanceof Player) || ((Player)sender).canSee(p)) matches.add(name);
		}
		if (!matches.isEmpty()) return matches;
		return null;
	}
	
	protected List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		return TxtUtil.tabCompleteSpellName(sender, partial);
	}
	
	// TODO can this safely be made varargs?
	/**
	 * This method is called when the spell is cast from the console.
	 * @param sender the console sender.
	 * @param args the command arguments
	 * @return true if the spell was handled, false otherwise
	 */
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	public abstract boolean canCastWithItem();
	
	public abstract boolean canCastByCommand();
	
	public boolean canCastWithLeftClick() {
		return this.castWithLeftClick;
	}
	
	public boolean canCastWithRightClick() {
		return this.castWithRightClick;
	}
	
	public boolean isAlwaysGranted() {
		return this.alwaysGranted;
	}
	
	public boolean isValidItemForCastCommand(ItemStack item) {
		if (!this.requireCastItemOnCommand || this.castItems == null) return true;
		if (item == null && this.castItems.length == 1 && this.castItems[0].getItemTypeId() == 0) return true;
		for (CastItem castItem : this.castItems) {
			if (castItem.equals(item)) return true;
		}
		return false;
	}
	
	public boolean canBind(CastItem item) {
		if (!this.bindable) return false;
		if (this.bindableItems == null) return true;
		return this.bindableItems.contains(item);
	}
	
	public ItemStack getSpellIcon() {
		return this.spellIcon;
	}
	
	public String getCostStr() {
		if (this.strCost == null || this.strCost.isEmpty()) return null;
		return this.strCost;
	}
	
	/**
	 * Check whether this spell is currently on cooldown for the specified player
	 * @param player The player to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(Player player) {
		if (Perm.NOCOOLDOWN.has(player)) return false;
		
		if (this.charges > 0) return this.chargesConsumed.get(player.getName()) >= this.charges;
		
		if (this.serverCooldown > 0 && this.nextCastServer > System.currentTimeMillis()) return true;
		
		Long next = this.nextCast.get(player.getName());
		if (next != null) {
			if (next > System.currentTimeMillis()) return true;
		}
		return false;
	}
	
	public float getCooldown() {
		return this.cooldown;
	}
	
	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 * @param player The player to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public float getCooldown(Player player) {
		if (this.charges > 0) return -1;
		
		float cd = 0;
		
		Long next = this.nextCast.get(player.getName());
		if (next != null) {
			float c = (next - System.currentTimeMillis()) / ((float)TimeUtil.MILLISECONDS_PER_SECOND);
			cd =  c > 0 ? c : 0;
		}
		
		if (this.serverCooldown > 0 && this.nextCastServer > System.currentTimeMillis()) {
			float c = (this.nextCastServer - System.currentTimeMillis()) / ((float)TimeUtil.MILLISECONDS_PER_SECOND);
			if (c > cd) cd = c;
		}
		
		return cd;
	}
	
	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(Player player, float cooldown) {
		setCooldown(player, cooldown, true);
	}
	
	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(final Player player, float cooldown, boolean activateSharedCooldowns) {
		if (cooldown > 0) {
			if (this.charges <= 0) {
				this.nextCast.put(player.getName(), System.currentTimeMillis() + (long)(cooldown * TimeUtil.MILLISECONDS_PER_SECOND));
			} else {
				final String name = player.getName();
				this.chargesConsumed.increment(name);
				// TODO convert this to lambda
				MagicSpells.scheduleDelayedTask(new Runnable() {
					
					@Override
					public void run() {
						chargesConsumed.decrement(name);
						if (rechargeSound == null) return;
						if (rechargeSound.isEmpty()) return;
						MagicSpells.getVolatileCodeHandler().playSound(player, rechargeSound, 1.0F, 1.0F);
					}
					
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cooldown));
			}
		} else {
			if (this.charges <= 0) {
				this.nextCast.remove(player.getName());
			} else {
				this.chargesConsumed.remove(player.getName());
			}
		}
		if (this.serverCooldown > 0) {
			this.nextCastServer = System.currentTimeMillis() + (long)(this.serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
		}
		if (activateSharedCooldowns && this.sharedCooldowns != null) {
			for (Map.Entry<Spell, Float> scd : this.sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(player, scd.getValue(), false);
			}
		}
	}
	
	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param player the player to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player) {
		return hasReagents(player, this.reagents);
	}
	
	// FIXME this doesn't seem strictly tied to Spell logic, could probably be moved
	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param player the player to check
	 * @param reagents the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player, SpellReagents reagents) {
		if (reagents == null) return true;
		return hasReagents(player, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}
	
	/**
	 * Checks if a player has the specified reagents, including health and mana
	 * @param player the player to check
	 * @param reagents the inventory item reagents to look for
	 * @param healthCost the health cost, in half-hearts
	 * @param manaCost the mana cost
	 * @return true if the player has all the reagents, false otherwise
	 */
	private boolean hasReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		// Is the player exempt from reagent costs?
		if (Perm.NOREAGENTS.has(player)) return true;
		
		// Health costs
		if (healthCost > 0 && player.getHealth() <= healthCost) return false;
		
		// Mana costs
		if (manaCost > 0 && (MagicSpells.plugin.mana == null || !MagicSpells.plugin.mana.hasMana(player, manaCost))) return false;
		
		// Hunger costs
		if (hungerCost > 0 && player.getFoodLevel() < hungerCost) return false;
		
		// Experience costs
		if (experienceCost > 0 && !ExperienceUtils.hasExp(player, experienceCost)) return false;
		
		// Level costs
		if (levelsCost > 0 && player.getLevel() < levelsCost) return false;
		
		// Durabilty costs
		if (durabilityCost > 0) {
			// Durability cost is charged from the main hand item
			ItemStack inHand = HandHandler.getItemInMainHand(player);
			if (inHand == null || inHand.getDurability() >= inHand.getType().getMaxDurability()) {
				return false;
			}
		}
		
		// Money costs
		if (moneyCost > 0) {
			MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
			if (moneyHandler == null || !moneyHandler.hasMoney(player, moneyCost)) {
				return false;
			}
		}
		
		// Item costs
		if (reagents != null) {
			Inventory playerInventory = player.getInventory();
			for (ItemStack item : reagents) {
				if (item != null && !inventoryContains(playerInventory, item)) {
					return false;
				}
			}
		}
		
		// Variable costs
		if (variables != null) {
			VariableManager varMan = MagicSpells.getVariableManager();
			if (varMan == null) return false;
			for (Map.Entry<String, Double> var : variables.entrySet()) {
				double val = var.getValue();
				if (val > 0 && varMan.getValue(var.getKey(), player) < val) return false;
			}
		}
		
		return true;		
	}
	
	/**
	 * Removes the reagent cost of this spell from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove reagents from
	 */
	protected void removeReagents(Player player) {
		removeReagents(player, this.reagents);
	}
	
	// TODO can this safely be made varargs?
	/**
	 * Removes the specified reagents from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 */
	protected void removeReagents(Player player, ItemStack[] reagents) {
		removeReagents(player, reagents, 0, 0, 0, 0, 0, 0, 0, null);
	}
	
	protected void removeReagents(Player player, SpellReagents reagents) {
		removeReagents(player, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}
	
	/**
	 * Removes the specified reagents, including health and mana, from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 * @param healthCost the health to remove
	 * @param manaCost the mana to remove
	 */
	private void removeReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		if (Perm.NOREAGENTS.has(player)) return;
		
		if (reagents != null) {
			for (ItemStack item : reagents) {
				if (item != null) {
					Util.removeFromInventory(player.getInventory(), item);
				}
			}
		}
		
		if (healthCost != 0) {
			double h = player.getHealth() - healthCost;
			if (h < 0) h = 0;
			if (h > player.getMaxHealth()) h = player.getMaxHealth();
			player.setHealth(h);
		}
		
		if (manaCost != 0) MagicSpells.plugin.mana.addMana(player, -manaCost, ManaChangeReason.SPELL_COST);
		
		if (hungerCost != 0) {
			int f = player.getFoodLevel() - hungerCost;
			if (f < 0) f = 0;
			if (f > 20) f = 20;
			player.setFoodLevel(f);
		}
		
		if (experienceCost != 0) ExperienceUtils.changeExp(player, -experienceCost);
		
		if (durabilityCost != 0) {
			ItemStack inHand = HandHandler.getItemInMainHand(player);
			if (inHand != null && inHand.getType().getMaxDurability() > 0) {
				short newDura = (short) (inHand.getDurability() + durabilityCost);
				if (newDura < 0) newDura = 0;
				if (newDura >= inHand.getType().getMaxDurability()) {
					HandHandler.setItemInMainHand(player, null);
				} else {
					inHand.setDurability(newDura);
					HandHandler.setItemInMainHand(player, inHand);
				}
			}
		}
		
		if (moneyCost != 0) {
			MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
			if (moneyHandler != null) {
				if (moneyCost > 0) {
					moneyHandler.removeMoney(player, moneyCost);
				} else {
					moneyHandler.addMoney(player, moneyCost);
				}
			}
		}
		
		if (levelsCost != 0) {
			int lvl = player.getLevel() - levelsCost;
			if (lvl < 0) lvl = 0;
			player.setLevel(lvl);
		}
		
		if (variables != null) {
			VariableManager varMan = MagicSpells.getVariableManager();
			if (varMan != null) {
				for (Map.Entry<String, Double> var : variables.entrySet()) {
					varMan.modify(var.getKey(), player, -var.getValue());
				}
			}
		}
	}
	
	private boolean inventoryContains(Inventory inventory, ItemStack item) {
		if (inventory == null) return false;
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < 36; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) return true;
		}
		return false;
	}
	
	/*private void removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		inventory.setContents(items);
	}*/
	
	protected int getRange(float power) {
		return this.spellPowerAffectsRange ? Math.round(this.range * power) : this.range;
	}
	
	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param player the player to get the target for
	 * @param range the maximum range to check
	 * @param checkLos whether to obey line-of-sight restrictions
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(Player player, float power) {
		TargetInfo<LivingEntity> target = getTargetedEntity(player, power, true, null);
		if (target == null) return null;
		if (!(target.getTarget() instanceof Player)) return null;
		return new TargetInfo<>((Player)target.getTarget(), target.getPower());
	}
	
	protected TargetInfo<Player> getTargetPlayer(Player player, float power) {
		return getTargetedPlayer(player, power);
	}
	
	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power) {
		return getTargetedEntity(player, power, false, null);
	}
	
	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power, ValidTargetChecker checker) {
		return getTargetedEntity(player, power, false, checker);
	}
	
	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		// Get nearby entities
		// TODO rename to avoid hiding
		int range = getRange(power);
		List<Entity> ne = player.getNearbyEntities(range, range, range);
		
		// Get valid targets
		List<LivingEntity> entities;
		if (MagicSpells.plugin.checkWorldPvpFlag && this.validTargetList.canTargetPlayers() && !isBeneficial() && !player.getWorld().getPVP()) {
			entities = this.validTargetList.filterTargetListCastingAsLivingEntities(player, ne, false);
		} else if (forceTargetPlayers) {
			entities = this.validTargetList.filterTargetListCastingAsLivingEntities(player, ne, true);
		} else {
			entities = this.validTargetList.filterTargetListCastingAsLivingEntities(player, ne);
		}
		
		// Find target
		LivingEntity target = null;
		BlockIterator bi;
		try {
			bi = new BlockIterator(player, range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
		Block b;
		Location l;
		int bx;
		int by;
		int bz;
		double ex;
		double ey;
		double ez;
		// How far can a target be from the line of sight along the x, y, and z directions
		double xTolLower = 0.75;
		double xTolUpper = 1.75;
		double yTolLower = 1;
		double yTolUpper = 2.5;
		double zTolLower = 0.75;
		double zTolUpper = 1.75;
		// Do min range
		for (int i = 0; i < minRange && bi.hasNext(); i++) {
			bi.next();
		}
		// Loop through player's line of sight
		while (bi.hasNext()) {
			b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();
			if (obeyLos && !BlockUtils.isTransparent(this, b)) {
				// Line of sight is broken, stop without target
				break;
			} else {
				// Check for entities near this block in the line of sight
				for (LivingEntity e : entities) {
					l = e.getLocation();
					ex = l.getX();
					ey = l.getY();
					ez = l.getZ();
					
					if (!(bx - xTolLower <= ex && ex <= bx + xTolUpper)) continue;
					if (!(bz - zTolLower <= ez && ez <= bz + zTolUpper)) continue;
					if (!(by - yTolLower <= ey && ey <= by + yTolUpper)) continue;
					
					// Entity is close enough, set target and stop
					target = e;
					
					// Check for invalid target
					if (target instanceof Player && ((Player)target).getGameMode() == GameMode.CREATIVE) {
						target = null;
						continue;
					}
					
					// Check for anti-magic-zone
					if (target != null && MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(target.getLocation(), this)) {
						target = null;
						continue;
					}
					
					// Check for teams
					if (target instanceof Player && MagicSpells.plugin.checkScoreboardTeams) {
						Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
						Team playerTeam = scoreboard.getPlayerTeam(player);
						Team targetTeam = scoreboard.getPlayerTeam((Player)target);
						if (playerTeam != null && targetTeam != null) {
							if (playerTeam.equals(targetTeam)) {
								if (!playerTeam.allowFriendlyFire() && !this.isBeneficial()) {
									target = null;
									continue;
								}
							} else {
								if (this.isBeneficial()) {
									target = null;
									continue;
								}
							}
						}
					}
					
					// Call event listeners
					if (target != null) {
						SpellTargetEvent event = new SpellTargetEvent(this, player, target, power);
						EventUtil.call(event);
						if (event.isCancelled()) {
							target = null;
							continue;
						} else {
							target = event.getTarget();
							power = event.getPower();
						}
					}
					
					// Call damage event
					if (targetDamageCause != null) {
						EntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(player, target, targetDamageCause, targetDamageAmount);
						EventUtil.call(event);
						if (event.isCancelled()) {
							target = null;
							continue;
						}
					}
					
					// Run checker
					if (target != null && checker != null) {
						if (!checker.isValidTarget(target)) {
							target = null;
							continue;
						}
					}
					
					return new TargetInfo<>(target, power);
				}
			}
		}
		return null;
	}
	
	protected Block getTargetedBlock(LivingEntity entity, float power) {
		return BlockUtils.getTargetBlock(this, entity, this.spellPowerAffectsRange ? Math.round(this.range * power) : this.range);
	}
	
	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, this.spellPowerAffectsRange ? Math.round(this.range * power) : this.range);
	}
	
	public HashSet<Material> getLosTransparentBlocks() {
		return this.losTransparentBlocks;
	}
	
	public boolean isTransparent(Block block) {
		return this.losTransparentBlocks.contains(block.getType());
	}
	
	protected void playSpellEffects(Entity pos1, Entity pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1.getLocation(), pos2.getLocation());
	}
	
	protected void playSpellEffects(Entity pos1, Location pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1.getLocation(), pos2);
	}
	
	protected void playSpellEffects(Location pos1, Entity pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1, pos2.getLocation());
	}
	
	protected void playSpellEffects(Location pos1, Location pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1, pos2);
	}
	
	protected void playSpellEffects(EffectPosition pos, Entity entity) {
		if (this.effects != null) {
			List<SpellEffect> effectsList = this.effects.get(pos);
			if (effectsList != null) {
				for (SpellEffect effect : effectsList) {
					Runnable canceler = effect.playEffect(entity);
					if (canceler == null) continue;
					if (!(entity instanceof Player)) continue;
					Player p = (Player)entity;
					Map<EffectPosition, List<Runnable>> runnablesMap = this.callbacks.get(p.getUniqueId().toString());
					if (runnablesMap == null) continue;
					List<Runnable> runnables = runnablesMap.get(pos);
					if (runnables == null) continue;
					runnables.add(canceler);
				}
			}
		}
	}
	
	protected void playSpellEffects(EffectPosition pos, Location location) {
		if (this.effects != null) {
			List<SpellEffect> effectsList = this.effects.get(pos);
			if (effectsList != null) {
				for (SpellEffect effect : effectsList) {
					effect.playEffect(location);
				}
			}
		}
	}
	
	protected void playSpellEffectsTrail(Location loc1, Location loc2) {
		if (this.effects != null) {
			if (!LocationUtil.isSameWorld(loc1, loc2)) return;
			List<SpellEffect> effectsList = this.effects.get(EffectPosition.TRAIL);
			if (effectsList != null) {
				for (SpellEffect effect : effectsList) {
					effect.playEffect(loc1, loc2);
				}
			}
			
			List<SpellEffect> rTrailEffects = this.effects.get(EffectPosition.REVERSE_LINE);
			if (rTrailEffects != null) {
				for (SpellEffect effect: rTrailEffects) {
					effect.playEffect(loc2, loc1);
				}
			}
		}
	}
	
	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity) {
		if (this.effects != null) {
			List<SpellEffect> spellEffects = this.effects.get(pos);
			if (spellEffects != null) {
				for (SpellEffect e: spellEffects) {
					e.playTrackingLinePatterns(origin, target, originEntity, targetEntity);
				}
			}
		}
	}
	
	public void initializePlayerEffectTracker(Player p) {
		if (this.callbacks != null) {
			String key = p.getUniqueId().toString();
			Map<EffectPosition, List<Runnable>> entry = new EnumMap<>(EffectPosition.class);
			for (EffectPosition pos: EffectPosition.values()) {
				List<Runnable> runnables = new ArrayList<>();
				entry.put(pos, runnables);
			}
			this.callbacks.put(key, entry);
		}
	}
	
	public void unloadPlayerEffectTracker(Player p) {
		String uuid = p.getUniqueId().toString();
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffects(pos, uuid);
		}
		this.callbacks.remove(uuid);
	}
	
	public void cancelEffects(EffectPosition pos, String uuid) {
		if (this.callbacks == null) return;
		if (this.callbacks.get(uuid) == null) return;
		List<Runnable> cancelers = this.callbacks.get(uuid).get(pos);
		while (!cancelers.isEmpty()) {
			Runnable c = cancelers.iterator().next();
			if (c instanceof Effect) {
				Effect eff = (Effect)c;
				eff.cancel();
			} else {
				c.run();
			}
			cancelers.remove(c);
		}
	}
	
	public void cancelEffectForAllPlayers(EffectPosition pos) {
		for (String key: this.callbacks.keySet()) {
			cancelEffects(pos, key);
		}
	}
	
	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker) {
		if (this.effects != null) {
			List<SpellEffect> effectsList = this.effects.get(EffectPosition.BUFF);
			if (effectsList != null) {
				for (SpellEffect effect : effectsList) {
					effect.playEffectWhileActiveOnEntity(entity, checker);
				}
			}
			effectsList = this.effects.get(EffectPosition.ORBIT);
			if (effectsList != null) {
				for (SpellEffect effect : effectsList) {
					effect.playEffectWhileActiveOrbit(entity, checker);
				}
			}
		}
	}
	
	protected void registerEvents() {
		registerEvents(this);
	}
	
	protected void registerEvents(Listener listener) {
		MagicSpells.registerEvents(listener);
	}
	
	protected void unregisterEvents() {
		unregisterEvents(this);
	}
	
	protected void unregisterEvents(Listener listener) {
		HandlerList.unregisterAll(listener);
	}
	
	protected int scheduleDelayedTask(Runnable task, int delay) {
		return MagicSpells.scheduleDelayedTask(task, delay);
	}
	
	protected int scheduleRepeatingTask(Runnable task, int delay, int interval) {
		return MagicSpells.scheduleRepeatingTask(task, delay, interval);
	}
	
	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicSpells.formatMessage(message, replacements);
	}
	
	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(Player player, String message, String... replacements) {
		sendMessage(formatMessage(message, replacements), player, null);
	}
	
	protected void sendMessage(Player player, String message) {
		sendMessage(message, player, null);
	}
	
	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	protected void sendMessage(String message, Player player, String[] args) {
		MagicSpells.sendMessage(message, player, args);
	}
	
	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 */
	protected void sendMessageNear(Player player, String message) {
		sendMessageNear(player, null, message, this.broadcastRange, MagicSpells.NULL_ARGS);
	}
	
	// TODO can this safely be made varargs?
	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	protected void sendMessageNear(Player player, Player ignore, String message, int range, String[] args) {
		if (message == null) return;
		if (message.isEmpty()) return;
		if (Perm.SILENT.has(player)) return;
		
		// FIXME extract the regexp to a pattern
		String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
		int rangeDoubled = range << 1;
		List<Entity> entities = player.getNearbyEntities(rangeDoubled, rangeDoubled, rangeDoubled);
		for (Entity entity : entities) {
			if (!(entity instanceof Player)) continue;
			if (entity == player) continue;
			if (entity == ignore) continue;
			for (String msg : msgs) {
				if (msg.isEmpty()) continue;
				((Player)entity).sendMessage(MagicSpells.plugin.textColor + msg);
			}
		}
	}
	
	public String getInternalName() {
		return this.internalName;
	}
	
	public String getName() {
		if (this.name != null && !this.name.isEmpty()) return this.name;
		return this.internalName;
	}
	
	public String getPermissionName() {
		return this.permName;
	}
	
	public boolean isHelperSpell() {
		return this.helperSpell;
	}
	
	public String getCantBindError() {
		return this.strCantBind;
	}
	
	public String[] getAliases() {
		return this.aliases;
	}
	
	public List<String> getIncantations() {
		return this.incantations;
	}
	
	public CastItem getCastItem() {
		if (this.castItems.length == 1) return this.castItems[0];
		return null;
	}
	
	public CastItem[] getCastItems() {
		return this.castItems;
	}
	
	public CastItem[] getRightClickCastItems() {
		return this.rightClickCastItems;
	}
	
	public CastItem[] getConsumeCastItems() {
		return this.consumeCastItems;
	}
	
	public String getDanceCastSequence() {
		return this.danceCastSequence;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public SpellReagents getReagents() {
		return this.reagents;
	}
	
	public String getConsoleName() {
		return MagicSpells.plugin.strConsoleName;
	}
	
	public String getStrWrongCastItem() {
		return this.strWrongCastItem;
	}
	
	public final boolean isBeneficial() {
		return this.beneficial;
	}
	
	public boolean isBeneficialDefault() {
		return false;
	}
	
	public ModifierSet getModifiers() {
		return this.modifiers;
	}
	
	public ModifierSet getTargetModifiers() {
		return this.targetModifiers;
	}
	
	public String getStrModifierFailed() {
		return this.strModifierFailed;
	}
	
	public Map<String, Integer> getXpGranted() {
		return this.xpGranted;
	}
	
	public Map<String, Integer> getXpRequired() {
		return this.xpRequired;
	}
	
	public String getStrXpLearned() {
		return this.strXpAutoLearned;
	}
	
	Map<String, Long> getCooldowns() {
		return this.nextCast;
	}
	
	public Map<String, VariableMod> getVariableModsCast() {
		return this.variableModsCast;
	}
	
	public Map<String, VariableMod> getVariableModsCasted() {
		return this.variableModsCasted;
	}
	
	public Map<String, VariableMod> getVariableModsTarget() {
		return this.variableModsTarget;
	}
	
	void setCooldownManually(String name, long nextCast) {
		this.nextCast.put(name, nextCast);
	}
	
	protected void debug(int level, String message) {
		if (this.debug) MagicSpells.debug(level, message);
	}
	
	/**
	 * This method is called when the plugin is being disabled, for any reason.
	 */
	protected void turnOff() {
		// No op
	}
	
	@Override
	public int compareTo(Spell spell) {
		return this.name.compareTo(spell.name);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Spell && ((Spell)o).internalName.equals(this.internalName);
	}
	
	@Override
	public int hashCode() {
		return this.internalName.hashCode();
	}
	
	// TODO move this to its own class
	public enum SpellCastState {
		
		NORMAL,
		ON_COOLDOWN,
		MISSING_REAGENTS,
		CANT_CAST,
		NO_MAGIC_ZONE,
		WRONG_WORLD
		
	}
	
	// TODO move this to its own class
	public enum PostCastAction {
		
		HANDLE_NORMALLY(true, true, true),
		ALREADY_HANDLED(false, false, false),
		NO_MESSAGES(true, true, false),
		NO_REAGENTS(true, false, true),
		NO_COOLDOWN(false, true, true),
		MESSAGES_ONLY(false, false, true),
		REAGENTS_ONLY(false, true, false),
		COOLDOWN_ONLY(true, false, false),
		DELAYED(false, false, false);
		
		private boolean cooldown;
		private boolean reagents;
		private boolean messages;
		
		PostCastAction(boolean cooldown, boolean reagents, boolean messages) {
			this.cooldown = cooldown;
			this.reagents = reagents;
			this.messages = messages;
		}
		
		public boolean setCooldown() {
			return this.cooldown;
		}
		
		public boolean chargeReagents() {
			return this.reagents;
		}
		
		public boolean sendMessages() {
			return this.messages;
		}
		
	}
	
	// TODO move this to its own class
	public class SpellCastResult {
		
		public SpellCastState state;
		public PostCastAction action;
		
		public SpellCastResult(SpellCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
		}
		
	}
	
	public class DelayedSpellCast implements Runnable, Listener {
		
		private Player player;
		private Location prevLoc;
		private Spell spell;
		private SpellCastEvent spellCast;
		private int taskId;
		private boolean cancelled = false;
		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;
		
		public DelayedSpellCast(SpellCastEvent spellCast) {
			this.player = spellCast.getCaster();
			this.prevLoc = player.getLocation().clone();
			this.spell = spellCast.getSpell();
			this.spellCast = spellCast;
			
			this.taskId = scheduleDelayedTask(this, spellCast.getCastTime());
			registerEvents(this);
		}
		
		@Override
		public void run() {
			if (!cancelled && player.isOnline() && !player.isDead()) {
				Location currLoc = player.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (!spell.hasReagents(player, reagents)) {
						spellCast.setSpellCastState(SpellCastState.MISSING_REAGENTS);
					}
					spell.handleCast(spellCast);
				} else {
					interrupt();
				}
			}
			unregisterEvents(this);
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(player)) return;
			cancelled = true;
			Bukkit.getScheduler().cancelTask(taskId);
			interrupt();
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!event.getCaster().equals(player)) return;
			cancelled = true;
			Bukkit.getScheduler().cancelTask(taskId);
			interrupt();
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (cancelled) return;
			if (!event.getPlayer().equals(player)) return;
			cancelled = true;
			Bukkit.getScheduler().cancelTask(taskId);
			interrupt();
		}
		
		private void interrupt() {
			sendMessage(strInterrupted, player, null);
			if (spellOnInterrupt != null) {
				spellOnInterrupt.castSpell(player, SpellCastState.NORMAL, spellCast.getPower(), null); // Null args
			}
		}
	}
	
	public class DelayedSpellCastWithBar implements Runnable, Listener {
		
		private Player player;
		private Location prevLoc;
		private Spell spell;
		private SpellCastEvent spellCast;
		private int castTime;
		private int taskId;
		private boolean cancelled = false;
		
		private int interval = 5;
		private int elapsed = 0;
		
		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;
		
		public DelayedSpellCastWithBar(SpellCastEvent spellCast) {
			this.player = spellCast.getCaster();
			this.prevLoc = player.getLocation().clone();
			this.spell = spellCast.getSpell();
			this.spellCast = spellCast;
			this.castTime = spellCast.getCastTime();
			
			MagicSpells.getExpBarManager().lock(player, this);
			
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}
		
		@Override
		public void run() {
			if (!cancelled && player.isOnline() && !player.isDead()) {
				elapsed += interval;
				Location currLoc = player.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (elapsed >= castTime) {
						if (!spell.hasReagents(player, reagents)) {
							spellCast.setSpellCastState(SpellCastState.MISSING_REAGENTS);
						}
						spell.handleCast(spellCast);
						cancelled = true;
					}
					MagicSpells.getExpBarManager().update(player, 0, (float)elapsed / (float)castTime, this);
				} else {
					interrupt();
				}
			} else {
				end();
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(player)) return;
			cancelled = true;
			interrupt();
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			Player caster = event.getCaster();
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (caster == null) return;
			if (!caster.equals(player)) return;
			cancelled = true;
			interrupt();
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (interruptOnTeleport && !cancelled && event.getPlayer().equals(player)) {
				cancelled = true;
				interrupt();
			}
		}
		
		private void interrupt() {
			sendMessage(strInterrupted, player, null);
			end();
			if (spellOnInterrupt != null) spellOnInterrupt.castSpell(player, SpellCastState.NORMAL, spellCast.getPower(), null);
		}
		
		private void end() {
			cancelled = true;
			Bukkit.getScheduler().cancelTask(taskId);
			unregisterEvents(this);
			MagicSpells.getExpBarManager().unlock(player, this);
			MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			ManaHandler mana = MagicSpells.getManaHandler();
			if (mana != null) mana.showMana(player);
		}
	}
	
	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}
	
	// TODO move this to its own class
	@FunctionalInterface
	public interface ValidTargetChecker {
		
		boolean isValidTarget(LivingEntity entity);
		
	}

}
