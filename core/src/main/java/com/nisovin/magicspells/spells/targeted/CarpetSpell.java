package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.Map.Entry;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class CarpetSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Map<Block, CarpetData> blocks;

	private final ConfigData<Material> material;

	private final int touchCheckInterval;
	private final ConfigData<Integer> radius;
	private final ConfigData<Integer> duration;

	private final ConfigData<Boolean> circle;
	private final ConfigData<Boolean> removeOnTouch;
	private final ConfigData<Boolean> powerAffectsRadius;

	private final String spellOnTouchName;
	private Subspell spellOnTouch;

	private TouchChecker checker;

	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		material = getConfigDataMaterial("block", Material.WHITE_CARPET);

		radius = getConfigDataInt("radius", 1);
		duration = getConfigDataInt("duration", 0);
		touchCheckInterval = getConfigInt("touch-check-interval", 3);

		circle = getConfigDataBoolean("circle", false);
		removeOnTouch = getConfigDataBoolean("remove-on-touch", true);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", true);

		spellOnTouchName = getConfigString("spell-on-touch", "");

		blocks = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnTouch = initSubspell(spellOnTouchName,
				"CarpetSpell '" + internalName + "' has an invalid spell-on-touch defined!",
				true);

		if (spellOnTouch != null) checker = new TouchChecker();
	}

	@Override
	public void turnOff() {
		super.turnOff();

		for (Entry<Block, CarpetData> entry : blocks.entrySet()) {
			entry.getKey().setType(entry.getValue().air());
		}
		blocks.clear();
		if (checker != null) checker.stop();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (targetSelf.get(data)) {
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!event.callEvent()) return noTarget(event);

			data = event.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data, false);
			if (info.noTarget()) return noTarget(info);

			data = info.spellData();
		}

		return layCarpet(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (targetSelf.get(data)) {
			if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			data = data.location(data.caster().getLocation());
		}

		return layCarpet(data);
	}

	private CastResult layCarpet(SpellData data) {
		Location loc = data.location();

		if (!loc.getBlock().getType().isOccluding()) {
			int c = 0;
			while (!loc.getBlock().getRelative(0, -1, 0).getType().isOccluding() && c <= 2) {
				loc.subtract(0, 1, 0);
				c++;
			}

			data = data.location(loc);
		} else {
			int c = 0;
			while (loc.getBlock().getType().isOccluding() && c <= 2) {
				loc.add(0, 1, 0);
				c++;
			}

			data = data.location(loc);
		}

		Block b;
		int y = loc.getBlockY();

		int rad = this.radius.get(data);
		if (powerAffectsRadius.get(data)) rad = Math.round(rad * data.power());

		Material material = this.material.get(data);
		if (!material.isBlock()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		boolean removeOnTouch = this.removeOnTouch.get(data);
		boolean circle = this.circle.get(data);

		final List<Block> blockList = new ArrayList<>();
		for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
			for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
				b = loc.getWorld().getBlockAt(x, y, z);
				if (circle && loc.getBlock().getLocation().distanceSquared(b.getLocation()) > rad * rad) continue;

				if (b.getType().isOccluding()) b = b.getRelative(0, 1, 0);
				else if (!b.getRelative(0, -1, 0).getType().isOccluding()) b = b.getRelative(0, -1, 0);

				if (!b.getType().isAir() && !b.getRelative(0, -1, 0).getType().isSolid()) continue;

				blocks.put(b, new CarpetData(data, material, b.getType(), removeOnTouch));
				b.setType(material, false);
				blockList.add(b);

				Location subLoc = b.getLocation().add(0.5, 0, 0.5);
				playSpellEffects(EffectPosition.TARGET, subLoc, data.location(subLoc));
			}
		}

		int duration = this.duration.get(data);
		if (duration > 0 && !blockList.isEmpty()) {
			MagicSpells.scheduleDelayedTask(() -> {
				for (Block b1 : blockList) {
					if (!material.equals(b1.getType())) continue;

					CarpetData carpetData = blocks.remove(b1);
					b1.setType(carpetData.air);
				}
			}, duration, loc);
		}

		if (data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private record CarpetData(SpellData data, Material material, Material air, boolean removeOnTouch) {
	}

	private class TouchChecker implements Runnable {

		private final ScheduledTask task;

		private TouchChecker() {
			task = MagicSpells.scheduleRepeatingTask(this, touchCheckInterval, touchCheckInterval);
		}

		@Override
		public void run() {
			for (Entry<Block, CarpetData> entry : new HashSet<>(blocks.entrySet())) {
				Block block = entry.getKey();
				CarpetData carpetData = entry.getValue();
				for (LivingEntity target : block.getLocation().add(0.5, 1.5, 0.5).getNearbyLivingEntities(0.5)) {
					if (!carpetData.material.equals(block.getType())) continue;
					if (!validTargetList.canTarget(carpetData.data.caster(), target)) continue;

					if (carpetData.removeOnTouch) {
						block.setType(carpetData.air);
						blocks.remove(block);
					}

					if (spellOnTouch != null) {
						SpellTargetEvent event = new SpellTargetEvent(CarpetSpell.this, carpetData.data, target);
						if (!event.callEvent()) continue;

						spellOnTouch.subcast(event.getSpellData());
					}
				}
			}
		}

		private void stop() {
			MagicSpells.cancelTask(task);
		}

	}

}
