package com.nisovin.magicjutsus.jutsus.instant;

import java.util.regex.Pattern;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class GateJutsu extends InstantJutsu {

	private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?[0-9]+,[0-9]+,-?[0-9]+(,-?[0-9.]+,-?[0-9.]+)?$");

	private String world;
	private String coords;
	private String strGateFailed;

	public GateJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		world = getConfigString("world", "CURRENT");
		coords = getConfigString("coordinates", "SPAWN");
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			World effectiveWorld;
			if (world.equals("CURRENT")) effectiveWorld = livingEntity.getWorld();
			else if (world.equals("DEFAULT")) effectiveWorld = Bukkit.getServer().getWorlds().get(0);
			else effectiveWorld = Bukkit.getServer().getWorld(world);

			if (effectiveWorld == null) {
				MagicJutsus.error("GateJutsu '" + internalName + "' has a non existent world defined!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Get location
			Location location;
			coords = coords.replace(" ", "");
			if (RegexUtil.matches(COORDINATE_PATTERN, coords)) {
				String[] c = coords.split(",");
				int x = Integer.parseInt(c[0]);
				int y = Integer.parseInt(c[1]);
				int z = Integer.parseInt(c[2]);
				float yaw = 0;
				float pitch = 0;
				if (c.length > 3) {
					yaw = Float.parseFloat(c[3]);
					pitch = Float.parseFloat(c[4]);
				}
				location = new Location(effectiveWorld, x, y, z, yaw, pitch);
			} else if (coords.equals("SPAWN")) {
				location = effectiveWorld.getSpawnLocation();
				location = new Location(effectiveWorld, location.getX(), effectiveWorld.getHighestBlockYAt(location), location.getZ());
			} else if (coords.equals("EXACTSPAWN")) {
				location = effectiveWorld.getSpawnLocation();
			} else if (coords.equals("CURRENT")) {
				Location l = livingEntity.getLocation();
				location = new Location(effectiveWorld, l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
			} else {
				MagicJutsus.error("GateJutsu '" + internalName + "' has invalid coordinates defined!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			location.setX(location.getX() + .5);
			location.setZ(location.getZ() + .5);
			MagicJutsus.debug(3, "Gate location: " + location.toString());
			
			Block b = location.getBlock();
			if (!BlockUtils.isPathable(b) || !BlockUtils.isPathable(b.getRelative(0, 1, 0))) {
				MagicJutsus.error("GateJutsu '" + internalName + "' has landing spot blocked!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Location from = livingEntity.getLocation();
			Location to = b.getLocation();
			boolean teleported = livingEntity.teleport(location);
			if (!teleported) {
				MagicJutsus.error("GateJutsu '" + internalName + "': teleport prevented!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			playJutsuEffects(EffectPosition.CASTER, from);
			playJutsuEffects(EffectPosition.TARGET, to);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
