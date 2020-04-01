package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;

public class CastAtMarkJutsu extends InstantJutsu {

	private String strNoMark;
	private String markJutsuName;
	private String jutsuToCastName;

	private MarkJutsu markJutsu;
	private Ninjutsu jutsuToCast;

	private boolean initialized = false;

	public CastAtMarkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		strNoMark = getConfigString("str-no-mark", "You do not have a mark specified");
		markJutsuName = getConfigString("mark-jutsu", "");
		jutsuToCastName = getConfigString("jutsu", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (initialized) return;

		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(markJutsuName);
		if (jutsu == null || !(jutsu instanceof MarkJutsu)) {
			MagicJutsus.error("CastAtMarkJutsu '" + internalName + "' has an invalid mark-jutsu defined!");
			return;
		}
		
		markJutsu = (MarkJutsu) jutsu;
		
		jutsuToCast = new Ninjutsu(jutsuToCastName);
		if (!jutsuToCast.process() || !jutsuToCast.isTargetedLocationJutsu()) {
			MagicJutsus.error("CastAtMarkJutsu '" + internalName + "' has an invalid jutsu defined!");
			return;
		}
		
		initialized = true;
	}

	@Override
	public void turnOff() {
		super.turnOff();

		markJutsu = null;
		jutsuToCast = null;
		initialized = false;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (!initialized) return PostCastAction.HANDLE_NORMALLY;
		if (state == JutsuCastState.NORMAL) {
			Location effectiveMark = markJutsu.getEffectiveMark(livingEntity);
			if (effectiveMark == null) {
				sendMessage(livingEntity, strNoMark);
				return PostCastAction.HANDLE_NORMALLY;
			}
			jutsuToCast.castAtLocation(livingEntity, effectiveMark, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
