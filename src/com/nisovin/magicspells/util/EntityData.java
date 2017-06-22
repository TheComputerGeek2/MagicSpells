package com.nisovin.magicspells.util;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Villager.Profession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class EntityData {
    
    private EntityType entityType;
    private boolean flag;
    private int var1;
    private int var2;
    private int var3;
    
    public EntityData(String type) {
        if(type.startsWith("baby ")) {
            flag = true;
            type = type.replace("baby ", "");
        }
        if(type.equalsIgnoreCase("human") || type.equalsIgnoreCase("player")) {
            type = "player";
        } else if(type.equalsIgnoreCase("wither skeleton")) {
            type = "skeleton";
            flag = true;
        } else if(type.equalsIgnoreCase("zombie villager") || type.equalsIgnoreCase("villager zombie")) {
            type = "zombie";
            var1 = 1;
        } else if(type.equalsIgnoreCase("powered creeper")) {
            type = "creeper";
            flag = true;
        } else if(type.toLowerCase().startsWith("villager ")) {
            final String prof = type.toLowerCase().replace("villager ", "");
            if(prof.matches("^[0-5]$")) {
                var1 = Integer.parseInt(prof);
            } else if(prof.toLowerCase().startsWith("green")) {
                var1 = 5;
            } else {
                try {
                    var1 = getProfessionId(Profession.valueOf(prof.toUpperCase()));
                } catch(final Exception e) {
                    MagicSpells.error("Invalid villager profession: " + prof);
                }
            }
            type = "villager";
        } else if(type.toLowerCase().endsWith(" villager")) {
            final String prof = type.toLowerCase().replace(" villager", "");
            if(prof.toLowerCase().startsWith("green")) {
                var1 = 5;
            } else {
                try {
                    var1 = getProfessionId(Profession.valueOf(prof.toUpperCase()));
                } catch(final Exception e) {
                    MagicSpells.error("Invalid villager profession: " + prof);
                }
            }
            type = "villager";
        } else if(type.toLowerCase().endsWith(" sheep")) {
            final String color = type.toLowerCase().replace(" sheep", "");
            if(color.equalsIgnoreCase("random")) {
                var1 = -1;
            } else {
                try {
                    final DyeColor dyeColor = DyeColor.valueOf(color.toUpperCase().replace(" ", "_"));
                    var1 = dyeColor.getWoolData();
                } catch(final IllegalArgumentException e) {
                    MagicSpells.error("Invalid sheep color: " + color);
                }
            }
            type = "sheep";
        } else if(type.toLowerCase().endsWith(" rabbit")) {
            final String rabbitType = type.toLowerCase().replace(" rabbit", "");
            var1 = 0;
            switch(rabbitType) {
                case "white":
                    var1 = 1;
                    break;
                case "black":
                    var1 = 2;
                    break;
                case "blackwhite":
                    var1 = 3;
                    break;
                case "gold":
                    var1 = 4;
                    break;
                case "saltpepper":
                    var1 = 5;
                    break;
                case "killer":
                    var1 = 99;
                    break;
            }
            type = "rabbit";
        } else if(type.toLowerCase().startsWith("wolf ")) {
            final String color = type.toLowerCase().replace("wolf ", "");
            if(color.equals("angry")) {
                var1 = -1;
            } else if(color.matches("[0-9a-fA-F]+")) {
                var1 = Integer.parseInt(color, 16);
            }
            type = "wolf";
        } else if(type.toLowerCase().equalsIgnoreCase("saddled pig")) {
            var1 = 1;
            type = "pig";
        } else if(type.equalsIgnoreCase("irongolem")) {
            type = "villagergolem";
        } else if(type.equalsIgnoreCase("mooshroom")) {
            type = "mushroomcow";
        } else if(type.equalsIgnoreCase("magmacube")) {
            type = "lavaslime";
        }/* else if(type.toLowerCase().contains("ocelot")) {
            //System.out.println("### Replacing 'ocelot' with 'ozelot'");
            type = type.toLowerCase().replace("ocelot", "ozelot");
        }*/ else if(type.equalsIgnoreCase("snowgolem")) {
            type = "snowman";
        } else if(type.equalsIgnoreCase("wither")) {
            type = "witherboss";
        } else if(type.equalsIgnoreCase("dragon")) {
            type = "enderdragon";
        } else if(type.toLowerCase().startsWith("block") || type.toLowerCase().startsWith("fallingblock")) {
            final String data = type.split(" ")[1];
            if(data.contains(":")) {
                final String[] subdata = data.split(":");
                var1 = Integer.parseInt(subdata[0]);
                var2 = Integer.parseInt(subdata[1]);
            } else {
                var1 = Integer.parseInt(data);
            }
            type = "fallingsand";
        } else if(type.toLowerCase().startsWith("item")) {
            final String data = type.split(" ")[1];
            if(data.contains(":")) {
                final String[] subdata = data.split(":");
                var1 = Integer.parseInt(subdata[0]);
                var2 = Integer.parseInt(subdata[1]);
            } else {
                var1 = Integer.parseInt(data);
            }
            type = "item";
        } else if(type.toLowerCase().contains("horse")) {
            final List<String> data = new ArrayList<>(Arrays.asList(type.split(" ")));
            var1 = 0;
            var2 = 0;
            if(data.get(0).equalsIgnoreCase("horse")) {
                data.remove(0);
            } else if(data.size() >= 2 && data.get(1).equalsIgnoreCase("horse")) {
                final String t = data.remove(0).toLowerCase();
                switch(t) {
                    case "donkey":
                        var1 = 1;
                        break;
                    case "mule":
                        var1 = 2;
                        break;
                    case "skeleton":
                    case "skeletal":
                        var1 = 4;
                        break;
                    case "zombie":
                    case "undead":
                        var1 = 3;
                        break;
                    default:
                        var1 = 0;
                        break;
                }
                data.remove(0);
            }
            while(!data.isEmpty()) {
                final String d = data.remove(0);
                if(d.matches("^[0-9]+$")) {
                    var2 = Integer.parseInt(d);
                } else if(d.equalsIgnoreCase("iron")) {
                    var3 = 1;
                } else if(d.equalsIgnoreCase("gold")) {
                    var3 = 2;
                } else if(d.equalsIgnoreCase("diamond")) {
                    var3 = 3;
                }
            }
            type = "entityhorse";
        } else if(type.equalsIgnoreCase("mule")) {
            var1 = 2;
            type = "entityhorse";
        } else if(type.equalsIgnoreCase("donkey")) {
            var1 = 1;
            type = "entityhorse";
        } else if(type.equalsIgnoreCase("elder guardian")) {
            flag = true;
            type = "guardian";
        }
        if(type.toLowerCase().matches("ozelot [0-3]")) {
            System.out.println("### ozelot 0-3");
            var1 = Integer.parseInt(type.split(" ")[1]);
            type = "ozelot";
        //} else if(type.toLowerCase().equals("ozelot random") || type.toLowerCase().equals("random ozelot")) {
        } else if(type.toLowerCase().equals("ocelot random") || type.toLowerCase().equals("random ocelot")) {
            //System.out.println("### random ozelot");
            var1 = -1;
            type = "ozelot";
        }
        if(type.equals("slime") || type.equals("lavaslime")) {
            var1 = 1;
        } else if(type.startsWith("slime") || type.startsWith("magmacube") || type.startsWith("lavaslime")) {
            final String[] data = type.split(" ");
            type = data[0];
            if(type.equals("magmacube")) {
                type = "lavaslime";
            }
            var1 = Integer.parseInt(data[1]);
        }
        if(type.equals("player")) {
            entityType = EntityType.PLAYER;
        } else {
            for(final EntityType e : EntityType.values()) {
                if(e != null && e.getName() != null) {
                    //System.out.println("### Comparing: " + e.getName() + " : " + type);
                    if(e.getName().toLowerCase().equalsIgnoreCase(type)) {
                        //System.out.println("### Choosing: " + type);
                        entityType = e;
                        //System.out.println("### Chose: " + entityType);
                    }
                }
            }
            if(entityType == null) {
                //System.out.println("### No such entityType: " + type);
            }
        }
    }
    
    private static int getProfessionId(final Profession prof) {
        switch(prof) {
            case FARMER:
                return 0;
            case LIBRARIAN:
                return 1;
            case PRIEST:
                return 2;
            case BLACKSMITH:
                return 3;
            case BUTCHER:
                return 4;
            default:
                return 0;
        }
    }
    
    public EntityType getType() {
        return entityType;
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
    
    @SuppressWarnings("ConstantConditions")
    public Entity spawn(final Location loc) {
        final Entity entity = loc.getWorld().spawnEntity(loc, entityType);
        if(entity instanceof Ageable && flag) {
            ((Ageable) entity).setBaby();
        }
        if(entityType == EntityType.ZOMBIE) {
            ((Zombie) entity).setBaby(flag);
            ((Zombie) entity).setVillager(var1 == 1);
        } else if(entityType == EntityType.SKELETON) {
            if(flag) {
                ((Skeleton) entity).setSkeletonType(SkeletonType.WITHER);
            }
        } else if(entityType == EntityType.CREEPER) {
            if(flag) {
                ((Creeper) entity).setPowered(true);
            }
        } else if(entityType == EntityType.WOLF) {
            if(var1 == -1) {
                ((Wolf) entity).setAngry(true);
            }
        } else if(entityType == EntityType.OCELOT) {
            if(var1 == 0) {
                ((Ocelot) entity).setCatType(Type.WILD_OCELOT);
            } else if(var1 == 1) {
                ((Ocelot) entity).setCatType(Type.BLACK_CAT);
            } else if(var1 == 2) {
                ((Ocelot) entity).setCatType(Type.RED_CAT);
            } else if(var1 == 3) {
                ((Ocelot) entity).setCatType(Type.SIAMESE_CAT);
            }
        } else if(entityType == EntityType.VILLAGER) {
            if(var1 == 0) {
                ((Villager) entity).setProfession(Profession.FARMER);
            } else if(var1 == 1) {
                ((Villager) entity).setProfession(Profession.LIBRARIAN);
            } else if(var1 == 2) {
                ((Villager) entity).setProfession(Profession.PRIEST);
            } else if(var1 == 3) {
                ((Villager) entity).setProfession(Profession.BLACKSMITH);
            } else if(var1 == 4) {
                ((Villager) entity).setProfession(Profession.BUTCHER);
            }
        } else if(entityType == EntityType.SLIME) {
            ((Slime) entity).setSize(var1);
        } else if(entityType == EntityType.MAGMA_CUBE) {
            ((MagmaCube) entity).setSize(var1);
        } else if(entityType == EntityType.PIG) {
            if(var1 == 1) {
                ((Pig) entity).setSaddle(true);
            }
        } else if(entityType == EntityType.SHEEP) {
            final DyeColor c = DyeColor.getByWoolData((byte) var1);
            if(c != null) {
                ((Sheep) entity).setColor(c);
            }
        } else //noinspection StatementWithEmptyBody
            if(entityType == EntityType.RABBIT) {
            /*if (var1 == 0) {
                ((Rabbit)entity).setRabbitType(Rabbit.Type.BROWN);
			} else if (var1 == 1) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.WHITE);
			} else if (var1 == 2) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.BLACK);
			} else if (var1 == 3) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.BLACK_AND_WHITE);
			} else if (var1 == 4) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.GOLD);
			} else if (var1 == 5) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.SALT_AND_PEPPER);
			} else if (var1 == 99) {
				((Rabbit)entity).setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
			}*/
            } else if(entityType == EntityType.GUARDIAN) {
                if(flag) {
                    ((Guardian) entity).setElder(true);
                }
            } else if(entityType == EntityType.HORSE) {
                if(var1 == 0) {
                    ((Horse) entity).setVariant(Variant.HORSE);
                } else if(var1 == 1) {
                    ((Horse) entity).setVariant(Variant.DONKEY);
                } else if(var1 == 2) {
                    ((Horse) entity).setVariant(Variant.MULE);
                } else if(var1 == 3) {
                    ((Horse) entity).setVariant(Variant.UNDEAD_HORSE);
                } else if(var1 == 4) {
                    ((Horse) entity).setVariant(Variant.SKELETON_HORSE);
                }
            }
        return entity;
    }
}
