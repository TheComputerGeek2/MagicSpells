package com.nisovin.magicjutsus.jutsus;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.MagicLocation;

public class LocationJutsu extends InstantJutsu {

	private MagicLocation location;
	
	private Ninjutsu jutsuToCast;
	private String jutsuToCastName;

	public LocationJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		String s = getConfigString("location", "world,0,0,0");
		try {
			String[] split = s.split(",");
			String world = split[0];
			double x = Double.parseDouble(split[1]);
			double y = Double.parseDouble(split[2]);
			double z = Double.parseDouble(split[3]);
			float yaw = 0;
			float pitch = 0;
			if (split.length > 4) yaw = Float.parseFloat(split[4]);
			if (split.length > 5) pitch = Float.parseFloat(split[5]);
			location = new MagicLocation(world, x, y, z, yaw, pitch);
		} catch (Exception e) {
			MagicJutsus.error("LocationJutsu '" + jutsuName + "' has an invalid location defined!");
		}

		jutsuToCastName = getConfigString("jutsu", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuToCast = new Ninjutsu(jutsuToCastName);
		if (!jutsuToCast.process() || !jutsuToCast.isTargetedLocationJutsu()) {
			MagicJutsus.error("LocationJutsu '" + internalName + "' has an invalid jutsu defined!");
			jutsuToCast = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = location.getLocation();
			if (loc == null) return PostCastAction.ALREADY_HANDLED;

			if (jutsuToCast != null) jutsuToCast.castAtLocation(livingEntity, loc, power);
			playJutsuEffects(livingEntity, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
