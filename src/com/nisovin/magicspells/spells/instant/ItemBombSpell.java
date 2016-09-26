package com.nisovin.magicspells.spells.instant;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ItemBombSpell extends InstantSpell implements TargetedLocationSpell {

	float velocity;
	float verticalAdjustment;
	int rotationOffset;
	float yOffset;
	ItemStack item;
	String itemName;
	int itemNameDelay;
	int delay;
	Subspell spell;
	boolean itemHasGravity;
	
	public ItemBombSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		velocity = getConfigFloat("velocity", 1);
		verticalAdjustment = getConfigFloat("vertical-adjustment", 0.5F);
		rotationOffset = getConfigInt("rotation-offset", 0);
		yOffset = getConfigFloat("y-offset", 1F);
		item = Util.getItemStackFromString(getConfigString("item", "stone"));
		itemName = getConfigString("item-name", null);
		itemNameDelay = getConfigInt("item-name-delay", 1);
		delay = getConfigInt("delay", 100);
		itemHasGravity = getConfigBoolean("gravity", true);
		spell = new Subspell(getConfigString("spell", ""));
		
		if (item == null) {
			MagicSpells.error("Invalid item on ItemBombSpell " + internalName);
		}
		if (itemName != null) {
			itemName = ChatColor.translateAlternateColorCodes('&', itemName);
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (!spell.process()) {
			MagicSpells.error("Invalid spell on ItemBombSpell " + internalName);
		}
	}

	@Override
	public PostCastAction castSpell(final Player player, SpellCastState state, final float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location l = player.getLocation().add(0, yOffset, 0);
			spawnItem(player, l, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void spawnItem(final Player player, Location l, final float power) {
		Vector v = getVector(l, power);
		final Item i = l.getWorld().dropItem(l, item);
		i.teleport(l);
		i.setVelocity(v);
		i.setGravity(itemHasGravity);
		i.setPickupDelay(delay * 2);
		if (itemName != null) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					i.setCustomName(itemName);
					i.setCustomNameVisible(true);
				}
			}, itemNameDelay);
		}
		MagicSpells.scheduleDelayedTask(new Runnable() {
			@Override
			public void run() {
				Location l = i.getLocation();
				i.remove();
				playSpellEffects(EffectPosition.TARGET, l);
				spell.castAtLocation(player, l, power);
			}
		}, delay);
		
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		} else {
			playSpellEffects(EffectPosition.CASTER, l);
		}
		
	}
	
	private Vector getVector(Location loc, float power) {
		Vector v = loc.getDirection();
		if (verticalAdjustment != 0) {
			v.setY(v.getY() + verticalAdjustment);
		}
		if (rotationOffset != 0) {
			Util.rotateVector(v, rotationOffset);
		}
		v.normalize().multiply(velocity);
		return v;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		spawnItem(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		spawnItem(null, target, power);
		return true;
	}

}
