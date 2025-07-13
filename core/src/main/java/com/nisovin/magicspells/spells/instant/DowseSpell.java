package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.TreeSet;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class DowseSpell extends InstantSpell {

	private Material material;

	private EntityType entityType;

	private String playerName;
	private String strNotFound;

	private final ConfigData<Integer> radius;

	private final boolean getDistance;
	private final ConfigData<Boolean> setCompass;
	private final ConfigData<Boolean> rotatePlayer;
	private final ConfigData<Boolean> powerAffectsRadius;

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

		setCompass = getConfigDataBoolean("set-compass", true);
		rotatePlayer = getConfigDataBoolean("rotate-player", true);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", true);

		getDistance = strCastSelf != null && strCastSelf.contains("%d");

		if (material == null && entityType == null)
			MagicSpells.error("DowseSpell '" + internalName + "' has no dowse target (block or entity) defined!");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		double radius = this.radius.get(data);
		if (powerAffectsRadius.get(data)) radius *= data.power();
		radius = Math.min(radius, MagicSpells.getGlobalRadius());

		int distance = -1;
		if (material != null) {
			Block foundBlock = null;

			Location loc = caster.getLocation();
			World world = caster.getWorld();

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
				sendMessage(strNotFound, caster, data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			if (rotatePlayer.get(data)) {
				Vector v = foundBlock.getLocation().add(0.5, 0.5, 0.5).subtract(caster.getEyeLocation()).toVector().normalize();
				caster.teleport(caster.getLocation().setDirection(v), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
			}

			if (setCompass.get(data)) caster.setCompassTarget(foundBlock.getLocation());

			if (getDistance) distance = (int) Math.round(caster.getLocation().distance(foundBlock.getLocation()));
		} else if (entityType != null) {
			// Find entity
			Entity foundEntity = null;
			double distanceSq = radius * radius;
			if (entityType == EntityType.PLAYER && playerName != null) {
				// Find specific player
				foundEntity = Bukkit.getPlayerExact(playerName);
				if (foundEntity != null) {
					if (!foundEntity.getWorld().equals(caster.getWorld())) foundEntity = null;
					else if (radius > 0 && caster.getLocation().distanceSquared(foundEntity.getLocation()) > distanceSq)
						foundEntity = null;
				}
			} else {
				// Find nearest entity
				List<Entity> nearby = caster.getNearbyEntities(radius, radius, radius);
				Location playerLoc = caster.getLocation();
				TreeSet<NearbyEntity> ordered = new TreeSet<>();
				for (Entity e : nearby) {
					if (e.getType() == entityType) {
						double d = e.getLocation().distanceSquared(playerLoc);
						if (d < distanceSq) ordered.add(new NearbyEntity(e, d));
					}
				}
				if (!ordered.isEmpty()) {
					for (NearbyEntity ne : ordered) {
						if (!(ne.entity instanceof LivingEntity le)) {
							foundEntity = ne.entity;
							break;
						}

						if (!validTargetList.canTarget(caster, le)) continue;
						SpellTargetEvent event = new SpellTargetEvent(this, data, le);
						EventUtil.call(event);
						if (event.isCancelled()) continue;
						foundEntity = event.getTarget();
						data = event.getSpellData();
						break;
					}
				}
			}

			if (foundEntity == null) {
				sendMessage(strNotFound, caster, data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			if (rotatePlayer.get(data)) {
				Location l = foundEntity instanceof LivingEntity ? ((LivingEntity) foundEntity).getEyeLocation() : foundEntity.getLocation();
				Vector v = l.subtract(caster.getEyeLocation()).toVector().normalize();
				caster.teleport(caster.getLocation().setDirection(v), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
			}

			if (setCompass.get(data)) caster.setCompassTarget(foundEntity.getLocation());

			if (getDistance) distance = (int) Math.round(caster.getLocation().distance(foundEntity.getLocation()));
		}

		playSpellEffects(EffectPosition.CASTER, caster, data);
		if (getDistance) {
			sendMessages(data, "%d", String.valueOf(distance));
			return new CastResult(PostCastAction.NO_MESSAGES, data);
		}

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
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
