package com.nisovin.magicspells.spells.targeted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.TargetInfo;

import javax.annotation.Nullable;

public class DisguiseSpell extends TargetedSpell implements TargetedEntitySpell {

	static IDisguiseManager manager;

	private DisguiseSpell thisSpell;
	private EntityData entityData;
	private boolean showPlayerName = false;
	private String nameplateText = "";
	private String uuid = "";
	private String skin = "";
	private String skinSig = "";
	private boolean alwaysShowNameplate = true;
	private boolean preventPickups = false;
	private boolean friendlyMobs = true;
	private boolean ridingBoat = false;
	private boolean undisguiseOnDeath = true;
	private boolean undisguiseOnLogout = false;
	private boolean undisguiseOnCast = false;
	boolean undisguiseOnGiveDamage = false;
	boolean undisguiseOnTakeDamage = false;
	private boolean disguiseSelf = false;
	private int duration;
	private boolean toggle;
	private String strFade;
	
	Map<String, Disguise> disguised = new HashMap<String, Disguise>();
	
	public DisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;
		
		if (manager == null) {
			try {
				manager = MagicSpells.getVolatileCodeHandler().getDisguiseManager(config);
			} catch (Exception e) {
				manager = null;
			}
			if (manager == null) {
				MagicSpells.error("DisguiseManager could not be created!");
				return;
			}
		}
		manager.registerSpell(this);
		
		String type = getConfigString("entity-type", "zombie");
		entityData = new EntityData(type);
		showPlayerName = getConfigBoolean("show-player-name", false);
		nameplateText = ChatColor.translateAlternateColorCodes('&', getConfigString("nameplate-text", ""));
		uuid = getConfigString("uuid", "");
		if (configKeyExists("skin")) {
			String skinName = getConfigString("skin", "skin");
			File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
			if (folder.exists()) {
				try {
					File file = new File(folder, skinName + ".skin.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skin = reader.readLine();
						reader.close();
					}
					file = new File(folder, skinName + ".sig.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skinSig = reader.readLine();
						reader.close();
					}
				} catch (Exception e) {
					MagicSpells.handleException(e);
				}
			}
		}
		alwaysShowNameplate = getConfigBoolean("always-show-nameplate", true);
		preventPickups = getConfigBoolean("prevent-pickups", true);
		friendlyMobs = getConfigBoolean("friendly-mobs", true);
		ridingBoat = getConfigBoolean("riding-boat", false);
		undisguiseOnDeath = getConfigBoolean("undisguise-on-death", true);
		undisguiseOnLogout = getConfigBoolean("undisguise-on-logout", false);
		undisguiseOnCast = getConfigBoolean("undisguise-on-cast", false);
		undisguiseOnGiveDamage = getConfigBoolean("undisguise-on-give-damage", false);
		undisguiseOnTakeDamage = getConfigBoolean("undisguise-on-take-damage", false);
		disguiseSelf = getConfigBoolean("disguise-self", false);
		duration = getConfigInt("duration", 0);
		toggle = getConfigBoolean("toggle", false);
		targetSelf = getConfigBoolean("target-self", true);
		strFade = getConfigString("str-fade", "");
				
