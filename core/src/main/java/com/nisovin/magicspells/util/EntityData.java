package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiConsumer;

import org.joml.Vector3f;
import org.joml.Quaternionf;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.kyori.adventure.util.TriState;
import net.kyori.adventure.text.Component;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.data.BlockData;
import org.bukkit.attribute.Attributable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.entity.Frictional;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.entity.CollarColorable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ai.CustomGoal;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.itemreader.AttributeHandler;

public class EntityData {

	private final Multimap<EntityType, Transformer<?>> options = MultimapBuilder.enumKeys(EntityType.class).arrayListValues().build();
	private final List<DelayedEntityData> delayedEntityData = new ArrayList<>();

	private ConfigData<EntityType> entityType;

	private final ConfigData<Angle> yaw;
	private final ConfigData<Angle> pitch;
	private final ConfigData<Vector> relativeOffset;

	// Legacy support for DisguiseSpell section format

	// Ageable
	private final ConfigData<Boolean> baby;

	// AbstractHorse & Pig
	private final ConfigData<Boolean> saddled;

	// ChestedHorse
	private final ConfigData<Boolean> chested;

	// Creeper
	private final ConfigData<Boolean> powered;

	// Enderman
	private final ConfigData<BlockData> carriedBlockData;

	// Falling Block
	private final ConfigData<BlockData> fallingBlockData;

	// Horse
	private final ConfigData<Horse.Color> horseColor;
	private final ConfigData<Horse.Style> horseStyle;

	// Item
	private final ConfigData<ItemStack> dropItem;

	// Llama
	private final ConfigData<Llama.Color> llamaColor;

	// Parrot
	private final ConfigData<Parrot.Variant> parrotVariant;

	// Puffer Fish & Slime
	private final ConfigData<Integer> size;

	// Sheep
	private final ConfigData<Boolean> sheared;

	// Sheep & Tropical Fish & Wolf
	private final ConfigData<DyeColor> color;

	// Tameable
	private final ConfigData<Boolean> tamed;

	// Tropical Fish
	private final ConfigData<TropicalFish.Pattern> tropicalFishPattern;
	private final ConfigData<DyeColor> tropicalFishPatternColor;

	// Villager
	private final ConfigData<Villager.Profession> profession;

	public EntityData(ConfigurationSection config) {
		this(config, false);
	}

