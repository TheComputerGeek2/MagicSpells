package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class MagicBondJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<LivingEntity, LivingEntity> bondTarget;

	private int duration;

	private String strDurationEnd;

	private JutsuFilter filter;

	public MagicBondJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		duration = getConfigInt("duration", 200);

		strDurationEnd = getConfigString("str-duration", "");

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		bondTarget = new HashMap<>();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			bond(livingEntity, target.getTarget(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		bond(caster, target, power);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}

	private void bond(LivingEntity caster, LivingEntity target, float power) {
		bondTarget.put(caster, target);
		playJutsuEffects(caster, target);
		JutsuMonitor monitorBond = new JutsuMonitor(caster, target, power);
		MagicJutsus.registerEvents(monitorBond);

		MagicJutsus.scheduleDelayedTask(() -> {
			if (!strDurationEnd.isEmpty()) {
				if (caster instanceof Player) MagicJutsus.sendMessage((Player) caster, strDurationEnd);
				if (target instanceof Player) MagicJutsus.sendMessage((Player) target, strDurationEnd);
			}
			bondTarget.remove(caster);

			HandlerList.unregisterAll(monitorBond);
		}, duration);
	}

	private class JutsuMonitor implements Listener {

		private LivingEntity caster;
		private LivingEntity target;
		private float power;

		private JutsuMonitor(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
		}

		@EventHandler
		public void onPlayerLeave(PlayerQuitEvent e) {
			if (bondTarget.containsKey(e.getPlayer()) || bondTarget.containsValue(e.getPlayer())) {
				bondTarget.remove(caster);
			}
		}

		@EventHandler
		public void onPlayerJutsuCast(JutsuCastEvent e) {
			Jutsu jutsu = e.getJutsu();
			if (e.getCaster() != caster || jutsu instanceof MagicBondJutsu) return;
			if (jutsu.onCooldown(caster)) return;
			if (!bondTarget.containsKey(caster) && !bondTarget.containsValue(target)) return;
			if (target.isDead()) return;
			if (!filter.check(jutsu)) return;

			jutsu.cast(target);

		}

		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (!getClass().getName().equals(other.getClass().getName())) return false;
			JutsuMonitor otherMonitor = (JutsuMonitor)other;
			if (otherMonitor.caster != caster) return false;
			if (otherMonitor.target != target) return false;
			if (otherMonitor.power != power) return false;
			return true;
		}

	}

}