		if (entityData.getType() == null) {
			MagicSpells.error("Invalid entity-type specified for disguise spell '" + spellName + "'");
		}
	}
	
	@Override
	public void initialize() {
		if (manager == null) return;
		super.initialize();
		if (undisguiseOnCast) {
			registerEvents(new CastListener());
		}
		if (undisguiseOnGiveDamage || undisguiseOnTakeDamage) {
			registerEvents(new DamageListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (manager == null) return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL) {
			Disguise oldDisguise = disguised.remove(player.getName().toLowerCase());
			manager.removeDisguise(player);
			if (oldDisguise != null && toggle) {
				sendMessage(strFade, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			TargetInfo<Player> target = getTargetPlayer(player, power);
			if (target != null) {
				disguise(target.getTarget());
				sendMessages(player, target.getTarget());
				playSpellEffects(EffectPosition.CASTER, player);
				return PostCastAction.NO_MESSAGES;
			} else {
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void disguise(Player player) {
		String nameplate = nameplateText;
		if (showPlayerName) nameplate = player.getDisplayName();
		PlayerDisguiseData playerDisguiseData = new PlayerDisguiseData((uuid.isEmpty() ? UUID.randomUUID().toString() : uuid), skin, skinSig);
		Disguise disguise = new Disguise(player, entityData.getType(), nameplate, playerDisguiseData, alwaysShowNameplate, disguiseSelf, ridingBoat, entityData.getFlag(), entityData.getVar1(), entityData.getVar2(), entityData.getVar3(), duration, this);
		manager.addDisguise(player, disguise);
		disguised.put(player.getName().toLowerCase(), disguise);
		playSpellEffects(EffectPosition.TARGET, player);
	}
	
	public void undisguise(Player player) {
		Disguise disguise = disguised.remove(player.getName().toLowerCase());
		if (disguise != null) {
			disguise.cancelDuration();
			sendMessage(strFade, player, MagicSpells.NULL_ARGS);
			playSpellEffects(EffectPosition.DISABLED, player);
		}
	}
	
	@Override
	public boolean castAtEntity(@Nullable Player player, LivingEntity target, float power) {
		if (target instanceof Player) {
			disguise((Player)target);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (target instanceof Player) {
			disguise((Player)target);
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {
		if (preventPickups && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (undisguiseOnDeath && disguised.containsKey(event.getEntity().getName().toLowerCase())) {
			manager.removeDisguise(event.getEntity(), entityData.getType() == EntityType.PLAYER);
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (undisguiseOnLogout && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
			manager.removeDisguise(event.getPlayer(), entityData.getType() == EntityType.PLAYER);
		}
	}
	
	@EventHandler
	public void onTarget(EntityTargetEvent event) {
		if (friendlyMobs && event.getTarget() != null && event.getTarget() instanceof Player && disguised.containsKey(((Player)event.getTarget()).getName().toLowerCase())) {
			event.setCancelled(true);
		}
	}
	
	class CastListener implements Listener {
		@EventHandler
		void onSpellCast(SpellCastedEvent event) {
			if (event.getCaster() == null) {
				return;
			}
			if (event.getSpell() != thisSpell && disguised.containsKey(event.getCaster().getName().toLowerCase())) {
				manager.removeDisguise(event.getCaster());
			}
		}
	}
	
	class DamageListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		void onDamage(EntityDamageEvent event) {
			if (undisguiseOnTakeDamage && event.getEntity() instanceof Player && disguised.containsKey(((Player)event.getEntity()).getName().toLowerCase())) {
				manager.removeDisguise((Player)event.getEntity());
			}
			if (undisguiseOnGiveDamage && event instanceof EntityDamageByEntityEvent) {
				Entity e = ((EntityDamageByEntityEvent)event).getDamager();
				if (e instanceof Player) {
					if (disguised.containsKey(((Player)e).getName().toLowerCase())) {
						manager.removeDisguise((Player)e);
					}
				} else if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof Player) {
					Player shooter = (Player)((Projectile)e).getShooter();
					if (disguised.containsKey(shooter.getName().toLowerCase())) {
						manager.removeDisguise(shooter);
					}
				}
			}
		}
	}
	
	public static IDisguiseManager getDisguiseManager() {
		return manager;
	}
	
	@Override
	public void turnOff() {
		if (manager != null) {
			for (String name : new ArrayList<String>(disguised.keySet())) {
				Player player = PlayerNameUtils.getPlayerExact(name);
				if (player != null) {
					manager.removeDisguise(player, false);
				}
			}
			manager.unregisterSpell(this);
			if (manager.registeredSpellsCount() == 0) {
				manager.destroy();
				manager = null;
			}
		}
	}

	
	public class Disguise {

		Player player;
		private EntityType entityType;
		private String nameplateText;
		private PlayerDisguiseData playerDisguiseData;
		private boolean alwaysShowNameplate;
		private boolean disguiseSelf;
		private boolean ridingBoat;
		private boolean flag;
		private int var1;
		private int var2;
		private int var3;
		private DisguiseSpell spell;
		
		private int taskId;
		
		public Disguise(Player player, EntityType entityType, String nameplateText, PlayerDisguiseData playerDisguiseData, boolean alwaysShowNameplate, boolean disguiseSelf, boolean ridingBoat, boolean flag, int var1, int var2, int var3, int duration, DisguiseSpell spell) {
			this.player = player;
			this.entityType = entityType;
			this.nameplateText = nameplateText;
			this.playerDisguiseData = playerDisguiseData;
			this.alwaysShowNameplate = alwaysShowNameplate;
			this.disguiseSelf = disguiseSelf;
			this.ridingBoat = ridingBoat;
			this.flag = flag;
			this.var1 = var1;
			this.var2 = var2;
			this.var3 = var3;
			if (duration > 0) {
				startDuration(duration);
			}
			this.spell = spell;
		}
		
		public Player getPlayer() {
			return player;
		}
		
		public EntityType getEntityType() {
			return entityType;
		}
		
		public String getNameplateText() {
			return nameplateText;
		}
		
		public PlayerDisguiseData getPlayerDisguiseData() {
			return playerDisguiseData;
		}
		
		public boolean alwaysShowNameplate() {
			return alwaysShowNameplate;
		}
		
		public boolean disguiseSelf() {
			return disguiseSelf;
		}
		
		public boolean isRidingBoat() {
			return ridingBoat;
		}
		
		public boolean getFlag() {
			return flag;
		}
		
		public int getVar1() {
			return var1;
		}
		
		public int getVar2() {
			return var2;
		}
		
		public int getVar3() {
			return var3;
		}
			
		private void startDuration(int duration) {
			taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				@Override
				public void run() {
					DisguiseSpell.manager.removeDisguise(player);
				}
			}, duration);
		}
		
		public void cancelDuration() {
			if (taskId > 0) {
				Bukkit.getScheduler().cancelTask(taskId);
				taskId = 0;
			}
		}
		
		public DisguiseSpell getSpell() {
			return spell;
		}
		
	}
	
	public class PlayerDisguiseData {
		public String uuid;
		public String skin;
		public String sig;
		public PlayerDisguiseData(String uuid, String skin, String sig) {
			this.uuid = uuid;
			this.skin = skin;
			this.sig = sig;
		}
		@Override
		public PlayerDisguiseData clone() {
			return new PlayerDisguiseData(uuid, skin, sig);
		}
	}
	
}
