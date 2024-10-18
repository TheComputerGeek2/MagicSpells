package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class MagicBondSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> duration;

	private final String strDurationEnd;

	private SpellFilter filter;

	public MagicBondSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 200);
		strDurationEnd = getConfigString("str-duration", "");
	}

	@Override
	protected void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();
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

		SpellMonitor monitor = new SpellMonitor(data.caster(), data.target());
		MagicSpells.registerEvents(monitor);

		MagicSpells.scheduleDelayedTask(() -> {
			if (!strDurationEnd.isEmpty()) {
				MagicSpells.sendMessage(strDurationEnd, monitor.caster, data);
				MagicSpells.sendMessage(strDurationEnd, monitor.target, data);
			}

			HandlerList.unregisterAll(monitor);
		}, duration.get(data));

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private class SpellMonitor implements Listener {

		private final LivingEntity caster;
		private final LivingEntity target;

		private SpellMonitor(LivingEntity caster, LivingEntity target) {
			this.caster = caster;
			this.target = target;
		}

		@EventHandler
		public void onPlayerLeave(PlayerQuitEvent e) {
			Player player = e.getPlayer();
			if (caster.equals(player) || target.equals(player)) HandlerList.unregisterAll(this);
		}

		@EventHandler(ignoreCancelled = true)
		public void onRemove(EntityRemoveFromWorldEvent event) {
			Entity entity = event.getEntity();
			if (!entity.isValid() && (caster.equals(entity) || target.equals(entity))) HandlerList.unregisterAll(this);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerSpellCast(SpellCastEvent e) {
			if (e.getSpellCastState() != SpellCastState.NORMAL || !caster.equals(e.getCaster()) || !caster.isValid() || !target.isValid())
				return;

			Spell spell = e.getSpell();
			if (spell instanceof MagicBondSpell || !filter.check(spell)) return;

			spell.hardCast(new SpellData(target));
		}

	}

}
