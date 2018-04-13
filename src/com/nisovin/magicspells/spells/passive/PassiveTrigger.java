package com.nisovin.magicspells.spells.passive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.MagicSpells;

public class PassiveTrigger {
	
	private static Map<String, PassiveTrigger> map = new HashMap<>();
	
	private static Map<EventPriority, String> triggerPrioritySuffix;
	static {
		triggerPrioritySuffix = new HashMap<>();
		triggerPrioritySuffix.put(EventPriority.LOWEST, "_lowestpriority");
		triggerPrioritySuffix.put(EventPriority.LOW, "_lowpriority");
		triggerPrioritySuffix.put(EventPriority.NORMAL, "");
		triggerPrioritySuffix.put(EventPriority.HIGH, "_highpriority");
		triggerPrioritySuffix.put(EventPriority.HIGHEST, "_highestpriority");
		triggerPrioritySuffix.put(EventPriority.MONITOR, "_monitorpriority");
	}
	
	public static Set<PassiveTrigger> TAKE_DAMAGE = addTriggers("takedamage", TakeDamageListener.class);
	public static Set<PassiveTrigger> GIVE_DAMAGE = addTriggers("givedamage", GiveDamageListener.class);
	public static Set<PassiveTrigger> FATAL_DAMAGE = addTriggers("fataldamage", FatalDamageListener.class);
	public static Set<PassiveTrigger> KILL = addTriggers("kill", KillListener.class);
	public static Set<PassiveTrigger> DEATH = addTriggers("death", DeathListener.class);
	public static Set<PassiveTrigger> RESPAWN = addTriggers("respawn", RespawnListener.class);
	public static Set<PassiveTrigger> JOIN = addTriggers("join", JoinListener.class);
	public static Set<PassiveTrigger> QUIT = addTriggers("quit", QuitListener.class);
	public static Set<PassiveTrigger> BLOCK_BREAK = addTriggers("blockbreak", BlockBreakListener.class);
	public static Set<PassiveTrigger> BLOCK_PLACE = addTriggers("blockplace", BlockPlaceListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK = addTriggers("rightclick", RightClickItemListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_OFFHAND = addTriggers("rightclickoffhand", RightClickItemListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_BLOCK_TYPE = addTriggers("rightclickblocktype", RightClickBlockTypeListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_BLOCK_TYPE_INSPECT_ITEM = addTriggers("rightclickblocktypeinspectitem", RightClickBlockTypeInspectItemListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_BLOCK_COORD = addTriggers("rightclickblockcoord", RightClickBlockCoordListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_BLOCK_COORD_OFFHAND = addTriggers("rightclickblockcoordoffhand", RightClickBlockCoordListener.class);
	public static Set<PassiveTrigger> LEFT_CLICK_BLOCK_TYPE = addTriggers("leftclickblocktype", LeftClickBlockTypeListener.class);
	public static Set<PassiveTrigger> LEFT_CLICK_BLOCK_COORD = addTriggers("leftclickblockcoord", LeftClickBlockCoordListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_ENTITY = addTriggers("rightclickentity", RightClickEntityListener.class);
	public static Set<PassiveTrigger> RIGHT_CLICK_ENTITY_OFFHAND = addTriggers("rightclickentityoffhand", RightClickEntityListener.class);
	public static Set<PassiveTrigger> SPELL_CAST = addTriggers("spellcast", SpellCastListener.class);
	public static Set<PassiveTrigger> SPELL_CASTED = addTriggers("spellcasted", SpellCastedListener.class);
	public static Set<PassiveTrigger> SPELL_TARGET = addTriggers("spelltarget", SpellTargetListener.class);
	public static Set<PassiveTrigger> SPELL_TARGETED = addTriggers("spelltargeted", SpellTargetedListener.class);
	public static Set<PassiveTrigger> SPRINT = addTriggers("sprint", SprintListener.class);
	public static Set<PassiveTrigger> STOP_SPRINT = addTriggers("stopsprint", SprintListener.class);
	public static Set<PassiveTrigger> SNEAK = addTriggers("sneak", SneakListener.class);
	public static Set<PassiveTrigger> STOP_SNEAK = addTriggers("stopsneak", SneakListener.class);
	public static Set<PassiveTrigger> FLY = addTriggers("fly", FlyListener.class);
	public static Set<PassiveTrigger> STOP_FLY = addTriggers("stopfly", FlyListener.class);
	public static Set<PassiveTrigger> HOT_BAR_SELECT = addTriggers("hotbarselect", HotBarListener.class);
	public static Set<PassiveTrigger> HOT_BAR_DESELECT = addTriggers("hotbardeselect", HotBarListener.class);
	public static Set<PassiveTrigger> DROP_ITEM = addTriggers("dropitem", DropItemListener.class);
	public static Set<PassiveTrigger> PICKUP_ITEM = addTriggers("pickupitem", PickupItemListener.class);
	public static Set<PassiveTrigger> CRAFT = addTriggers("craft", CraftListener.class);
	public static Set<PassiveTrigger> FISH = addTriggers("fish", FishListener.class);
	public static Set<PassiveTrigger> SHOOT = addTriggers("shoot", ShootListener.class);
	public static Set<PassiveTrigger> TELEPORT = addTriggers("teleport", TeleportListener.class);
	public static Set<PassiveTrigger> BUFF = addTriggers("buff", BuffListener.class);
	public static Set<PassiveTrigger> TICKS = addTriggers("ticks", TicksListener.class);
	
	// can't do priorities here
	public static PassiveTrigger RESOURCE_PACK = addTrigger("resourcepack", ResourcePackListener.class);
	
	public static Set<PassiveTrigger> ENTER_BED = addTriggers("enterbed", EnterBedListener.class);
	public static Set<PassiveTrigger> LEAVE_BED = addTriggers("leavebed", LeaveBedListener.class);
	public static Set<PassiveTrigger> SHEAR_SHEEP = addTriggers("shearsheep", SheepShearListener.class);
	public static Set<PassiveTrigger> SWAP_HAND_ITEMS = addTriggers("swaphanditem", OffhandSwapListener.class);
	
	public static Set<PassiveTrigger> START_GLIDE = addTriggers("startglide", GlideListener.class);
	public static Set<PassiveTrigger> STOP_GLIDE = addTriggers("stopglide", GlideListener.class);
	
	
	public static Set<PassiveTrigger> addTriggers(String baseName, Class<? extends PassiveListener> listener) {
		Set<PassiveTrigger> ret = new HashSet<>();
		for (Map.Entry<EventPriority, String> entry: triggerPrioritySuffix.entrySet()) {
			ret.add(addTrigger(baseName + entry.getValue(), listener, entry.getKey()));
		}
		return ret;
	}
	
	public static PassiveTrigger addTrigger(String name, Class<? extends PassiveListener> listener) {
		return addTrigger(name, listener, EventPriority.NORMAL);
	}
	
	public static PassiveTrigger addTrigger(String name, Class<? extends PassiveListener> listener, EventPriority overridePriority) {
		PassiveTrigger trigger = new PassiveTrigger(name, listener, overridePriority);
		map.put(trigger.getName(), trigger);
		return trigger;
	}
	
	public static PassiveTrigger getByName(String name) {
		return map.get(name);
	}
	
	String name;
	Class<? extends PassiveListener> listenerClass;
	PassiveListener listener;
	EventPriority customPriority;
	
	PassiveTrigger(String name, Class<? extends PassiveListener> listener, EventPriority overridePriority) {
		this.name = name;
		this.listenerClass = listener;
		this.customPriority = overridePriority;
	}
	
	public String getName() {
		return name;
	}
	
	public PassiveListener getListener() {
		if (listener == null) {
			try {
				listener = listenerClass.newInstance();
				listener.priority = customPriority;
				MagicSpells.registerEvents(listener, customPriority);
			} catch (Exception e) {
				MagicSpells.handleException(e);
			}
		}
		return listener;
	}
	
}
