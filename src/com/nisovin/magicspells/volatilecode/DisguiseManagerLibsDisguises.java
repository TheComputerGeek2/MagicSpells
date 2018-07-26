package com.nisovin.magicspells.volatilecode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.nisovin.magicspells.util.compat.EventUtil;
import me.libraryaddict.disguise.disguisetypes.AnimalColor;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.FlagWatcher;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.RabbitType;
import me.libraryaddict.disguise.disguisetypes.watchers.AgeableWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.BatWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.GuardianWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.HorseWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.OcelotWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.PigWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.RabbitWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.SheepWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.SkeletonWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.SlimeWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.VillagerWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.WolfWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell.Disguise;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public class DisguiseManagerLibsDisguises implements Listener, IDisguiseManager {
	
	protected boolean hideArmor;
	
	protected Set<DisguiseSpell> disguiseSpells = new HashSet<>();
	protected Map<String, DisguiseSpell.Disguise> disguises = new ConcurrentHashMap<>();
	protected Map<String, me.libraryaddict.disguise.disguisetypes.Disguise> libsDisguises = new ConcurrentHashMap<>();
	protected Map<Integer, DisguiseSpell.Disguise> disguisedEntityIds = new ConcurrentHashMap<>();
	protected Map<Integer, me.libraryaddict.disguise.disguisetypes.Disguise> libsDisguisedEntityIds = new ConcurrentHashMap<>();
	protected Set<Integer> dragons = Collections.synchronizedSet(new HashSet<Integer>());
	protected Map<Integer, Integer> mounts = new ConcurrentHashMap<>();
	
	protected Random random = new Random();
	
	public DisguiseManagerLibsDisguises(MagicConfig config) {
		this.hideArmor = config.getBoolean("general.disguise-spell-hide-armor", false);
		EventUtil.register(this);
	}
	
	@Override
	public void registerSpell(DisguiseSpell spell) {
		this.disguiseSpells.add(spell);
	}

	@Override
	public void unregisterSpell(DisguiseSpell spell) {
		this.disguiseSpells.remove(spell);
	}

	@Override
	public int registeredSpellsCount() {
		return this.disguiseSpells.size();
	}

	@Override
	public void addDisguise(Player player, Disguise disguise) {
		if (isDisguised(player)) removeDisguise(player);
		me.libraryaddict.disguise.disguisetypes.Disguise libs = getDisguiseLibDisguise(disguise);
		this.disguises.put(player.getName().toLowerCase(), disguise);
		this.libsDisguises.put(player.getName().toLowerCase(), libs);
		
		this.disguisedEntityIds.put(player.getEntityId(), disguise);
		this.libsDisguisedEntityIds.put(player.getEntityId(), libs);
		
		if (disguise.getEntityType() == EntityType.ENDER_DRAGON) this.dragons.add(player.getEntityId());
		applyDisguise(player, libs);
	}

	private void applyDisguise(Player player, me.libraryaddict.disguise.disguisetypes.Disguise disguise) {
		disguise.setEntity(player);
		disguise.startDisguise();
	}
	
	@Override
	public void removeDisguise(Player player) {
		removeDisguise(player, true);
	}

	@Override
	public void removeDisguise(Player player, boolean sendPlayerPackets) {
		removeDisguise(player, sendPlayerPackets, true);
	}

	@Override
	public void removeDisguise(Player player, boolean sendPlayerPackets, boolean delaySpawnPacket) {
		DisguiseSpell.Disguise disguise = this.disguises.get(player.getName().toLowerCase());
		me.libraryaddict.disguise.disguisetypes.Disguise libsDisguise = this.libsDisguises.get(player.getName().toLowerCase());
		
		this.disguisedEntityIds.remove(player.getEntityId());
		this.libsDisguisedEntityIds.remove(player.getEntityId());
		
		this.dragons.remove(player.getEntityId());
		if (libsDisguise != null) {
			libsDisguise.stopDisguise();
			disguise.getSpell().undisguise(player);
			this.disguises.remove(player.getName().toLowerCase());
			this.libsDisguises.remove(player.getName().toLowerCase());
		}
		this.mounts.remove(player.getEntityId());
	}

	@Override
	public boolean isDisguised(Player player) {
		return this.disguises.containsKey(player.getName().toLowerCase());
	}

	@Override
	public Disguise getDisguise(Player player) {
		return this.disguises.get(player.getName().toLowerCase());
	}
	
	private me.libraryaddict.disguise.disguisetypes.Disguise getLibsDisguise(Player player) {
		return this.libsDisguises.get(player.getName().toLowerCase());
	}

	@Override
	public void destroy() {
		HandlerList.unregisterAll(this);
		removeAllDisguises();
		
		this.disguises.clear();
		this.libsDisguises.clear();
		this.disguisedEntityIds.clear();
		this.libsDisguisedEntityIds.clear();
		this.dragons.clear();
		this.mounts.clear();
		this.disguiseSpells.clear();
	}
	
	private me.libraryaddict.disguise.disguisetypes.Disguise getDisguiseLibDisguise(DisguiseSpell.Disguise dis) {
		me.libraryaddict.disguise.disguisetypes.Disguise ret;
		EntityType entityType = dis.getEntityType();
		DisguiseType disType = DisguiseType.getType(entityType);
		if (disType.isPlayer()) {
			ret = new PlayerDisguise(dis.getPlayer());
		} else if (disType.isMob()) {
			ret = new MobDisguise(disType);
		} else { //disType.isMisc()
			ret = new MiscDisguise(disType, dis.getVar1(), dis.getVar2());
		}
		
		FlagWatcher w = ret.getWatcher();
		
		if (disType.isMob()) {
			if (w instanceof AgeableWatcher) ((AgeableWatcher)w).setBaby(dis.getFlag());
			
			if (w instanceof SkeletonWatcher) {
				if (dis.getFlag()) ((SkeletonWatcher)w).setType(SkeletonType.WITHER);
			} else if (w instanceof CreeperWatcher) {
				CreeperWatcher c = (CreeperWatcher)w;
				c.setPowered(dis.getFlag());
			} else if (w instanceof GuardianWatcher) {
				((GuardianWatcher)w).setElder(dis.getFlag());
			} else if (w instanceof ZombieWatcher) {
				if (dis.getVar1() >= 1) {
					((ZombieWatcher)w).setProfession(dis.getVar1());
				}
			} else if (w instanceof BatWatcher) {
				((BatWatcher)w).setHanging(false);
			} else if (w instanceof VillagerWatcher) {
				((VillagerWatcher)w).setProfession(dis.getVar1());
			} else if (w instanceof SlimeWatcher) {
				((SlimeWatcher)w).setSize(2);
			} else if (w instanceof PigWatcher) {
				((PigWatcher)w).setSaddled(dis.getVar1() == 1);
			} else if (w instanceof RabbitWatcher) {
				((RabbitWatcher)w).setType(RabbitType.getType(dis.getVar1()));
			} else if (w instanceof SheepWatcher) {
				if (dis.getVar1() == -1) {
					((SheepWatcher)w).setColor(AnimalColor.getColor(this.random.nextInt(16)));
				} else if (dis.getVar1() >= 0 && dis.getVar1() < 16) {
					((SheepWatcher)w).setColor(AnimalColor.getColor(dis.getVar1()));
				}
			} else if (w instanceof OcelotWatcher) {
				if (dis.getVar1() == -1) {
					((OcelotWatcher)w).setType(Ocelot.Type.getType(this.random.nextInt(4)));
				} else if (dis.getVar1() >= 0 && dis.getVar1() < 4) {
					((OcelotWatcher)w).setType(Ocelot.Type.getType(dis.getVar1()));
				}
			} else if (w instanceof WolfWatcher) {
				WolfWatcher wolfWatch = (WolfWatcher)w;
				if (dis.getVar1() > 0) {
					wolfWatch.setTamed(true);
					wolfWatch.setOwner(dis.getPlayer().getUniqueId());
					wolfWatch.setCollarColor(AnimalColor.getColor(dis.getVar1()));
				}
			} else if (w instanceof HorseWatcher) {
				HorseWatcher h = (HorseWatcher)w;
				h.setVariant(dis.getVar1()); // Var 1 is horse type, donkey, mule, etc
				// Var2 is colors
				int colorId = dis.getVar2() % 256;
				int patternId = dis.getVar2()/256;
				Horse.Color horseColor;
				switch (colorId) {
				case 0:
					horseColor = Color.WHITE;
					break;
				case 1:
					horseColor = Color.CREAMY;
					break;
				case 2:
					horseColor = Color.CHESTNUT;
					break;
				case 3:
					horseColor = Color.BROWN;
					break;
				case 4:
					horseColor = Color.BLACK;
					break;
				case 5:
					horseColor = Color.GRAY;
					break;
				case 6:
					horseColor = Color.DARK_BROWN;
					break;
				default:
					horseColor = Color.WHITE;
				}
				
				Horse.Style horseStyle;
				switch (patternId) {
				case 0:
					horseStyle = Horse.Style.NONE;
					break;
				case 1:
					horseStyle = Horse.Style.WHITE;
					break;
				case 2:
					horseStyle = Horse.Style.WHITEFIELD;
					break;
				case 3:
					horseStyle = Horse.Style.WHITE_DOTS;
					break;
				case 4:
					horseStyle = Horse.Style.BLACK_DOTS;
					break;
				default:
					horseStyle = Horse.Style.NONE;
				}
				h.setStyle(horseStyle);
				h.setColor(horseColor);
				
				
				// Var 3 is armor
				ItemStack horseArmor = null;
				switch (dis.getVar3()) {
				case 0: // No armor, already null
					break;
				case 1: // Iron armor
					horseArmor = new ItemStack(Material.IRON_HORSE_ARMOR, 1);
					break;
				case 2:
					horseArmor = new ItemStack(Material.GOLDEN_HORSE_ARMOR, 1);
					break;
				case 3:
					horseArmor = new ItemStack(Material.DIAMOND_HORSE_ARMOR, 1);
					break;
				}
				
				h.setHorseArmor(horseArmor);
			}
		}
		
		if (dis.getNameplateText() != null && !dis.getNameplateText().isEmpty()) {
			w.setCustomName(dis.getNameplateText());
			w.setCustomNameVisible(dis.alwaysShowNameplate());
		}
		
		ret.setViewSelfDisguise(dis.disguiseSelf());
		
		/*
		 * EntityType entityType = disguise.getEntityType();
		String name = disguise.getNameplateText();
		if (name == null || name.isEmpty()) name = player.getName();
		if (entityType == EntityType.PLAYER) {
			entity = new EntityHuman(world, getGameProfile(name, disguise.getPlayerDisguiseData())) {
			};
		}
		
		 */
		return ret;
	}
	
	private void removeAllDisguises() {
		
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.disguisedEntityIds.remove(event.getPlayer().getEntityId());
		me.libraryaddict.disguise.disguisetypes.Disguise dis = this.libsDisguisedEntityIds.remove(event.getPlayer().getEntityId());
		if (dis != null && dis.isDisguiseInUse()) dis.stopDisguise();
		
		this.dragons.remove(event.getPlayer().getEntityId());
		if (this.mounts.containsKey(event.getPlayer().getEntityId())) {
			//sendDestroyEntityPackets(event.getPlayer(), mounts.remove(event.getPlayer().getEntityId()));
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if (isDisguised(p)) {
			me.libraryaddict.disguise.disguisetypes.Disguise libsDisguise = getLibsDisguise(p);
			this.disguisedEntityIds.put(p.getEntityId(), getDisguise(p));
			this.libsDisguisedEntityIds.put(p.getEntityId(), libsDisguise);
			if (getDisguise(p).getEntityType() == EntityType.ENDER_DRAGON) this.dragons.add(p.getEntityId());
			applyDisguise(p, libsDisguise);
		}
	}

}
