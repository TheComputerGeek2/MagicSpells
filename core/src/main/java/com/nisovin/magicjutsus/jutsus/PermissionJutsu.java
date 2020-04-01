package com.nisovin.magicjutsus.jutsus;

import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class PermissionJutsu extends InstantJutsu {

	private int duration;
	
	private List<String> permissionNodes;
	
	public PermissionJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		duration = getConfigInt("duration", 0);
		permissionNodes = getConfigStringList("permission-nodes", null);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && duration > 0 && permissionNodes != null) {
			for (String node : permissionNodes) {
				livingEntity.addAttachment(MagicJutsus.plugin, node, true, duration);
			}
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
