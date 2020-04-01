package com.nisovin.magicjutsus.jutsus.instant;

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

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class DowseJutsu extends InstantJutsu {

	private Material material;

	private EntityType entityType;

	private String playerName;
	private String strNotFound;

	private int radius;

	private boolean setCompass;
	private boolean getDistance;
	private boolean rotatePlayer;

	public DowseJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		String blockName = getConfigString("block-type", "");
		String entityName = getConfigString("entity-type", "");

		if (!blockName.isEmpty()) material = Material.getMaterial(blockName.toUpperCase());
		if (!entityName.isEmpty()) {
			if (entityName.equalsIgnoreCase("player")) {
				entityType = EntityType.PLAYER;
			} else if (entityName.toLowerCase().startsWith("player:")) {
				entityType = EntityType.PLAYER;
				playerName = entityName.split(":")[1];
			} else {
				entityType = Util.getEntityType(entityName);
			}
		}

		strNotFound = getConfigString("str-not-found", "No dowsing target found.");

		radius = getConfigInt("radius", 4);

		setCompass = getConfigBoolean("set-compass", true);
		rotatePlayer = getConfigBoolean("rotate-player", true);

		getDistance = strCastSelf != null && strCastSelf.contains("%d");

		if (material == null && entityType == null) MagicJutsus.error("DowseJutsu '" + internalName + "' has no dowse target (block or entity) defined!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
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
				for (int r = 1; r <= Math.round(radius * power); r++) {
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
								JutsuTargetEvent event = new JutsuTargetEvent(this, player, (LivingEntity) ne.entity, power);
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
				} else {
					if (rotatePlayer) {
						Location l = foundEntity instanceof LivingEntity ? ((LivingEntity) foundEntity).getEyeLocation() : foundEntity.getLocation();
						Vector v = l.subtract(player.getEyeLocation()).toVector().normalize();
						Util.setFacing(player, v);
					}
					if (setCompass) player.setCompassTarget(foundEntity.getLocation());
					if (getDistance) distance = (int) Math.round(player.getLocation().distance(foundEntity.getLocation()));
				}
			}
			
			playJutsuEffects(EffectPosition.CASTER, player);
			if (getDistance) {
				sendMessage(formatMessage(strCastSelf, "%d", distance + ""), player, args);
				sendMessageNear(player, strCastOthers);
				return PostCastAction.NO_MESSAGES;
			}
		}
		
		return PostCastAction.HANDLE_NORMALLY;
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
			if (e.distanceSquared < this.distanceSquared) return -1;
			if (e.distanceSquared > this.distanceSquared) return 1;
			return 0;
		}
		
	}

}
