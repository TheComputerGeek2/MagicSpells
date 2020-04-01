package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class TimeJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private int timeToSet;

	private String strAnnounce;
		
	public TimeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		timeToSet = getConfigInt("time-to-set", 0);
		strAnnounce = getConfigString("str-announce", "The sun suddenly appears in the sky.");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			World world = livingEntity.getWorld();
			setTime(world);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		setTime(target.getWorld());
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		setTime(target.getWorld());
		return true;
	}

	private void setTime(World world) {
		world.setTime(timeToSet);
		for (Player p : world.getPlayers()) sendMessage(strAnnounce, p, MagicJutsus.NULL_ARGS);
	}

}
