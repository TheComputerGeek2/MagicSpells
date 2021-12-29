package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.lang.ref.WeakReference;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;

public class EntitySelectSpell extends TargetedSpell {
	
	private Map<UUID, WeakReference<LivingEntity>> targets;
	
	public EntitySelectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targets = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power, args);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(caster);
			
			targets.put(caster.getUniqueId(), new WeakReference<>(targetInfo.getTarget()));
			sendMessages(caster, targetInfo.getTarget(), args);
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
