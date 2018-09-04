package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;

public class ForcepushSpell extends InstantSpell {
	
	private int radius;
	private int force;
	private int yForce;
	private int maxYForce;
	private boolean addVelocityInstead;
	
	public ForcepushSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.radius = getConfigInt("range", 3);
		this.force = getConfigInt("pushback-force", 30);
		this.yForce = getConfigInt("additional-vertical-force", 15);
		this.maxYForce = getConfigInt("max-vertical-force", 20);
		this.addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			knockback(player, this.radius, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public void knockback(Player player, int castingRange, float basePower) {
	    Vector p = player.getLocation().toVector();
		List<Entity> entities = player.getNearbyEntities(castingRange, castingRange, castingRange);
		Vector e;
		Vector v;
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!this.validTargetList.canTarget(player, entity)) continue;
			LivingEntity target = (LivingEntity)entity;
			float power = basePower;
			SpellTargetEvent event = new SpellTargetEvent(this, player, target, power);
			EventUtil.call(event);
			if (event.isCancelled()) continue;
			
			if (target != event.getTarget() && target.getWorld().equals(event.getTarget().getWorld())) {
				target = event.getTarget();
			}
			power = event.getPower();
			
			e = target.getLocation().toVector();
			v = e.subtract(p).normalize().multiply(this.force/10.0 * power);
			if (this.force != 0) {
				v.setY(v.getY() + (this.yForce/10.0 * power));
			} else {
				v.setY(this.yForce/10.0 * power);
			}
			if (v.getY() > (this.maxYForce/10.0)) v.setY(this.maxYForce/10.0);
			if (!addVelocityInstead) target.setVelocity(v);
			else target.setVelocity(target.getVelocity().add(v));
			playSpellEffects(EffectPosition.TARGET, target);
	    }
		playSpellEffects(EffectPosition.CASTER, player);
	}

}
