package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class MountSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> duration;

	private final ConfigData<Boolean> reverse;

	public MountSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 0);

		reverse = getConfigDataBoolean("reverse", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		int duration = this.duration.get(data);

		if (reverse.get(data)) {
			if (!caster.getPassengers().isEmpty()) caster.eject();
			if (caster.getVehicle() != null) caster.getVehicle().eject();
			if (target.getVehicle() != null) target.getVehicle().eject();

			caster.addPassenger(target);
			if (duration > 0) {
				LivingEntity finalTarget = target;
				MagicSpells.scheduleDelayedTask(() -> caster.removePassenger(finalTarget), duration, caster);
			}

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (caster.getVehicle() != null) {
			Entity veh = caster.getVehicle();
			veh.eject();

			List<Entity> passengers = caster.getPassengers();
			if (passengers.isEmpty()) {
				playSpellEffects(data);
				return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
			}

			caster.eject();
			for (Entity e : passengers) {
				veh.addPassenger(e);
				if (duration > 0) {
					MagicSpells.scheduleDelayedTask(() -> veh.removePassenger(e), duration, veh);
				}
			}

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		for (Entity e : target.getPassengers()) {
			if (!(e instanceof LivingEntity le)) continue;
			target = le;
			break;
		}

		caster.eject();
		target.addPassenger(caster);
		if (duration > 0) {
			LivingEntity finalTarget = target;
			MagicSpells.scheduleDelayedTask(() -> finalTarget.removePassenger(caster), duration, finalTarget);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
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
