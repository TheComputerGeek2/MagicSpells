package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.TreeSet;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class DowseSpell extends InstantSpell {

	private Material material;

	private EntityType entityType;

	private String playerName;
	private String strNotFound;

	private ConfigData<Integer> radius;

	private boolean setCompass;
	private boolean getDistance;
	private boolean rotatePlayer;
	private boolean powerAffectsRadius;

	public DowseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String blockName = getConfigString("block-type", "");
		String entityName = getConfigString("entity-type", "");

		if (!blockName.isEmpty()) material = Util.getMaterial(blockName);
		if (!entityName.isEmpty()) {
			if (entityName.equalsIgnoreCase("player")) {
				entityType = EntityType.PLAYER;
			} else if (entityName.toLowerCase().startsWith("player:")) {
				entityType = EntityType.PLAYER;
				playerName = entityName.split(":")[1];
			} else {
				entityType = MobUtil.getEntityType(entityName);
			}
		}

		strNotFound = getConfigString("str-not-found", "No dowsing target found.");

		radius = getConfigDataInt("radius", 4);

		setCompass = getConfigBoolean("set-compass", true);
		rotatePlayer = getConfigBoolean("rotate-player", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);

		getDistance = strCastSelf != null && strCastSelf.contains("%d");

		if (material == null && entityType == null) MagicSpells.error("DowseSpell '" + internalName + "' has no dowse target (block or entity) defined!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			double radius = this.radius.get(caster, null, power, args);
			if (powerAffectsRadius) radius *= power;

			radius = Math.min(radius, MagicSpells.getGlobalRadius());

			int distance = -1;
			if (material != null) {
				Block foundBlock = null;
				
				Location loc = player.getLocation();
				World world = player.getWorld();
				int cx = loc.getBlockX();
				int cy = loc.getBlockY();
				int cz = loc.getBlockZ();
				
				// Label to exit the search
				search:
				for (int r = 1; r <= radius; r++) {
					for (int x = -r; x <= r; x++) {
						for (int y = -r; y <= r; y++) {
							for (int z = -r; z <= r; z++) {
								if (x == r || y == r || z == r || -x == r || -y == r || -z == r) {
									Block block = world.getBlockAt(cx + x, cy + y, cz + z);
									if (material.equals(block.getType())) {
										foundBlock = block;
										break search;
									}
								}
							}
						}
					}
				}
							
				if (foundBlock == null) {
					sendMessage(strNotFound, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} 
				
				if (rotatePlayer) {
					Vector v = foundBlock.getLocation().add(0.5, 0.5, 0.5).subtract(player.getEyeLocation()).toVector().normalize();
					Util.setFacing(player, v);
				}
				if (setCompass) player.setCompassTarget(foundBlock.getLocation());
				if (getDistance) distance = (int) Math.round(player.getLocation().distance(foundBlock.getLocation()));
			} else if (entityType != null) {
				// Find entity
				Entity foundEntity = null;
				double distanceSq = radius * radius;
				if (entityType == EntityType.PLAYER && playerName != null) {
					// Find specific player
					foundEntity = PlayerNameUtils.getPlayerExact(playerName);
					if (foundEntity != null) {
						if (!foundEntity.getWorld().equals(player.getWorld())) foundEntity = null;
						else if (radius > 0 && player.getLocation().distanceSquared(foundEntity.getLocation()) > distanceSq) foundEntity = null;
					}
				} else {
					// Find nearest entity
					List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
					Location playerLoc = player.getLocation();
					TreeSet<NearbyEntity> ordered = new TreeSet<>();
					for (Entity e : nearby) {
						if (e.getType() == entityType) {
							double d = e.getLocation().distanceSquared(playerLoc);
							if (d < distanceSq) ordered.add(new NearbyEntity(e, d));
						}
					}
					if (!ordered.isEmpty()) {
						for (NearbyEntity ne : ordered) {
							if (ne.entity instanceof LivingEntity) {
								SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity) ne.entity, power, args);
								EventUtil.call(event);
								if (!event.isCancelled()) {
									foundEntity = ne.entity;
									break;
								}
							} else {
								foundEntity = ne.entity;
								break;
							}
						}
					}
				}

				if (foundEntity == null) {
					sendMessage(strNotFound, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}

				if (rotatePlayer) {
					Location l = foundEntity instanceof LivingEntity ? ((LivingEntity) foundEntity).getEyeLocation() : foundEntity.getLocation();
					Vector v = l.subtract(player.getEyeLocation()).toVector().normalize();
					Util.setFacing(player, v);
				}
				if (setCompass) player.setCompassTarget(foundEntity.getLocation());
				if (getDistance) distance = (int) Math.round(player.getLocation().distance(foundEntity.getLocation()));
			}
			
			playSpellEffects(EffectPosition.CASTER, player);
			if (getDistance) {
				sendMessage(strCastSelf, player, args, "%d", distance + "");
				sendMessageNear(player, strCastOthers);
				return PostCastAction.NO_MESSAGES;
			}
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getStrNotFound() {
		return strNotFound;
	}

	public void setStrNotFound(String strNotFound) {
		this.strNotFound = strNotFound;
	}

	public boolean shouldUpdateCompass() {
		return setCompass;
	}

	public void setUpdateCompass(boolean setCompass) {
		this.setCompass = setCompass;
	}

	public boolean shouldGetDistance() {
		return getDistance;
	}

	public void setGetDistance(boolean getDistance) {
		this.getDistance = getDistance;
	}

	public boolean shouldRotatePlayer() {
		return rotatePlayer;
	}

	public void setRotatePlayer(boolean rotatePlayer) {
		this.rotatePlayer = rotatePlayer;
	}

	private static class NearbyEntity implements Comparable<NearbyEntity> {

		private Entity entity;
		private double distanceSquared;
		
		private NearbyEntity(Entity entity, double distanceSquared) {
			this.entity = entity;
			this.distanceSquared = distanceSquared;
		}
		
		@Override
		public int compareTo(NearbyEntity e) {
			return Double.compare(e.distanceSquared, this.distanceSquared);
		}
		
	}

}
