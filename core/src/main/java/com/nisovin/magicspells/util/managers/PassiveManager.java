package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.spells.passive.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PassiveManager {

	private static final Map<String, EventPriority> eventPriorities = new HashMap<>();

	private static final Map<String, Class<? extends PassiveListener>> listeners = new HashMap<>();

	public PassiveManager() {
		initialize();
	}

	public void addListener(String name, Class<? extends PassiveListener> listener) {
		listeners.put(name.toLowerCase(), listener);
	}

	public void addListener(Class<? extends PassiveListener> listener, String name) {
		listeners.put(name.toLowerCase(), listener);
	}

	public Map<String, EventPriority> getEventPriorities() {
		return eventPriorities;
	}

	public Map<String, Class<? extends PassiveListener>> getListeners() {
		return listeners;
	}

	public PassiveListener getListenerByName(String name) {
		Class<? extends PassiveListener> clazz = listeners.get(name.toLowerCase());
		if (clazz == null) return null;

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	public EventPriority getEventPriorityFromName(String name) {
		if (name == null) throw new NullPointerException("name");
		for (String str : eventPriorities.keySet()) {
			if (!name.contains(str)) continue;
			return eventPriorities.get(str);
		}
		return null;
	}

	public String getEventPriorityName(EventPriority priority) {
		for (String name : eventPriorities.keySet()) {
			if (!eventPriorities.get(name).equals(priority)) continue;
			return name;
		}
		return null;
	}
	
	private void initialize() {
		// initialize priorities
		for (EventPriority priority : EventPriority.values()) {
			eventPriorities.put("_" + priority.name().toLowerCase() + "priority", priority);
		}

		addListener("blockbreak", BlockBreakListener.class);
		addListener("blockplace", BlockPlaceListener.class);
		addListener("buff", BuffListener.class);
		addListener("craft", CraftListener.class);
		addListener("death", DeathListener.class);
		addListener("dropitem", DropItemListener.class);
		addListener("enterbed", EnterBedListener.class);
		addListener("fataldamage", FatalDamageListener.class);
		addListener("gamemodechange", GameModeChangeListener.class);
		addListener("givedamage", GiveDamageListener.class);
		addListener("hitarrow", HitArrowListener.class);
		addListener("hotbardeselect", HotbarDeselectListener.class);
		addListener("hotbarselect", HotbarSelectListener.class);
		addListener("inventoryaction", InventoryActionListener.class);
		addListener("inventoryclick", InventoryClickListener.class);
		addListener("join", JoinListener.class);
		addListener("jump", JumpListener.class);
		addListener("kill", KillListener.class);
		addListener("leavebed", LeaveBedListener.class);
		addListener("leftclickblockcoord", LeftClickBlockCoordListener.class);
		addListener("leftclickblocktype", LeftClickBlockTypeListener.class);
		addListener("magicspellsloaded", MagicSpellsLoadedListener.class);
		addListener("missarrow", MissArrowListener.class);
		addListener("offhandswap", OffhandSwapListener.class);
		addListener("pickupitem", PickupItemListener.class);
		addListener("potioneffect", PotionEffectListener.class);
		addListener("quit", QuitListener.class);
		addListener("resourcepack", ResourcePackListener.class);
		addListener("respawn", RespawnListener.class);
		addListener("rightclickblockcoord", RightClickBlockCoordListener.class);
		addListener("rightclickblocktype", RightClickBlockTypeListener.class);
		addListener("rightclickentity", RightClickEntityListener.class);
		addListener("rightclickitem", RightClickItemListener.class);
		addListener("sheepshear", SheepShearListener.class);
		addListener("shoot", ShootListener.class);
		addListener("signbook", SignBookListener.class);
		addListener("spellcasted", SpellCastedListener.class);
		addListener("spellcast", SpellCastListener.class);
		addListener("spellselect", SpellSelectListener.class);
		addListener("spelltargeted", SpellTargetedListener.class);
		addListener("spelltarget", SpellTargetListener.class);
		addListener("startfly", StartFlyListener.class);
		addListener("startglide", StartGlideListener.class);
		addListener("startsneak", StartSneakListener.class);
		addListener("startsprint", StartSprintListener.class);
		addListener("startswim", StartSwimListener.class);
		addListener("stopfly", StopFlyListener.class);
		addListener("stopglide", StopGlideListener.class);
		addListener("stopsneak", StopSneakListener.class);
		addListener("stopsprint", StopSprintListener.class);
		addListener("stopswim", StopSwimListener.class);
		addListener("takedamage", TakeDamageListener.class);
		addListener("teleport", TeleportListener.class);
		addListener("ticks", TicksListener.class);
		addListener("worldchange", WorldChangeListener.class);
	}
	
}
