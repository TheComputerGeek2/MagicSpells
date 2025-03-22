package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.util.RayTraceResult;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class SpawnTntSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Map<Integer, TNTData> tnts;

	private final ConfigData<Integer> fuse;

	private final ConfigData<Float> velocity;
	private final ConfigData<Float> upVelocity;

	private final ConfigData<Boolean> cancelGravity;
	private final ConfigData<Boolean> cancelExplosion;
	private final ConfigData<Boolean> preventBlockDamage;

	private String spellToCastName;
	private Subspell spellToCast;

	public SpawnTntSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fuse = getConfigDataInt("fuse", TimeUtil.TICKS_PER_SECOND);

		velocity = getConfigDataFloat("velocity", 0F);
		upVelocity = getConfigDataFloat("up-velocity", velocity);

		cancelGravity = getConfigDataBoolean("cancel-gravity", false);
		cancelExplosion = getConfigDataBoolean("cancel-explosion", false);
		preventBlockDamage = getConfigDataBoolean("prevent-block-damage", false);

		spellToCastName = getConfigString("spell", "");

		tnts = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName,
				"SpawnTntSpell '" + internalName + "' has an invalid spell defined!",
				true);
	}

	@Override
	public CastResult cast(SpellData data) {
		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(data);

		Block last = result.getHitBlock();
		Block prev = last.getRelative(result.getHitBlockFace());

		Location location = prev.getLocation().add(0.5, 0, 0.5);
		location.setDirection(location.toVector().subtract(data.caster().getLocation().toVector()));

		SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, location);
		if (!targetEvent.callEvent()) return noTarget(targetEvent);

		return castAtLocation(targetEvent.getSpellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location loc = data.location();

		TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class, t -> {
			if (cancelGravity.get(data)) t.setGravity(false);
			t.setFuseTicks(fuse.get(data));
			t.setSource(data.caster());

			float velocity = this.velocity.get(data);
			float upVelocity = this.upVelocity.get(data);

			if (velocity > 0) t.setVelocity(loc.getDirection().setY(0).normalize().multiply(velocity).setY(upVelocity));
			else if (upVelocity > 0) t.setVelocity(new Vector(0, upVelocity, 0));
		});

		playSpellEffects(EffectPosition.PROJECTILE, tnt, data);
		if (data.hasCaster())
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, data.caster().getLocation(), tnt.getLocation(), data.caster(), tnt, data);

		tnts.put(tnt.getEntityId(), new TNTData(data.noLocation(), cancelExplosion.get(data), preventBlockDamage.get(data)));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity tnt = event.getEntity();

		TNTData data = tnts.remove(tnt.getEntityId());
		if (data == null) return;

		if (data.cancelExplosion) {
			event.setCancelled(true);
			tnt.remove();
		}

		if (data.preventBlockDamage) {
			event.blockList().clear();
			event.setYield(0F);
		}

		for (Block b : event.blockList()) {
			Location location = b.getLocation();
			SpellData subData = data.spellData.location(location);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, location, subData);
		}
		if (spellToCast == null || data.spellData.hasCaster() && !data.spellData.caster().isValid()) return;

		spellToCast.subcast(data.spellData.location(tnt.getLocation()));
	}

	private record TNTData(SpellData spellData, boolean cancelExplosion, boolean preventBlockDamage) {
	}

}
