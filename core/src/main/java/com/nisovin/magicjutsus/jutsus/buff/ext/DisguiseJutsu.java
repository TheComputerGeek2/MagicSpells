package com.nisovin.magicjutsus.jutsus.buff.ext;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.EntityData;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.*;

// NOTE: LIBSDISGUISES IS REQUIRED FOR THIS
public class DisguiseJutsu extends BuffJutsu {

	private Set<UUID> entities;

	private EntityData entityData;

	private Disguise disguise;

	private String playerName;
	private String skinName;

	private short data;

	private boolean burning;
	private boolean glowing;

	public DisguiseJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		entities = new HashSet<>();

		playerName = getConfigString("player-name", "");
		skinName = getConfigString("skin-name", playerName);

		data = (short) getConfigInt("data", 0);

		burning = getConfigBoolean("burning", false);
		glowing = getConfigBoolean("glowing", false);

		ConfigurationSection disguiseSection = getConfigSection("disguise");
		if (disguiseSection != null) entityData = new EntityData(disguiseSection);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (entityData == null || entityData.getEntityType() == null) {
			MagicJutsus.error("DisguiseJutsu '" + internalName + "' has an invalid disguise defined!");
			return;
		}

		if (entityData.isPlayer()) disguise = new PlayerDisguise(playerName, skinName);
		else if (entityData.isMob()) disguise = new MobDisguise(DisguiseType.getType(entityData.getEntityType()), !entityData.isBaby());
		else if (entityData.isMisc()) disguise = new MiscDisguise(DisguiseType.getType(entityData.getEntityType()), entityData.getMaterial(), data);
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.add(entity.getUniqueId());
		if (disguise == null) return false;

		FlagWatcher watcher = disguise.getWatcher();

		watcher.setBurning(burning);
		watcher.setGlowing(glowing);

		if (watcher instanceof AbstractHorseWatcher) {
			((AbstractHorseWatcher) watcher).setCarryingChest(entityData.isChested());
			((AbstractHorseWatcher) watcher).setSaddled(entityData.isSaddled());
			if (watcher instanceof HorseWatcher) {
				((HorseWatcher) watcher).setColor(entityData.getHorseColor());
				((HorseWatcher) watcher).setStyle(entityData.getHorseStyle());
			}
			if (watcher instanceof LlamaWatcher) {
				((LlamaWatcher) watcher).setColor(entityData.getLlamaColor());
			}
		} else if (watcher instanceof CreeperWatcher) {
			((CreeperWatcher) watcher).setPowered(entityData.isPowered());
		} else if (watcher instanceof VillagerWatcher) {
			((VillagerWatcher) watcher).setProfession(entityData.getProfession());
		} else if (watcher instanceof SheepWatcher) {
			((SheepWatcher) watcher).setColor(entityData.getDyeColor());
			((SheepWatcher) watcher).setSheared(entityData.isSheared());
		} else if (watcher instanceof WolfWatcher) {
			((WolfWatcher) watcher).setCollarColor(entityData.getDyeColor());
			((WolfWatcher) watcher).setTamed(entityData.isTamed());
		} else if (watcher instanceof PigWatcher) {
			((PigWatcher) watcher).setSaddled(entityData.isSaddled());
		} else if (watcher instanceof FallingBlockWatcher) {
			((FallingBlockWatcher) watcher).setBlock(new ItemStack(entityData.getMaterial()));
		} else if (watcher instanceof DroppedItemWatcher) {
			((DroppedItemWatcher) watcher).setItemStack(new ItemStack(entityData.getMaterial()));
		} else if (watcher instanceof PufferFishWatcher) {
			((PufferFishWatcher) watcher).setPuffState(entityData.getSize());
		} else if (watcher instanceof TropicalFishWatcher) {
			((TropicalFishWatcher) watcher).setBodyColor(entityData.getDyeColor());
			((TropicalFishWatcher) watcher).setPatternColor(entityData.getPatternDyeColor());
			((TropicalFishWatcher) watcher).setPattern(entityData.getFishPattern());
		} else if (watcher instanceof ParrotWatcher) {
			((ParrotWatcher) watcher).setVariant(entityData.getParrotVariant());
		} else if (watcher instanceof SlimeWatcher) {
			((SlimeWatcher) watcher).setSize(entityData.getSize());
		} else if (watcher instanceof EndermanWatcher) {
			((EndermanWatcher) watcher).setItemInMainHand(entityData.getMaterial());
		}

		DisguiseAPI.disguiseEntity(entity, disguise);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		DisguiseAPI.undisguiseToAll(entity);
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			DisguiseAPI.undisguiseToAll(entity);
		}
		entities.clear();
	}

}
