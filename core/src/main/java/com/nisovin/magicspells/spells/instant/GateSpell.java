package com.nisovin.magicspells.spells.instant;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Vehicle;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class GateSpell extends InstantSpell {

	private String strGateFailed;
	private final ConfigData<String> world;
	private final ConfigData<String> coordinates;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		world = getConfigDataString("world", "CURRENT");
		coordinates = getConfigDataString("coordinates", "SPAWN");
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
	}

	@Override
	public CastResult cast(SpellData data) {
		String world = this.world.get(data);

		World effectiveWorld = switch (world.toUpperCase()) {
			case "CURRENT" -> data.caster().getWorld();
			case "DEFAULT" -> Bukkit.getWorlds().get(0);
			default -> Bukkit.getWorld(world);
		};

		if (effectiveWorld == null) {
			MagicSpells.error("GateSpell '" + internalName + "' has a non existent world defined!");
			sendMessage(strGateFailed, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		String coordinates = this.coordinates.get(data).replace(" ", "");
		Location location = switch (coordinates.toUpperCase()) {
			case "SPAWN" -> {
				Location spawn = effectiveWorld.getSpawnLocation();
				spawn.setY(effectiveWorld.getHighestBlockYAt(spawn) + 1);

				yield spawn;
			}
			case "EXACTSPAWN" -> effectiveWorld.getSpawnLocation();
			case "CURRENT" -> {
				Location current = data.caster().getLocation();
				current.setWorld(effectiveWorld);

				yield current;
			}
			default -> {
				String[] coords = coordinates.split(",");
				if (coords.length < 3) yield null;

				try {
					double x = Double.parseDouble(coords[0]);
					double y = Double.parseDouble(coords[1]);
					double z = Double.parseDouble(coords[2]);

					float yaw = 0, pitch = 0;
					if (coords.length > 3) yaw = Float.parseFloat(coords[3]);
					if (coords.length > 4) pitch = Float.parseFloat(coords[4]);

					yield new Location(effectiveWorld, x, y, z, yaw, pitch);
				} catch (NumberFormatException e) {
					yield null;
				}
			}
		};

		if (location == null) {
			MagicSpells.error("GateSpell '" + internalName + "' has invalid coordinates defined!");
			sendMessage(strGateFailed, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}
		MagicSpells.debug(3, "Gate location: " + location);

		Block b = location.getBlock();
		if (!b.isPassable() || !b.getRelative(0, 1, 0).isPassable()) {
			MagicSpells.error("GateSpell '" + internalName + "' has landing spot blocked!");
			sendMessage(strGateFailed, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Location from = data.caster().getLocation();
		if (data.caster() instanceof Vehicle || !data.caster().isValid()) {
			MagicSpells.error("GateSpell '" + internalName + "': teleport prevented!");
			sendMessage(strGateFailed, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}
		Util.tryTeleportMountedAsync(data.caster(), location);

		playSpellEffects(EffectPosition.CASTER, from, data);
		playSpellEffects(EffectPosition.TARGET, location, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public String getStrGateFailed() {
		return strGateFailed;
	}

	public void setStrGateFailed(String strGateFailed) {
		this.strGateFailed = strGateFailed;
	}

}
