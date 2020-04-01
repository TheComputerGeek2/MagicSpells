package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class RoarJutsu extends InstantJutsu {

	private float radius;

	private String strNoTarget;

	private boolean cancelIfNoTargets;

	public RoarJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		radius = getConfigFloat("radius", 8F);

		strNoTarget = getConfigString("str-no-target", "No targets found.");

		cancelIfNoTargets = getConfigBoolean("cancel-if-no-targets", true);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			int count = 0;
			List<Entity> entities = livingEntity.getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity)) continue;
				if (entity instanceof Player) continue;
				if (!validTargetList.canTarget(livingEntity, entity)) continue;
				MagicJutsus.getVolatileCodeHandler().setTarget((LivingEntity) entity, livingEntity);
				playJutsuEffectsTrail(livingEntity.getLocation(), entity.getLocation());
				playJutsuEffects(EffectPosition.TARGET, entity);
				count++;
			}
			if (cancelIfNoTargets && count == 0) {
				sendMessage(strNoTarget, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
