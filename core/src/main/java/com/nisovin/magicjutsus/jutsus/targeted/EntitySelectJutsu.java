package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.lang.ref.WeakReference;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;

public class EntitySelectJutsu extends TargetedJutsu {
	
	private Map<UUID, WeakReference<LivingEntity>> targets;
	
	public EntitySelectJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		targets = new HashMap<>();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(livingEntity);
			
			targets.put(livingEntity.getUniqueId(), new WeakReference<>(targetInfo.getTarget()));
			sendMessages(livingEntity, targetInfo.getTarget());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public void turnOff() {
		super.turnOff();

		targets.clear();
		targets = null;
	}

	public LivingEntity getTarget(Player player) {
		UUID id = player.getUniqueId();
		if (!targets.containsKey(id)) return null;
		
		WeakReference<LivingEntity> ref = targets.get(id);
		
		if (ref == null) {
			targets.remove(id);
			return null;
		}
		
		return ref.get();
	}
	
	private void remove(Player player) {
		targets.remove(player.getUniqueId());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		remove(event.getPlayer());
	}
	
}
