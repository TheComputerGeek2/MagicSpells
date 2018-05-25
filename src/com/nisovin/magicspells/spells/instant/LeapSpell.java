package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LeapSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private float rotation;
	private double forwardVelocity;
	private double upwardVelocity;
	private boolean cancelDamage;
	private boolean clientOnly;
	private Subspell landSpell;
	private String landSpellName;
	private boolean allowTargeting;

	private Set<Player> jumping;

	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		this.jumping = new HashSet();

		this.rotation = getConfigFloat("rotation", 0F);
		this.forwardVelocity = getConfigInt("forward-velocity", 40) / 10D;
		this.upwardVelocity = getConfigInt("upward-velocity", 15) / 10D;
		this.cancelDamage = getConfigBoolean("cancel-damage", true);
		this.clientOnly = getConfigBoolean("client-only", false);
		this.landSpellName = getConfigString("land-spell", "");
		this.allowTargeting = getConfigBoolean("allow-targeting", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = new Subspell(landSpellName);
		if (!landSpell.process()) {
			if (!landSpellName.isEmpty()) MagicSpells.error("Leap Spell '" + internalName + "' has an invalid land-spell defined!");
			landSpell = null;
		}
	}

	public boolean isJumping(Player pl) {
		return jumping.contains(pl);
	}

	//When we don't have a target to refer to.
	private void jump(Player caster, float power) {
		propelPlayer(caster, caster.getLocation().getDirection(), power);
	}

	//When we have a target to refer to.
	private void jump(Player caster, Location target, float power) {
		/*This is so that legacy spells are supported, we wouldn't want spell systems to break
		just because one guy decided to make them targetable*/
		if (!allowTargeting) {
			jump(caster, power);
			return;
		}
		propelPlayer(caster, getVectorDir(caster.getLocation(), target), power);
	}

	private Vector getVectorDir(Location caster, Location target) {
		return target.clone().subtract(caster.toVector()).toVector();
	}

	//The main function that propels the caster in the direction of the target.
	private void propelPlayer(Player player, Vector v, float power) {
		v.setY(0).normalize().multiply(forwardVelocity * power).setY(upwardVelocity * power);
		if (rotation != 0) Util.rotateVector(v, rotation);
		if (clientOnly) {
			MagicSpells.getVolatileCodeHandler().setClientVelocity(player, v);
		} else {
			player.setVelocity(v);
		}
		jumping.add(player);
	}


	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			jump(player, power);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	//For any of the cast methods to work, I need a caster and a target (either entity or location)
	public boolean castAtLocation(Player caster, Location target, float power) {
		jump(caster, target, power);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		jump(caster, target.getLocation(), power);
		return true;
	}

	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player)) return;
        Player pl = (Player)e.getEntity();
        if (jumping.isEmpty()) return;
        if (!jumping.remove(pl)) return;
        if (landSpell != null) landSpell.cast(pl, 1);
        playSpellEffects(EffectPosition.TARGET, pl.getLocation());
        if (cancelDamage) e.setCancelled(true);
    }

}
