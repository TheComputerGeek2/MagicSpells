package com.nisovin.magicjutsus.util;

// this should probably be kept as a star import for version safety
import org.bukkit.entity.*;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;

public class EntityData {

	private EntityType entityType;

	private Material material;
	private DyeColor dyeColor;
	private DyeColor patternDyeColor;

	private Horse.Color horseColor;
	private Horse.Style horseStyle;
	private Rabbit.Type rabbitType;
	private Llama.Color llamaColor;
	private Parrot.Variant parrotVariant;
	private Villager.Profession profession;
	private TropicalFish.Pattern fishPattern;

	private EulerAngle headAngle;
	private EulerAngle bodyAngle;
	private EulerAngle leftArmAngle;
	private EulerAngle rightArmAngle;
	private EulerAngle leftLegAngle;
	private EulerAngle rightLegAngle;

	private boolean baby;
	private boolean tamed;
	private boolean angry;
	private boolean small;
	private boolean marker;
	private boolean visible;
	private boolean hasArms;
	private boolean powered;
	private boolean chested;
	private boolean saddled;
	private boolean sheared;
	private boolean hasBasePlate;

	private int size;

	private boolean isMob = false;
	private boolean isMisc = false;
	private boolean isPlayer = false;

	public EntityData(ConfigurationSection section) {
		if (section == null) throw new NullPointerException("section");

		size = section.getInt("size", 0);

		baby = section.getBoolean("baby", false);
		tamed = section.getBoolean("tamed", false);
		angry = section.getBoolean("angry", false);
		small = section.getBoolean("small", false);
		marker = section.getBoolean("marker", false);
		visible = section.getBoolean("visible", true);
		hasArms = section.getBoolean("has-arms", true);
		chested = section.getBoolean("chested", false);
		powered = section.getBoolean("powered", false);
		saddled = section.getBoolean("saddled", false);
		sheared = section.getBoolean("sheared", false);
		hasBasePlate = section.getBoolean("has-base-plate", true);

		Vector head = Util.getVector(section.getString("head-angle", "0,0,0"));
		Vector body = Util.getVector(section.getString("body-angle", "0,0,0"));
		Vector leftArm = Util.getVector(section.getString("left-arm-angle", "0,0,0"));
		Vector rightArm = Util.getVector(section.getString("right-arm-angle", "0,0,0"));
		Vector leftLeg = Util.getVector(section.getString("left-leg-angle", "0,0,0"));
		Vector rightLeg = Util.getVector(section.getString("right-leg-angle", "0,0,0"));

		headAngle = new EulerAngle(head.getX(), head.getY(), head.getZ());
		bodyAngle = new EulerAngle(body.getX(), body.getY(), body.getZ());
		leftArmAngle = new EulerAngle(leftArm.getX(), leftArm.getY(), leftArm.getZ());
		rightArmAngle = new EulerAngle(rightArm.getX(), rightArm.getY(), rightArm.getZ());
		leftLegAngle = new EulerAngle(leftLeg.getX(), leftLeg.getY(), leftLeg.getZ());
		rightLegAngle = new EulerAngle(rightLeg.getX(), rightLeg.getY(), rightLeg.getZ());

		String mat = section.getString("material", "");
		String type = section.getString("type", "");
		String color = section.getString("color", "");
		String patternColor = section.getString("pattern-color", "");
		String style = section.getString("style", "");
		String entity = section.getString("entity", "");
		switch (entity.toLowerCase()) {
			case "player":
				entityType = EntityType.PLAYER;
				isPlayer = true;
				break;
			case "wither_skeleton":
				entityType = EntityType.WITHER_SKELETON;
				isMob = true;
				break;
			case "zombie_villager":
				entityType = EntityType.ZOMBIE_VILLAGER;
				isMob = true;
				break;
			case "creeper":
				entityType = EntityType.CREEPER;
				isMob = true;
				break;
			case "villager":
				entityType = EntityType.VILLAGER;
				isMob = true;
				try {
					profession = Villager.Profession.valueOf(type.toUpperCase());
				} catch (Exception e) {
					MagicJutsus.error("Invalid villager profession: " + type);
					profession = null;
				}
				break;
			case "sheep":
				entityType = EntityType.SHEEP;
				isMob = true;
				try {
					dyeColor = DyeColor.valueOf(color.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid sheep color: " + color);
					dyeColor = null;
				}
				break;
			case "rabbit":
				entityType = EntityType.RABBIT;
				isMob = true;
				try {
					rabbitType = Rabbit.Type.valueOf(type.toUpperCase());
				} catch (Exception e) {
					MagicJutsus.error("Invalid rabbit type: " + type);
					rabbitType = null;
				}
				break;
			case "wolf":
				entityType = EntityType.WOLF;
				isMob = true;
				try {
					dyeColor = DyeColor.valueOf(color.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid wolf collar color: " + color);
					dyeColor = null;
				}
				break;
			case "pig":
				entityType = EntityType.PIG;
				isMob = true;
				break;
			case "iron_golem":
				entityType = EntityType.IRON_GOLEM;
				isMob = true;
				break;
			case "mooshrom":
				entityType = EntityType.MUSHROOM_COW;
				isMob = true;
				break;
			case "magma_cube":
				entityType = EntityType.MAGMA_CUBE;
				isMob = true;
				break;
			case "ocelot":
				entityType = EntityType.OCELOT;
				isMob = true;
				break;
			case "snow_golem":
				entityType = EntityType.SNOWMAN;
				isMob = true;
				break;
			case "wither":
				entityType = EntityType.WITHER;
				isMob = true;
				break;
			case "ender_dragon":
				entityType = EntityType.ENDER_DRAGON;
				isMob = true;
				break;
			case "horse":
				entityType = EntityType.HORSE;
				isMob = true;
				try {
					horseColor = Horse.Color.valueOf(color.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid horse color: " + color);
					horseColor = null;
				}

				try {
					horseStyle = Horse.Style.valueOf(style.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid horse style: " + style);
					horseStyle = null;
				}
				break;
			case "skeleton_horse":
				entityType = EntityType.SKELETON_HORSE;
				isMob = true;
				break;
			case "zombie_horse":
				entityType = EntityType.ZOMBIE_HORSE;
				isMob = true;
				break;
			case "mule":
				entityType = EntityType.MULE;
				isMob = true;
				break;
			case "donkey":
				entityType = EntityType.DONKEY;
				isMob = true;
				break;
			case "elder_guardian":
				entityType = EntityType.ELDER_GUARDIAN;
				isMob = true;
				break;
			case "slime":
				entityType = EntityType.SLIME;
				isMob = true;
				break;
			case "cod":
				entityType = EntityType.COD;
				isMob = true;
				break;
			case "salmon":
				entityType = EntityType.SALMON;
				isMob = true;
				break;
			case "pufferfish":
				entityType = EntityType.PUFFERFISH;
				isMob = true;
				break;
			case "tropical_fish":
				entityType = EntityType.TROPICAL_FISH;
				isMob = true;
				try {
					fishPattern = TropicalFish.Pattern.valueOf(type.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid fish pattern: " + type);
					fishPattern = null;
				}

				try {
					dyeColor = DyeColor.valueOf(color.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid fish body color: " + color);
					dyeColor = null;
				}

				try {
					patternDyeColor = DyeColor.valueOf(patternColor.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid fish pattern color: " + patternColor);
					patternDyeColor = null;
				}
				break;
			case "llama":
				entityType = EntityType.LLAMA;
				isMob = true;
				try {
					llamaColor = Llama.Color.valueOf(color.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid llama color: " + color);
					llamaColor = null;
				}
				break;
			case "polar_bear":
				entityType = EntityType.POLAR_BEAR;
				isMob = true;
				break;
			case "armor_stand":
				entityType = EntityType.ARMOR_STAND;
				isMisc = true;
				break;
			case "bat":
				entityType = EntityType.BAT;
				isMob = true;
				break;
			case "blaze":
				entityType = EntityType.BLAZE;
				isMob = true;
				break;
			case "dolphin":
				entityType = EntityType.DOLPHIN;
				isMob = true;
				break;
			case "enderman":
				entityType = EntityType.ENDERMAN;
				isMob = true;
				material = Material.getMaterial(mat.toUpperCase());
				if (material == null) {
					MagicJutsus.error("Invalid enderman material: " + mat);
					material = null;
				}
				break;
			case "ghast":
				entityType = EntityType.GHAST;
				isMob = true;
				break;
			case "guardian":
				entityType = EntityType.GUARDIAN;
				isMob = true;
				break;
			case "illusioner":
				entityType = EntityType.ILLUSIONER;
				isMob = true;
				break;
			case "vindicator":
				entityType = EntityType.VINDICATOR;
				isMob = true;
				break;
			case "evoker":
				entityType = EntityType.EVOKER;
				isMob = true;
				break;
			case "skeleton":
				entityType = EntityType.SKELETON;
				isMob = true;
				break;
			case "spider":
				entityType = EntityType.SPIDER;
				isMob = true;
				break;
			case "cave_spider":
				entityType = EntityType.CAVE_SPIDER;
				isMob = true;
				break;
			case "giant":
				entityType = EntityType.GIANT;
				isMob = true;
				break;
			case "zombie":
				entityType = EntityType.ZOMBIE;
				isMob = true;
				break;
			case "zombie_pigman":
				entityType = EntityType.PIG_ZOMBIE;
				isMob = true;
				break;
			case "silverfish":
				entityType = EntityType.SILVERFISH;
				isMob = true;
				break;
			case "witch":
				entityType = EntityType.WITCH;
				isMob = true;
				break;
			case "endermite":
				entityType = EntityType.ENDERMITE;
				isMob = true;
				break;
			case "shulker":
				entityType = EntityType.SHULKER;
				isMob = true;
				break;
			case "cow":
				entityType = EntityType.COW;
				isMob = true;
				break;
			case "chicken":
				entityType = EntityType.CHICKEN;
				isMob = true;
				break;
			case "squid":
				entityType = EntityType.SQUID;
				isMob = true;
				break;
			case "parrot":
				entityType = EntityType.PARROT;
				isMob = true;
				try {
					parrotVariant = Parrot.Variant.valueOf(type.toUpperCase());
				} catch (Exception exception) {
					MagicJutsus.error("Invalid parrot variant: " + type);
					parrotVariant = null;
				}
				break;
			case "turtle":
				entityType = EntityType.TURTLE;
				isMob = true;
				break;
			case "phantom":
				entityType = EntityType.PHANTOM;
				isMob = true;
				break;
			case "drowned":
				entityType = EntityType.DROWNED;
				isMob = true;
				break;
			case "falling_block":
				entityType = EntityType.FALLING_BLOCK;
				isMisc = true;
				material = Material.getMaterial(mat.toUpperCase());
				if (material == null || !material.isBlock()) {
					MagicJutsus.error("Invalid falling block material: " + mat);
					material = null;
				}
				break;
			case "item":
				entityType = EntityType.DROPPED_ITEM;
				isMisc = true;
				material = Material.getMaterial(mat.toUpperCase());
				if (material == null) MagicJutsus.error("Invalid item material: " + mat);
				break;
		}
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public Material getMaterial() {
		return material;
	}

	public boolean isPlayer() {
		return isPlayer;
	}

	public boolean isMob() {
		return isMob;
	}

	public boolean isMisc() {
		return isMisc;
	}

	public boolean isBaby() {
		return baby;
	}

	public boolean isTamed() {
		return tamed;
	}

	public boolean isAngry() {
		return angry;
	}

	public boolean isPowered() {
		return powered;
	}

	public boolean isChested() {
		return chested;
	}

	public boolean isSaddled() {
		return saddled;
	}

	public boolean isSheared() {
		return sheared;
	}

	public int getSize() {
		return size;
	}

	public Villager.Profession getProfession() {
		return profession;
	}

	public Horse.Color getHorseColor() {
		return horseColor;
	}

	public Horse.Style getHorseStyle() {
		return horseStyle;
	}

	public DyeColor getDyeColor() {
		return dyeColor;
	}

	public DyeColor getPatternDyeColor() {
		return patternDyeColor;
	}

	public TropicalFish.Pattern getFishPattern() {
		return fishPattern;
	}

	public Llama.Color getLlamaColor() {
		return llamaColor;
	}

	public Parrot.Variant getParrotVariant() {
		return parrotVariant;
	}

	public Rabbit.Type getRabbitType() {
		return rabbitType;
	}


	public Entity spawn(Location location) {
		if (location == null) throw new NullPointerException("location");
		
		Entity entity = location.getWorld().spawnEntity(location, entityType);

		if (entity instanceof Ageable) {
			if (isBaby()) ((Ageable) entity).setBaby();
			else ((Ageable) entity).setAdult();
		}

		if (entity instanceof AbstractHorse && isSaddled()) ((AbstractHorse) entity).getInventory().setSaddle(new ItemStack(Material.SADDLE));

		if (entity instanceof ChestedHorse) ((ChestedHorse) entity).setCarryingChest(isChested());

		if (entity instanceof Slime) ((Slime) entity).setSize(getSize());

		if (entity instanceof Phantom) ((Phantom) entity).setSize(getSize());

		switch (entityType) {
			case ARMOR_STAND:
				((ArmorStand) entity).setSmall(small);
				((ArmorStand) entity).setArms(hasArms);
				((ArmorStand) entity).setMarker(marker);
				((ArmorStand) entity).setVisible(visible);
				((ArmorStand) entity).setBasePlate(hasBasePlate);
				((ArmorStand) entity).setHeadPose(headAngle);
				((ArmorStand) entity).setBodyPose(bodyAngle);
				((ArmorStand) entity).setLeftArmPose(leftArmAngle);
				((ArmorStand) entity).setRightArmPose(rightArmAngle);
				((ArmorStand) entity).setLeftLegPose(leftLegAngle);
				((ArmorStand) entity).setRightLegPose(rightLegAngle);
				break;
			case ZOMBIE:
				((Zombie) entity).setBaby(isBaby());
				break;
			case CREEPER:
				((Creeper) entity).setPowered(isPowered());
				break;
			case ENDERMAN:
				((Enderman) entity).setCarriedBlock(material.createBlockData());
				break;
			case WOLF:
				((Wolf) entity).setAngry(isAngry());
				((Wolf) entity).setTamed(isTamed());
				if (isTamed()) ((Wolf) entity).setCollarColor(getDyeColor());
				break;
			case VILLAGER:
				((Villager) entity).setProfession(getProfession());
				break;
			case PIG:
				((Pig) entity).setSaddle(isSaddled());
				break;
			case SHEEP:
				((Sheep) entity).setSheared(isSheared());
				((Sheep) entity).setColor(getDyeColor());
				break;
			case RABBIT:
				((Rabbit) entity).setRabbitType(getRabbitType());
				break;
			case HORSE:
				((Horse) entity).setColor(getHorseColor());
				((Horse) entity).setStyle(getHorseStyle());
				break;
			case LLAMA:
				((Llama) entity).setColor(getLlamaColor());
				((Llama) entity).getInventory().setDecor(new ItemStack(getMaterial()));
				break;
			case PUFFERFISH:
				((PufferFish) entity).setPuffState(getSize());
				break;
			case TROPICAL_FISH:
				((TropicalFish) entity).setBodyColor(getDyeColor());
				((TropicalFish) entity).setPatternColor(getPatternDyeColor());
				((TropicalFish) entity).setPattern(getFishPattern());
				break;
			case PARROT:
				((Parrot) entity).setVariant(getParrotVariant());
				break;
		}

		return entity;
	}

}