	public EntityData(ConfigurationSection config, boolean forceOptional) {
		entityType = ConfigDataUtil.getEntityType(config, "entity", null);

		yaw = ConfigDataUtil.getAngle(config, "yaw", Angle.DEFAULT);
		pitch = ConfigDataUtil.getAngle(config, "pitch", Angle.DEFAULT);
		relativeOffset = ConfigDataUtil.getVector(config, "relative-offset", new Vector(0, 0, 0));

		Multimap<Class<?>, Transformer<?>> transformers = MultimapBuilder.linkedHashKeys().arrayListValues().build();

		// Entity
		addOptBoolean(transformers, config, "silent", Entity.class, Entity::setSilent);
		addOptBoolean(transformers, config, "glowing", Entity.class, Entity::setGlowing);
		addOptBoolean(transformers, config, "gravity", Entity.class, Entity::setGravity);
		addOptBoolean(transformers, config, "no-physics", Entity.class, Entity::setNoPhysics);
		addOptBoolean(transformers, config, "persistent", Entity.class, Entity::setPersistent);
		addOptBoolean(transformers, config, "visual-fire", Entity.class, Entity::setVisualFire);
		addOptBoolean(transformers, config, "invulnerable", Entity.class, Entity::setInvulnerable);
		addOptBoolean(transformers, config, "visible-by-default", Entity.class, Entity::setVisibleByDefault);
		addOptBoolean(transformers, config, "custom-name-visible", Entity.class, Entity::setCustomNameVisible);

		addOptInteger(transformers, config, "fire-ticks", Entity.class, Entity::setFireTicks);
		addOptInteger(transformers, config, "freeze-ticks", Entity.class, Entity::setFreezeTicks);

		addOptVector(transformers, config, "velocity", Entity.class, Entity::setVelocity);

		for (String tagString : config.getStringList("scoreboard-tags")) {
			ConfigData<String> tag = ConfigDataUtil.getString(tagString);
			transformers.put(Entity.class, (Entity entity, SpellData data) -> entity.addScoreboardTag(tag.get(data)));
		}

		// Ageable
		baby = addBoolean(transformers, config, "baby", false, Ageable.class, (ageable, baby) -> {
			if (baby) ageable.setBaby();
			else ageable.setAdult();
		}, forceOptional);

		addOptInteger(transformers, config, "age", Ageable.class, Ageable::setAge);

		// Attributable
		List<?> attributeModifierStrings = config.getList("attribute-modifiers", new ArrayList<>());
		Multimap<Attribute, AttributeModifier> attributeModifiers = AttributeHandler.getAttributeModifiers(attributeModifierStrings, null);
		if (!attributeModifiers.isEmpty()) {
			transformers.put(Attributable.class, (Attributable entity, SpellData data) -> {
				attributeModifiers.asMap().forEach((attribute, modifiers) -> {
					AttributeInstance attributeInstance = entity.getAttribute(attribute);
					if (attributeInstance == null) return;

					modifiers.forEach(attributeInstance::addModifier);
				});
			});
		}

		// Damageable
		addOptDouble(transformers, config, "health", Damageable.class, Damageable::setHealth);

		// Nameable
		addOptComponent(transformers, config, "custom-name", Nameable.class, Nameable::customName);

		// LivingEntity
		addOptBoolean(transformers, config, "ai", LivingEntity.class, LivingEntity::setAI);
		addOptBoolean(transformers, config, "can-pickup-items", LivingEntity.class, LivingEntity::setCanPickupItems);

		addOptDouble(transformers, config, "max-health", LivingEntity.class, Util::setMaxHealth);

		addOptEquipment(transformers, config, "equipment.main-hand", EquipmentSlot.HAND);
		addOptEquipment(transformers, config, "equipment.off-hand", EquipmentSlot.OFF_HAND);
		addOptEquipment(transformers, config, "equipment.helmet", EquipmentSlot.HEAD);
		addOptEquipment(transformers, config, "equipment.chestplate", EquipmentSlot.CHEST);
		addOptEquipment(transformers, config, "equipment.leggings", EquipmentSlot.LEGS);
		addOptEquipment(transformers, config, "equipment.boots", EquipmentSlot.FEET);
		addOptEquipment(transformers, config, "equipment.body", Mob.class, EquipmentSlot.BODY);

		List<ConfigData<PotionEffect>> potionEffects = Util.getPotionEffects(config.getList("potion-effects", new ArrayList<>()), null);
		if (potionEffects != null) {
			transformers.put(LivingEntity.class, (LivingEntity entity, SpellData data) -> {
				potionEffects.forEach(effect -> entity.addPotionEffect(effect.get(data)));
			});
		}

		// Mob
		addOptBoolean(transformers, config, "aware", Mob.class, Mob::setAware);

		addOptEquipmentDropChance(transformers, config, "equipment.main-hand-drop-chance", EquipmentSlot.HAND);
		addOptEquipmentDropChance(transformers, config, "equipment.off-hand-drop-chance", EquipmentSlot.OFF_HAND);
		addOptEquipmentDropChance(transformers, config, "equipment.helmet-drop-chance", EquipmentSlot.HEAD);
		addOptEquipmentDropChance(transformers, config, "equipment.chestplate-drop-chance", EquipmentSlot.CHEST);
		addOptEquipmentDropChance(transformers, config, "equipment.leggings-drop-chance", EquipmentSlot.LEGS);
		addOptEquipmentDropChance(transformers, config, "equipment.boots-drop-chance", EquipmentSlot.FEET);
		addOptEquipmentDropChance(transformers, config, "equipment.body-drop-chance", EquipmentSlot.BODY);

		// Tameable
		tamed = addBoolean(transformers, config, "tamed", false, Tameable.class, Tameable::setTamed, forceOptional);
		if (config.getBoolean("tamed-owner")) {
			transformers.put(Tameable.class, (Tameable tameable, SpellData data) -> {
				if (!(data.recipient() instanceof AnimalTamer tamer)) return;
				tameable.setOwner(tamer);
			});
		}

		// AbstractHorse
		saddled = addBoolean(transformers, config, "saddled", false, AbstractHorse.class, (horse, saddled) -> {
			if (saddled) horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
		}, forceOptional);

		// Armor Stand
		addBoolean(transformers, config, "small", false, ArmorStand.class, ArmorStand::setSmall, forceOptional);
		addBoolean(transformers, config, "marker", false, ArmorStand.class, ArmorStand::setMarker, forceOptional);
		addBoolean(transformers, config, "visible", true, ArmorStand.class, ArmorStand::setVisible, forceOptional);
		addBoolean(transformers, config, "has-arms", true, ArmorStand.class, ArmorStand::setArms, forceOptional);
		addBoolean(transformers, config, "has-base-plate", true, ArmorStand.class, ArmorStand::setBasePlate, forceOptional);
		addBoolean(transformers, config, "disable-slots", false, ArmorStand.class, (stand, disabled) -> {
			if (disabled) stand.setDisabledSlots(EquipmentSlot.values());
		}, forceOptional);

		addEulerAngle(transformers, config, "head-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setHeadPose, forceOptional);
		addEulerAngle(transformers, config, "body-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setBodyPose, forceOptional);
		addEulerAngle(transformers, config, "left-arm-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setLeftArmPose, forceOptional);
		addEulerAngle(transformers, config, "right-arm-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setRightArmPose, forceOptional);
		addEulerAngle(transformers, config, "left-leg-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setLeftLegPose, forceOptional);
		addEulerAngle(transformers, config, "right-leg-angle", EulerAngle.ZERO, ArmorStand.class, ArmorStand::setRightLegPose, forceOptional);

		for (String slotName : config.getStringList("disable-slots")) {
			ConfigData<EquipmentSlot> slotData = ConfigDataUtil.getEnum(slotName, EquipmentSlot.class, null);

			transformers.put(ArmorStand.class, (ArmorStand stand, SpellData data) -> {
				EquipmentSlot slot = slotData.get(data);
				if (slot == null) return;

				stand.addDisabledSlots(slot);
			});
		}

		for (Object object : config.getList("equipment-locks", new ArrayList<>())) {
			if (!(object instanceof Map<?,?> map)) continue;
			ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

			ConfigData<EquipmentSlot> slotData = ConfigDataUtil.getEnum(section, "slot", EquipmentSlot.class, null);
			ConfigData<ArmorStand.LockType> lockData = ConfigDataUtil.getEnum(section, "lock", ArmorStand.LockType.class, null);

			transformers.put(ArmorStand.class, (ArmorStand stand, SpellData data) -> {
				EquipmentSlot slot = slotData.get(data);
				ArmorStand.LockType lock = lockData.get(data);
				if (slot == null || lock == null) return;

				stand.addEquipmentLock(slot, lock);
			});
		}

		// Axolotl
		fallback(
			key -> addOptEnum(transformers, config, key, Axolotl.class, Axolotl.Variant.class, Axolotl::setVariant),
			"axolotl-variant", "type"
		);

		// Cat
		fallback(
			key -> addOptRegistryEntry(transformers, config, key, Cat.class, Registry.CAT_VARIANT, Cat::setCatType),
			"cat-variant", "type"
		);

		// CollarColorable
		fallback(
			key -> addOptEnum(transformers, config, key, CollarColorable.class, DyeColor.class, CollarColorable::setCollarColor),
			"collar-color", "color"
		);

		// CommandMinecart
		addOptString(transformers, config, "command", CommandMinecart.class, CommandMinecart::setCommand);

		// ChestedHorse
		chested = addBoolean(transformers, config, "chested", false, ChestedHorse.class, ChestedHorse::setCarryingChest, forceOptional);

		// Creeper
		powered = addBoolean(transformers, config, "powered", false, Creeper.class, Creeper::setPowered, forceOptional);

		// Enderman
		carriedBlockData = fallback(
			key -> addBlockData(transformers, config, key, null, Enderman.class, Enderman::setCarriedBlock, forceOptional),
			"carried-block", "material"
		);

		// Falling Block
		fallingBlockData = fallback(
			key -> addOptBlockData(transformers, config, key, FallingBlock.class, FallingBlock::setBlockData),
			"falling-block", "material"
		);

		addOptBoolean(transformers, config, "cancel-drop", FallingBlock.class, FallingBlock::setCancelDrop);
		addOptBoolean(transformers, config, "hurt-entities", FallingBlock.class, FallingBlock::setHurtEntities);

		addOptFloat(transformers, config, "damage-per-block", FallingBlock.class, FallingBlock::setDamagePerBlock);

		addOptInteger(transformers, config, "max-damage", FallingBlock.class, FallingBlock::setMaxDamage);

		// Fox
		fallback(
			key -> addOptEnum(transformers, config, key, Fox.class, Fox.Type.class, Fox::setFoxType),
			"fox-type", "type"
		);

		// Frictional
		addOptEnum(transformers, config, "friction-state", Frictional.class, TriState.class, Frictional::setFrictionState);

		// Frog
		fallback(
			key -> addOptRegistryEntry(transformers, config, key, Frog.class, Registry.FROG_VARIANT, Frog::setVariant),
			"frog-variant", "type"
		);

		// Goat
		addOptBoolean(transformers, config, "left-horn", Goat.class, Goat::setLeftHorn);
		addOptBoolean(transformers, config, "right-horn", Goat.class, Goat::setRightHorn);
		addOptBoolean(transformers, config, "screaming", Goat.class, Goat::setScreaming);

		// Hoglin
		addOptBoolean(transformers, config, "immune-to-zombification", Hoglin.class, Hoglin::setImmuneToZombification);
		addOptBoolean(transformers, config, "able-to-be-hunted", Hoglin.class, Hoglin::setIsAbleToBeHunted);

		// Horse
		horseColor = fallback(
			key -> addOptEnum(transformers, config, key, Horse.class, Horse.Color.class, Horse::setColor),
			"horse-color", "color"
		);
		horseStyle = fallback(
			key -> addOptEnum(transformers, config, key, Horse.class, Horse.Style.class, Horse::setStyle),
			"horse-style", "style"
		);

		// Item
		dropItem = fallback(
			key -> addOptItemStack(transformers, config, key, Item.class, Item::setItemStack),
			"material", "item"
		);

		addOptInteger(transformers, config, "pickup-delay", Item.class, Item::setPickupDelay);

		addOptBoolean(transformers, config, "will-age", Item.class, Item::setWillAge);
		addOptBoolean(transformers, config, "can-mob-pickup", Item.class, Item::setCanMobPickup);
		addOptBoolean(transformers, config, "can-player-pickup", Item.class, Item::setCanPlayerPickup);

		// Interaction
		addOptFloat(transformers, config, "interaction-height", Interaction.class, Interaction::setInteractionHeight);
		addOptFloat(transformers, config, "interaction-width", Interaction.class, Interaction::setInteractionWidth);
		addOptBoolean(transformers, config, "responsive", Interaction.class, Interaction::setResponsive);

		// Llama
		llamaColor = fallback(
			key -> addOptEnum(transformers, config, key, Llama.class, Llama.Color.class, Llama::setColor),
			"llama-variant", "color"
		);
		fallback(
			key -> addOptMaterial(transformers, config, key, Llama.class, (llama, material) -> llama.getInventory().setDecor(new ItemStack(material))),
			"llama-decor", "material"
		);

		// Minecart
		addOptDouble(transformers, config, "max-speed", Minecart.class, Minecart::setMaxSpeed);

		addOptBoolean(transformers, config, "slow-when-empty", Minecart.class, Minecart::setSlowWhenEmpty);

		addOptInteger(transformers, config, "display-block-offset", Minecart.class, Minecart::setDisplayBlockOffset);

		addOptBlockData(transformers, config, "display-block", Minecart.class, Minecart::setDisplayBlockData);

		// Mushroom Cow
		fallback(
			key -> addOptEnum(transformers, config, key, MushroomCow.class, MushroomCow.Variant.class, MushroomCow::setVariant),
			"mooshroom-type", "type"
		);

		// Panda
		addOptEnum(transformers, config, "main-gene", Panda.class, Panda.Gene.class, Panda::setMainGene);
		addOptEnum(transformers, config, "hidden-gene", Panda.class, Panda.Gene.class, Panda::setHiddenGene);

		// Parrot
		parrotVariant = fallback(
			key -> addOptEnum(transformers, config, key, Parrot.class, Parrot.Variant.class, Parrot::setVariant),
			"parrot-variant", "type"
		);

		// Phantom
		addInteger(transformers, config, "size", 0, Phantom.class, Phantom::setSize, forceOptional);
		addOptBoolean(transformers, config, "should-burn-in-day", Phantom.class, Phantom::setShouldBurnInDay);

		// Piglin
		addOptBoolean(transformers, config, "able-to-hunt", Piglin.class, Piglin::setIsAbleToHunt);
		addOptInteger(transformers, config, "dancing", Piglin.class, Piglin::setDancing);

		// Piglin Abstract
		addOptBoolean(transformers, config, "immune-to-zombification", PiglinAbstract.class, PiglinAbstract::setImmuneToZombification);

		// Puffer Fish
		size = addInteger(transformers, config, "size", 0, PufferFish.class, PufferFish::setPuffState, forceOptional);

		// Rabbit
		fallback(
			key -> addOptEnum(transformers, config, key, Rabbit.class, Rabbit.Type.class, Rabbit::setRabbitType),
			"rabbit-type", "type"
		);

		// Raider
		addOptBoolean(transformers, config, "patrol-leader", Raider.class, Raider::setPatrolLeader);
		addOptBoolean(transformers, config, "can-join-raid", Raider.class, Raider::setCanJoinRaid);
		addOptBoolean(transformers, config, "celebrating", Raider.class, Raider::setCelebrating);

		// Sheep
		sheared = addBoolean(transformers, config, "sheared", false, Sheep.class, Sheep::setSheared, forceOptional);
		color = fallback(
			key -> addOptEnum(transformers, config, key, Sheep.class, DyeColor.class, Sheep::setColor),
			"sheep-color", "color"
		);

		// Shulker
		fallback(
			key -> addOptEnum(transformers, config, key, Shulker.class, DyeColor.class, Shulker::setColor),
			"shulker-color", "color"
		);

		// Skeleton
		addOptBoolean(transformers, config, "should-burn-in-day", Skeleton.class, Skeleton::setShouldBurnInDay);

		// Slime
		addInteger(transformers, config, "size", 0, Slime.class, Slime::setSize, forceOptional);

		// Steerable
		addBoolean(transformers, config, "saddled", false, Steerable.class, Steerable::setSaddle, forceOptional);

		// Tropical Fish
		fallback(
			key -> addOptEnum(transformers, config, key, TropicalFish.class, DyeColor.class, TropicalFish::setBodyColor),
			"tropical-fish.body-color", "color"
		);
		tropicalFishPatternColor = fallback(
			key -> addOptEnum(transformers, config, key, TropicalFish.class, DyeColor.class, TropicalFish::setPatternColor),
			"tropical-fish.pattern-color", "pattern-color"
		);
		tropicalFishPattern = fallback(
			key -> addOptEnum(transformers, config, key, TropicalFish.class, TropicalFish.Pattern.class, TropicalFish::setPattern),
			"tropical-fish.pattern", "type"
		);

		// Salmon
		addOptEnum(transformers, config, "salmon-variant", Salmon.class, Salmon.Variant.class, Salmon::setVariant);

		// Villager
		profession = fallback(
			key -> addOptRegistryEntry(transformers, config, key, Villager.class, Registry.VILLAGER_PROFESSION, Villager::setProfession),
			"villager-profession", "type"
		);
		addOptRegistryEntry(transformers, config, "villager-type", Villager.class, Registry.VILLAGER_TYPE, Villager::setVillagerType);

		// Vindicator
		addOptBoolean(transformers, config, "johnny", Vindicator.class, Vindicator::setJohnny);

		// Wolf
		addBoolean(transformers, config, "angry", false, Wolf.class, Wolf::setAngry, forceOptional);
		addOptRegistryEntry(transformers, config, "wolf-variant", Wolf.class, RegistryKey.WOLF_VARIANT, Wolf::setVariant);

		// Zombie
		addOptBoolean(transformers, config, "should-burn-in-day", Zombie.class, Zombie::setShouldBurnInDay);

		// Zombie Villager
		addOptRegistryEntry(transformers, config, "villager-profession", ZombieVillager.class, Registry.VILLAGER_PROFESSION, ZombieVillager::setVillagerProfession);
		addOptRegistryEntry(transformers, config, "villager-type", ZombieVillager.class, Registry.VILLAGER_TYPE, ZombieVillager::setVillagerType);

		// Display
		ConfigData<Quaternionf> leftRotation = getQuaternion(config, "transformation.left-rotation");
		ConfigData<Quaternionf> rightRotation = getQuaternion(config, "transformation.right-rotation");
		ConfigData<Vector3f> translation = getVector(config, "transformation.translation");
		ConfigData<Vector3f> scale = getVector(config, "transformation.scale");
		ConfigData<Transformation> transformation = data -> null;
		if (!leftRotation.isNull() && !rightRotation.isNull() && !translation.isNull() && !scale.isNull()) {
			if (leftRotation.isConstant() && rightRotation.isConstant() && translation.isConstant() && scale.isConstant()) {
				Quaternionf lr = leftRotation.get();
				Quaternionf rr = rightRotation.get();
				Vector3f t = translation.get();
				Vector3f s = scale.get();

				Transformation transform = new Transformation(t, lr, s, rr);
				transformation = data -> transform;
			} else {
				transformation = data -> {
					Quaternionf lr = leftRotation.get(data);
					if (lr == null) return null;

					Quaternionf rr = rightRotation.get(data);
					if (rr == null) return null;

					Vector3f t = translation.get(data);
					if (t == null) return null;

					Vector3f s = scale.get(data);
					if (s == null) return null;

					return new Transformation(t, lr, s, rr);
				};
			}
		}
		transformers.put(Display.class, new TransformerImpl<>(transformation, Display::setTransformation, true));

		addOptInteger(transformers, config, "teleport-duration", Display.class, Display::setTeleportDuration);
		addOptInteger(transformers, config, "interpolation-duration", Display.class, Display::setInterpolationDuration);
		addOptFloat(transformers, config, "view-range", Display.class, Display::setViewRange);
		addOptFloat(transformers, config, "shadow-radius", Display.class, Display::setShadowRadius);
		addOptFloat(transformers, config, "shadow-strength", Display.class, Display::setShadowStrength);
		addOptFloat(transformers, config, "width", Display.class, Display::setDisplayWidth);
		addOptFloat(transformers, config, "height", Display.class, Display::setDisplayHeight);
		addOptInteger(transformers, config, "interpolation-delay", Display.class, Display::setInterpolationDelay);
		addOptEnum(transformers, config, "billboard", Display.class, Display.Billboard.class, Display::setBillboard);
		addOptARGBColor(transformers, config, "glow-color-override", Display.class, Display::setGlowColorOverride);

		ConfigData<Integer> blockLight = ConfigDataUtil.getInteger(config, "brightness.block");
		ConfigData<Integer> skyLight = ConfigDataUtil.getInteger(config, "brightness.sky");
		ConfigData<Display.Brightness> brightness = data -> null;
		if (!blockLight.isNull() && !skyLight.isNull()) {
			if (blockLight.isConstant() && skyLight.isConstant()) {
				int bl = blockLight.get();
				int sl = skyLight.get();

				if (0 <= bl && bl <= 15 && 0 <= sl && sl <= 15) {
					Display.Brightness b = new Display.Brightness(bl, sl);
					brightness = data -> b;
				}
			} else {
				brightness = data -> {
					Integer bl = blockLight.get(data);
					if (bl == null || bl < 0 || bl > 15) return null;

					Integer sl = skyLight.get(data);
					if (sl == null || sl < 0 || sl > 15) return null;

					return new Display.Brightness(bl, sl);
				};
			}
		}
		transformers.put(Display.class, new TransformerImpl<>(brightness, Display::setBrightness, true));

		// BlockDisplay
		addOptBlockData(transformers, config, "block", BlockDisplay.class, BlockDisplay::setBlock);

		// ItemDisplay
		addOptItemStack(transformers, config, "item", ItemDisplay.class, ItemDisplay::setItemStack);

		addOptEnum(transformers, config, "item-display-transform", ItemDisplay.class, ItemDisplay.ItemDisplayTransform.class, ItemDisplay::setItemDisplayTransform);

		// TextDisplay
		addOptComponent(transformers, config, "text", TextDisplay.class, TextDisplay::text);
		addOptInteger(transformers, config, "line-width", TextDisplay.class, TextDisplay::setLineWidth);
		addOptARGBColor(transformers, config, "background", TextDisplay.class, TextDisplay::setBackgroundColor);
		addOptByte(transformers, config, "text-opacity", TextDisplay.class, TextDisplay::setTextOpacity);
		addOptBoolean(transformers, config, "shadow", TextDisplay.class, TextDisplay::setShadowed);
		addOptBoolean(transformers, config, "see-through", TextDisplay.class, TextDisplay::setSeeThrough);
		addOptBoolean(transformers, config, "default-background", TextDisplay.class, TextDisplay::setDefaultBackground);
		addOptEnum(transformers, config, "alignment", TextDisplay.class, TextDisplay.TextAlignment.class, TextDisplay::setAlignment);

		// Mob Goals
		ConfigurationSection mobGoals = config.getConfigurationSection("mob-goals");
		if (mobGoals != null) {
			if (mobGoals.getBoolean("remove-all"))
				transformers.put(Mob.class, (Mob mob, SpellData data) -> Bukkit.getMobGoals().removeAllGoals(mob));

			for (String string : mobGoals.getStringList("remove-types")) {
				ConfigData<GoalType> typeData = ConfigDataUtil.getEnum(string, GoalType.class, null);

				transformers.put(Mob.class, (Mob mob, SpellData data) -> {
					GoalType type = typeData.get(data);
					if (type == null) return;

					Bukkit.getMobGoals().removeAllGoals(mob, type);
				});
			}

			for (String string : mobGoals.getStringList("remove")) {
				ConfigData<NamespacedKey> keyData = ConfigDataUtil.getNamespacedKey(string, null);

				transformers.put(Mob.class, (Mob mob, SpellData data) -> {
					NamespacedKey key = keyData.get(data);
					if (key == null) return;

					Bukkit.getMobGoals().removeGoal(mob, GoalKey.of(Mob.class, key));
				});
			}

			for (String string : mobGoals.getStringList("remove-vanilla")) {
				ConfigData<String> stringData = ConfigDataUtil.getString(string);

				transformers.put(Mob.class, (Mob mob, SpellData data) -> {
					String value = stringData.get(data);
					if (value == null) return;

					GoalKey<?> key = MagicSpells.getCustomGoalsManager().getVanillaGoal(value);
					if (key == null) return;

					// We have to loop through because casting to parameter types is tricky.
					// It loops through on each MobGoals#removeGoal call anyway.
					for (Goal<@NotNull Mob> goal : Bukkit.getMobGoals().getAllGoals(mob)) {
						if (!goal.getKey().equals(key)) continue;
						Bukkit.getMobGoals().removeGoal(mob, goal);
					}
				});
			}

			for (Object object : mobGoals.getList("add", new ArrayList<>())) {
				if (!(object instanceof Map<?, ?> map)) continue;
				ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

				ConfigData<Integer> priorityData = ConfigDataUtil.getInteger(section, "priority", 0);
				ConfigData<String> goalNameData = ConfigDataUtil.getString(section, "goal", "");

				ConfigurationSection goalSection = section.getConfigurationSection("data");
				if (goalSection == null) continue;

				transformers.put(Mob.class, (Mob mob, SpellData data) -> {
					int priority = priorityData.get(data);
					String goalName = goalNameData.get(data);

					CustomGoal goal = MagicSpells.getCustomGoalsManager().getGoal(goalName, mob, data);
					if (goal == null) return;

					boolean success = goal.initialize(goalSection);
					if (success) Bukkit.getMobGoals().addGoal(mob, priority, goal);
				});
			}
		}

		// Apply transformers
		for (EntityType entityType : EntityType.values()) {
			Class<? extends Entity> entityClass = entityType.getEntityClass();
			if (entityClass == null) continue;

			for (Class<?> transformerType : transformers.keySet())
				if (transformerType.isAssignableFrom(entityClass))
					options.putAll(entityType, transformers.get(transformerType));
		}

		// Delayed Entity Data
		List<?> delayedDataEntries = config.getList("delayed-entity-data");
		if (delayedDataEntries == null || delayedDataEntries.isEmpty()) return;

		for (Object object : delayedDataEntries) {
			if (!(object instanceof Map<?, ?> map)) continue;

			ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

			ConfigurationSection dataSection = section.getConfigurationSection("entity-data");
			if (dataSection == null) continue;

			ConfigData<Long> delay = ConfigDataUtil.getLong(section, "delay", 1);
			ConfigData<Long> interval = ConfigDataUtil.getLong(section, "interval", 0);
			ConfigData<Long> iterations = ConfigDataUtil.getLong(section, "iterations", 0);

			EntityData entityData = new EntityData(dataSection, true);
			delayedEntityData.add(new DelayedEntityData(entityData, delay, interval, iterations));
		}
	}

	@Nullable
	public Entity spawn(@NotNull Location location) {
		return spawn(location, SpellData.NULL, null);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @Nullable Consumer<Entity> postConsumer) {
		return spawn(location, SpellData.NULL, postConsumer);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @Nullable Consumer<Entity> preConsumer, @Nullable Consumer<Entity> postConsumer) {
		return spawn(location, SpellData.NULL, preConsumer, postConsumer);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @NotNull SpellData data, @Nullable Consumer<Entity> postConsumer) {
		return spawn(location, data, (Consumer<Entity>) null, postConsumer);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @NotNull SpellData data, @Nullable Consumer<Entity> preConsumer, @Nullable Consumer<Entity> postConsumer) {
		EntityType type = this.entityType.get(data);
		if (type == null || !type.isSpawnable()) return null;

		Class<? extends Entity> entityClass = type.getEntityClass();
		if (entityClass == null) return null;

		return spawn(location, data, entityClass, entity -> {
			if (preConsumer != null) preConsumer.accept(entity);
		}, entity -> {
			if (postConsumer != null) postConsumer.accept(entity);
		});
	}

	@NotNull
	public <T extends Entity> T spawn(@NotNull Location location, @NotNull SpellData data, @NotNull Class<T> entityClass, @Nullable Consumer<T> postConsumer) {
		return spawn(location, data, entityClass, null, postConsumer);
	}

	@NotNull
	public <T extends Entity> T spawn(@NotNull Location location, @NotNull SpellData data, @NotNull Class<T> entityClass, @Nullable Consumer<T> preConsumer, @Nullable Consumer<T> postConsumer) {
		Location spawnLocation = location.clone();

		Vector relativeOffset = this.relativeOffset.get(data);
		spawnLocation.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(spawnLocation, relativeOffset.setY(0));

		spawnLocation.setYaw(yaw.get(data).apply(spawnLocation.getYaw()));
		spawnLocation.setPitch(pitch.get(data).apply(spawnLocation.getPitch()));

		return spawnLocation.getWorld().spawn(spawnLocation, entityClass, entity -> {
			if (preConsumer != null) preConsumer.accept(entity);

			apply(entity, data);

			if (postConsumer != null) postConsumer.accept(entity);
		});
	}

	public void apply(@NotNull Entity entity, @NotNull SpellData data) {
		Collection<Transformer<?>> transformers = options.get(entity.getType());
		//noinspection rawtypes
		for (Transformer transformer : transformers)
			//noinspection unchecked
			transformer.apply(entity, data);

		for (DelayedEntityData delayedData : delayedEntityData) {
			EntityData entityData = delayedData.data;

			long delay = Math.max(delayedData.delay.get(data), 1);
			long interval = delayedData.interval.get(data);
			long iterations = delayedData.iterations.get(data);

			if (interval > 0) {
				if (iterations > 0) {
					entity.getScheduler().runAtFixedRate(
						MagicSpells.getInstance(),
						new Consumer<>() {

							private long count = 0;

							@Override
							public void accept(ScheduledTask task) {
								entityData.apply(entity, data);
								if (++count > iterations) task.cancel();
							}

						},
						() -> {},
						delay,
						interval
					);
				} else {
					entity.getScheduler().runAtFixedRate(
						MagicSpells.getInstance(),
						task -> entityData.apply(entity, data),
						() -> {},
						delay,
						interval
					);
				}
			} else {
				entity.getScheduler().runDelayed(
					MagicSpells.getInstance(),
					task -> entityData.apply(entity, data),
					() -> {},
					delay
				);
			}
		}
	}

	private <T> ConfigData<Boolean> addBoolean(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, boolean def, Class<T> type, BiConsumer<T, Boolean> setter, boolean forceOptional) {
		ConfigData<Boolean> supplier;
		if (forceOptional) {
			supplier = ConfigDataUtil.getBoolean(config, name);
			transformers.put(type, new TransformerImpl<>(supplier, setter, true));
		} else {
			supplier = ConfigDataUtil.getBoolean(config, name, def);
			transformers.put(type, new TransformerImpl<>(supplier, setter));
		}

		return supplier;
	}

	private <T> ConfigData<Integer> addInteger(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, int def, Class<T> type, BiConsumer<T, Integer> setter, boolean forceOptional) {
		ConfigData<Integer> supplier;
		if (forceOptional) {
			supplier = ConfigDataUtil.getInteger(config, name);
			transformers.put(type, new TransformerImpl<>(supplier, setter, true));
		} else {
			supplier = ConfigDataUtil.getInteger(config, name, def);
			transformers.put(type, new TransformerImpl<>(supplier, setter));
		}

		return supplier;
	}

	private <T> ConfigData<BlockData> addBlockData(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, BlockData def, Class<T> type, BiConsumer<T, BlockData> setter, boolean forceOptional) {
		ConfigData<BlockData> supplier;
		if (forceOptional) {
			supplier = ConfigDataUtil.getBlockData(config, name, null);
			transformers.put(type, new TransformerImpl<>(supplier, setter, true));
		} else {
			supplier = ConfigDataUtil.getBlockData(config, name, def);
			transformers.put(type, new TransformerImpl<>(supplier, setter));
		}

		return supplier;
	}

	private <T> void addEulerAngle(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, EulerAngle def, Class<T> type, BiConsumer<T, EulerAngle> setter, boolean forceOptional) {
		if (forceOptional) {
			ConfigData<EulerAngle> supplier = ConfigDataUtil.getEulerAngle(config, name, null);
			transformers.put(type, new TransformerImpl<>(supplier, setter, true));
		} else {
			ConfigData<EulerAngle> supplier = ConfigDataUtil.getEulerAngle(config, name, def);
			transformers.put(type, new TransformerImpl<>(supplier, setter));
		}
	}

	private <T> void addOptBoolean(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Boolean> setter) {
		ConfigData<Boolean> supplier = ConfigDataUtil.getBoolean(config, name);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptByte(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Byte> setter) {
		ConfigData<Byte> supplier = ConfigDataUtil.getByte(config, name);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptInteger(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Integer> setter) {
		ConfigData<Integer> supplier = ConfigDataUtil.getInteger(config, name);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptFloat(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Float> setter) {
		ConfigData<Float> supplier = ConfigDataUtil.getFloat(config, name);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptDouble(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Double> setter) {
		ConfigData<Double> supplier = ConfigDataUtil.getDouble(config, name);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptString(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, String> setter) {
		ConfigData<String> supplier = ConfigDataUtil.getString(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T, E extends Enum<E>> ConfigData<E> addOptEnum(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, Class<E> enumType, BiConsumer<T, E> setter) {
		ConfigData<E> supplier = ConfigDataUtil.getEnum(config, name, enumType, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));

		return supplier;
	}

	private <T> ConfigData<Material> addOptMaterial(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Material> setter) {
		ConfigData<Material> supplier = ConfigDataUtil.getMaterial(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));

		return supplier;
	}

	private <T> void addOptARGBColor(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Color> setter) {
		ConfigData<Color> supplier = ConfigDataUtil.getARGBColor(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptVector(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Vector> setter) {
		ConfigData<Vector> supplier = ConfigDataUtil.getVector(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> void addOptComponent(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Component> setter) {
		ConfigData<Component> supplier = ConfigDataUtil.getComponent(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));
	}

	private <T> ConfigData<BlockData> addOptBlockData(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, BlockData> setter) {
		ConfigData<BlockData> supplier = ConfigDataUtil.getBlockData(config, name, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));

		return supplier;
	}

	private <T> ConfigData<ItemStack> addOptItemStack(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, ItemStack> setter) {
		ConfigData<String> supplier = ConfigDataUtil.getString(config, name, null);
		if (supplier.isConstant()) {
			ItemStack item = getItemStack(supplier.get());
			if (item == null) return data -> null;

			transformers.put(type, new TransformerImpl<>(data -> item, setter, true));
			return data -> item;
		}

		ConfigData<ItemStack> itemSupplier = data -> getItemStack(supplier.get(data));
		transformers.put(type, new TransformerImpl<>(itemSupplier, setter, true));

		return itemSupplier;
	}

	private ItemStack getItemStack(String string) {
		if (string == null) return null;

		try {
			return Bukkit.getItemFactory().createItemStack(string);
		} catch (IllegalArgumentException ignored) {
		}

		MagicItem magicItem = MagicItems.getMagicItemFromString(string);
		return magicItem == null ? null : magicItem.getItemStack();
	}

	private void addOptEquipment(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, EquipmentSlot slot) {
		addOptEquipment(transformers, config, name, LivingEntity.class, slot);
	}

	private <T extends LivingEntity> void addOptEquipment(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, EquipmentSlot slot) {
		addOptItemStack(transformers, config, name, type, (entity, item) -> {
			EntityEquipment equipment = entity.getEquipment();
			if (equipment == null) return;

			equipment.setItem(slot, item);
		});
	}

	private <T> void addOptEquipmentDropChance(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, EquipmentSlot slot) {
		addOptFloat(transformers, config, name, Mob.class, (entity, chance) -> {
			EntityEquipment equipment = entity.getEquipment();
			equipment.setDropChance(slot, chance);
		});
	}

	private <T, R extends Keyed> void addOptRegistryEntry(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, RegistryKey<R> key, BiConsumer<T, R> setter) {
		addOptRegistryEntry(transformers, config, name, type, RegistryAccess.registryAccess().getRegistry(key), setter);
	}

	private <T, R extends Keyed> ConfigData<R> addOptRegistryEntry(Multimap<Class<?>, Transformer<?>> transformers, ConfigurationSection config, String name, Class<T> type, Registry<R> registry, BiConsumer<T, R> setter) {
		ConfigData<R> supplier = ConfigDataUtil.getRegistryEntry(config, name, registry, null);
		transformers.put(type, new TransformerImpl<>(supplier, setter, true));

		return supplier;
	}

	private <T> ConfigData<T> fallback(Function<String, ConfigData<T>> function, String... keys) {
		for (String key : keys) {
			ConfigData<T> data = function.apply(key);
			if (!data.isNull()) return data;
		}

		return data -> null;
	}

	public ConfigData<Vector3f> getVector(ConfigurationSection config, String path) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return data -> null;

			String[] vec = value.split(",");
			if (vec.length != 3) return data -> null;

			try {
				Vector3f vector = new Vector3f(Float.parseFloat(vec[0]), Float.parseFloat(vec[1]), Float.parseFloat(vec[2]));
				return data -> vector;
			} catch (NumberFormatException e) {
				return data -> null;
			}
		}

		ConfigData<Float> x;
		ConfigData<Float> y;
		ConfigData<Float> z;

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return data -> null;

			x = ConfigDataUtil.getFloat(section, "x");
			y = ConfigDataUtil.getFloat(section, "y");
			z = ConfigDataUtil.getFloat(section, "z");
		} else if (config.isList(path)) {
			List<?> value = config.getList(path);
			if (value == null || value.size() != 3) return data -> null;

			Object xObj = value.get(0);
			Object yObj = value.get(1);
			Object zObj = value.get(2);

			if (xObj instanceof Number number) {
				float val = number.floatValue();
				x = data -> val;
			} else if (xObj instanceof String string) {
				x = FunctionData.build(string, Double::floatValue);
			} else x = null;

			if (yObj instanceof Number number) {
				float val = number.floatValue();
				y = data -> val;
			} else if (yObj instanceof String string) {
				y = FunctionData.build(string, Double::floatValue);
			} else y = null;

			if (zObj instanceof Number number) {
				float val = number.floatValue();
				z = data -> val;
			} else if (zObj instanceof String string) {
				z = FunctionData.build(string, Double::floatValue);
			} else z = null;

			if (x == null || y == null || z == null) return data -> null;
		} else {
			return data -> null;
		}

		if (x.isNull() || y.isNull() || z.isNull()) return data -> null;

		if (x.isConstant() && y.isConstant() && z.isConstant()) {
			Vector3f vector = new Vector3f(x.get(), y.get(), z.get());
			return data -> vector;
		}

		return new ConfigData<>() {

			@Override
			public Vector3f get(@NotNull SpellData data) {
				Float vx = x.get(data);
				if (vx == null) return null;

				Float vy = y.get(data);
				if (vy == null) return null;

				Float vz = z.get(data);
				if (vz == null) return null;

				return new Vector3f(vx, vy, vz);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};

	}

	private ConfigData<Quaternionf> getQuaternion(ConfigurationSection config, String path) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return data -> null;

			String[] quat = value.split(",");
			if (quat.length != 4) return data -> null;

			try {
				Quaternionf rot = new Quaternionf(Float.parseFloat(quat[0]), Float.parseFloat(quat[1]), Float.parseFloat(quat[2]), Float.parseFloat(quat[3]));
				return data -> rot;
			} catch (NumberFormatException e) {
				return data -> null;
			}
		}

		ConfigData<Float> angle = ConfigDataUtil.getFloat(config, path + ".angle");
		ConfigData<Vector3f> axis = getVector(config, path + ".axis");
		if (!angle.isNull() && !axis.isNull()) {
			if (angle.isConstant() && axis.isConstant()) {
				Vector3f ax = axis.get();
				float ang = angle.get();

				Quaternionf rot = new Quaternionf();
				rot.rotationAxis(ang, ax.x, ax.y, ax.z);

				return data -> rot;
			}

			return new ConfigData<>() {

				@Override
				public Quaternionf get(@NotNull SpellData data) {
					Float ang = angle.get(data);
					if (ang == null) return null;

					Vector3f ax = axis.get(data);
					if (ax == null) return null;

					return new Quaternionf().rotationAxis(ang, ax.x, ax.y, ax.z);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		ConfigData<Float> x;
		ConfigData<Float> y;
		ConfigData<Float> z;
		ConfigData<Float> w;

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return data -> null;

			x = ConfigDataUtil.getFloat(section, "x");
			y = ConfigDataUtil.getFloat(section, "y");
			z = ConfigDataUtil.getFloat(section, "z");
			w = ConfigDataUtil.getFloat(section, "w");
		} else if (config.isList(path)) {
			List<?> value = config.getList(path);
			if (value == null || value.size() != 4) return data -> null;

			Object xObj = value.get(0);
			Object yObj = value.get(1);
			Object zObj = value.get(2);
			Object wObj = value.get(3);

			if (xObj instanceof Number number) {
				float val = number.floatValue();
				x = data -> val;
			} else if (xObj instanceof String string) {
				x = FunctionData.build(string, Double::floatValue);
			} else x = null;

			if (yObj instanceof Number number) {
				float val = number.floatValue();
				y = data -> val;
			} else if (yObj instanceof String string) {
				y = FunctionData.build(string, Double::floatValue);
			} else y = null;

			if (zObj instanceof Number number) {
				float val = number.floatValue();
				z = data -> val;
			} else if (zObj instanceof String string) {
				z = FunctionData.build(string, Double::floatValue);
			} else z = null;

			if (wObj instanceof Number number) {
				float val = number.floatValue();
				w = data -> val;
			} else if (zObj instanceof String string) {
				w = FunctionData.build(string, Double::floatValue);
			} else w = null;

			if (x == null || y == null || z == null || w == null) return data -> null;
		} else {
			return data -> null;
		}

		if (x.isNull() || y.isNull() || z.isNull() || w.isNull()) return data -> null;

		if (x.isConstant() && y.isConstant() && z.isConstant() && w.isConstant()) {
			Quaternionf rot = new Quaternionf(x.get(), y.get(), z.get(), w.get());
			return data -> rot;
		}

		return new ConfigData<>() {

			@Override
			public Quaternionf get(@NotNull SpellData data) {
				Float qx = x.get(data);
				if (qx == null) return null;

				Float qy = y.get(data);
				if (qy == null) return null;

				Float qz = z.get(data);
				if (qz == null) return null;

				Float qw = w.get(data);
				if (qw == null) return null;

				return new Quaternionf(qx, qy, qz, qw);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};

	}

	public ConfigData<EntityType> getEntityType() {
		return entityType;
	}

	public ConfigData<Vector> getRelativeOffset() {
		return relativeOffset;
	}

	/**
	 * @deprecated Use {@link EntityData#spawn(Location, SpellData, Class, Consumer)}
	 */
	@Deprecated(forRemoval = true)
	public void setEntityType(ConfigData<EntityType> entityType) {
		this.entityType = entityType;
	}

	@ApiStatus.Internal
	public ConfigData<ItemStack> getDroppedItemStack() {
		return dropItem;
	}

	@ApiStatus.Internal
	public ConfigData<BlockData> getFallingBlockData() {
		return fallingBlockData;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getBaby() {
		return baby;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getChested() {
		return chested;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getPowered() {
		return powered;
	}

	@ApiStatus.Internal
	public ConfigData<BlockData> getCarriedBlockData() {
		return carriedBlockData;
	}

	@ApiStatus.Internal
	public ConfigData<Horse.Color> getHorseColor() {
		return horseColor;
	}

	@ApiStatus.Internal
	public ConfigData<Horse.Style> getHorseStyle() {
		return horseStyle;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getSaddled() {
		return saddled;
	}

	@ApiStatus.Internal
	public ConfigData<Llama.Color> getLlamaColor() {
		return llamaColor;
	}

	@ApiStatus.Internal
	public ConfigData<Parrot.Variant> getParrotVariant() {
		return parrotVariant;
	}

	@ApiStatus.Internal
	public ConfigData<Integer> getSize() {
		return size;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getSheared() {
		return sheared;
	}

	@ApiStatus.Internal
	public ConfigData<DyeColor> getColor() {
		return color;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getTamed() {
		return tamed;
	}

	@ApiStatus.Internal
	public ConfigData<TropicalFish.Pattern> getTropicalFishPattern() {
		return tropicalFishPattern;
	}

	@ApiStatus.Internal
	public ConfigData<DyeColor> getTropicalFishPatternColor() {
		return tropicalFishPatternColor;
	}

	@ApiStatus.Internal
	public ConfigData<Villager.Profession> getProfession() {
		return profession;
	}

	private record DelayedEntityData(EntityData data, ConfigData<Long> delay, ConfigData<Long> interval, ConfigData<Long> iterations) {

	}

	private interface Transformer<T> {

		void apply(T entity, SpellData data);

	}

	private record TransformerImpl<T, C>(ConfigData<C> supplier, BiConsumer<T, C> setter, boolean optional) implements Transformer<T> {

		public TransformerImpl(ConfigData<C> supplier, BiConsumer<T, C> setter) {
			this(supplier, setter, false);
		}

		public void apply(T entity, SpellData data) {
			C value = supplier.get(data);
			if (!optional || value != null) setter.accept(entity, value);
		}

	}

}
