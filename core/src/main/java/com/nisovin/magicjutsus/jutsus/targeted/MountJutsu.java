package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class MountJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int duration;

	private boolean reverse;

	public MountJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		duration = getConfigInt("duration", 0);

		reverse = getConfigBoolean("reverse", false);

		if (duration < 0) duration = 0;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);
			mount(livingEntity, target);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		mount(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void mount(LivingEntity caster, LivingEntity target) {
		if (caster == null || target == null) return;

		if (reverse) {
			if (!caster.getPassengers().isEmpty()) caster.eject();
			if (caster.getVehicle() != null) caster.getVehicle().eject();
			if (target.getVehicle() != null) target.getVehicle().eject();

			caster.addPassenger(target);
			if (duration > 0) {
				LivingEntity finalTarget = target;
				MagicJutsus.scheduleDelayedTask(() -> {
					caster.removePassenger(finalTarget);
				}, duration);
			}
			sendMessages(caster, target);
			return;
		}

		if (caster.getVehicle() != null) {
			Entity veh = caster.getVehicle();
			veh.eject();
			List<Entity> passengers = caster.getPassengers();
			if (passengers.isEmpty()) return;

			caster.eject();
			for (Entity e : passengers) {
				veh.addPassenger(e);
				if (duration > 0) {
					MagicJutsus.scheduleDelayedTask(() -> {
						veh.removePassenger(e);
					}, duration);
				}
			}
			return;
		}

		for (Entity e : target.getPassengers()) {
			if (!(e instanceof LivingEntity)) continue;
			target = (LivingEntity) e;
			break;
		}

		caster.eject();
		target.addPassenger(caster);
		if (duration > 0) {
			LivingEntity finalTarget1 = target;
			MagicJutsus.scheduleDelayedTask(() -> {
				finalTarget1.removePassenger(caster);
			}, duration);
		}
		sendMessages(caster, target);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Entity vehicle = player.getVehicle();
		List<Entity> passengers = player.getPassengers();
		if (!passengers.isEmpty()) player.eject();
		if (vehicle instanceof Player) vehicle.eject();
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Entity vehicle = player.getVehicle();
		List<Entity> passengers = player.getPassengers();
		if (!passengers.isEmpty()) player.eject();
		if (vehicle instanceof Player) vehicle.eject();
	}

}
