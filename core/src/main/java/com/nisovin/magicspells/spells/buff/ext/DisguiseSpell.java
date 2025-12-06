package com.nisovin.magicspells.spells.buff.ext;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.DyeColor;
import org.bukkit.entity.Villager;
import org.bukkit.entity.LivingEntity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.*;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;

@DependsOn({"packetevents", "LibsDisguises"})
public class DisguiseSpell extends BuffSpell {

	private final Set<UUID> entities;

	private ConfigData<Disguise> disguiseData;

	private EntityData entityData;

	private final String playerName;
	private final String skinName;

	private final boolean burning;
	private final boolean glowing;

	public DisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();

		playerName = getConfigString("player-name", "");
		skinName = getConfigString("skin-name", playerName);

		burning = getConfigBoolean("burning", false);
		glowing = getConfigBoolean("glowing", false);

		if (!isConfigSection("disguise")) return;

		entityData = new EntityData(getConfigSection("disguise"));
		MagicSpells.error("DisguiseSpell '" + internalName + "' is using the legacy 'disguise' section, which is planned for removal. Please switch to a 'disguise' string.");
	}

	@Override
	public void initialize() {
		super.initialize();

		if (entityData != null) return;

		String disguiseString = getConfigString("disguise", null);
		if (disguiseString == null) {
			MagicSpells.error("DisguiseSpell '" + internalName + "' has an invalid 'disguise' defined!");
			return;
		}

		ConfigData<String> supplier = ConfigDataUtil.getString(disguiseString);
		if (supplier.isConstant()) {
			try {
				Disguise disguise = DisguiseParser.parseDisguise(disguiseString);
				disguiseData = data -> disguise;
			} catch (Throwable t) {
				MagicSpells.error("DisguiseSpell '" + internalName + "' has an invalid 'disguise' defined.");
				DebugHandler.debug(t);
			}

			return;
		}

		disguiseData = data -> {
			try {
				return DisguiseParser.parseDisguise(supplier.get(data));
			} catch (Throwable ignored) {
				return null;
			}
		};
	}

	@Override
	public boolean castBuff(SpellData data) {
		// STRING
		if (disguiseData != null) {
			Disguise disguise = disguiseData.get(data);
			if (disguise == null) return false;

			DisguiseAPI.disguiseEntity(data.target(), disguise);
			entities.add(data.target().getUniqueId());

			return true;
		}

		if (entityData == null) return false;

		DisguiseType disguiseType = DisguiseType.getType(entityData.getEntityType().get(data));

		Disguise disguise;
		if (disguiseType.isPlayer()) disguise = new PlayerDisguise(playerName, skinName);
		else if (disguiseType.isMob()) disguise = new MobDisguise(disguiseType);
		else if (disguiseType.isMisc()) disguise = new MiscDisguise(disguiseType);
		else return false;

		FlagWatcher watcher = disguise.getWatcher();
		watcher.setBurning(burning);
		watcher.setGlowing(glowing);

		if (watcher instanceof AgeableWatcher ageableWatcher)
			ageableWatcher.setBaby(entityData.getBaby().get(data));

		if (watcher instanceof AbstractHorseWatcher abstractHorseWatcher) {
			abstractHorseWatcher.setSaddled(entityData.getSaddled().get(data));

			if (abstractHorseWatcher instanceof ChestedHorseWatcher chestedHorseWatcher)
				chestedHorseWatcher.setCarryingChest(entityData.getChested().get(data));

			if (abstractHorseWatcher instanceof HorseWatcher horseWatcher) {
				horseWatcher.setColor(entityData.getHorseColor().get(data));
				horseWatcher.setStyle(entityData.getHorseStyle().get(data));
			}

			if (abstractHorseWatcher instanceof LlamaWatcher llamaWatcher)
				llamaWatcher.setColor(entityData.getLlamaColor().get(data));
		}

		if (watcher instanceof TameableWatcher tameableWatcher) {
			tameableWatcher.setTamed(entityData.getTamed().get(data));

			if (tameableWatcher instanceof ParrotWatcher parrotWatcher)
				parrotWatcher.setVariant(entityData.getParrotVariant().get(data));

			if (tameableWatcher instanceof WolfWatcher wolfWatcher) {
				DyeColor color = entityData.getColor().get(data);
				if (color != null) wolfWatcher.setCollarColor(color);
			}
		}

		if (watcher instanceof CreeperWatcher creeperWatcher)
			creeperWatcher.setPowered(entityData.getPowered().get(data));

		if (watcher instanceof DroppedItemWatcher droppedItemWatcher)
			droppedItemWatcher.setItemStack(entityData.getDroppedItemStack().get(data));

		if (watcher instanceof EndermanWatcher endermanWatcher)
			endermanWatcher.setHeldBlock(SpigotConversionUtil.fromBukkitBlockData(entityData.getCarriedBlockData().get(data)));

		if (watcher instanceof FallingBlockWatcher fallingBlockWatcher)
			fallingBlockWatcher.setBlockData(entityData.getFallingBlockData().get(data));

		if (watcher instanceof PigWatcher pigWatcher)
			pigWatcher.setSaddled(entityData.getSaddled().get(data));

		if (watcher instanceof PufferFishWatcher pufferFishWatcher)
			pufferFishWatcher.setPuffState(entityData.getSize().get(data));

		if (watcher instanceof SheepWatcher sheepWatcher) {
			DyeColor color = entityData.getColor().get(data);
			if (color != null) sheepWatcher.setColor(color);

			sheepWatcher.setSheared(entityData.getSheared().get(data));
		}

		if (watcher instanceof SlimeWatcher slimeWatcher)
			slimeWatcher.setSize(entityData.getSize().get(data));

		if (watcher instanceof TropicalFishWatcher tropicalFishWatcher) {
			tropicalFishWatcher.setBodyColor(entityData.getColor().get(data));
			tropicalFishWatcher.setPatternColor(entityData.getTropicalFishPatternColor().get(data));
			tropicalFishWatcher.setPattern(entityData.getTropicalFishPattern().get(data));
		}

		if (watcher instanceof VillagerWatcher villagerWatcher) {
			Villager.Profession profession = entityData.getProfession().get(data);
			if (profession != null) villagerWatcher.setProfession(profession);
		}

		DisguiseAPI.disguiseEntity(data.target(), disguise);
		entities.add(data.target().getUniqueId());

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
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities;
	}

	public Set<UUID> getEntities() {
		return entities;
	}

	public EntityData getEntityData() {
		return entityData;
	}

	public void setEntityData(EntityData entityData) {
		this.entityData = entityData;
	}

}
