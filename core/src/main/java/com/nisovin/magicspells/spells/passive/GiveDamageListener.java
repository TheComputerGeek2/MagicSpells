package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of a pipe separated list that can contain
// damage causes or damaging magic items to accept
public class GiveDamageListener extends PassiveListener {

	private final EnumSet<DamageCause> damageCauses = EnumSet.noneOf(DamageCause.class);
	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();

			boolean isDamCause = false;
			for (EntityDamageEvent.DamageCause c : EntityDamageEvent.DamageCause.values()) {
				if (!s.equalsIgnoreCase(c.name())) continue;

				damageCauses.add(c);
				isDamCause = true;
				break;
			}
			if (isDamCause) continue;

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid damage cause or magic item '" + s + "' in givedamage trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity attacked)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (event.getFinalDamage() == 0D) return;

		LivingEntity caster = getAttacker(event);
		if (caster == null || !hasSpell(caster) || !canTrigger(caster)) return;

		if (!damageCauses.isEmpty() && !damageCauses.contains(event.getCause())) return;

		if (!items.isEmpty()) {
			EntityEquipment eq = caster.getEquipment();
			if (eq == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(eq.getItemInMainHand());
			if (itemData == null || !contains(itemData)) return;
		}

		boolean casted = passiveSpell.activate(caster, attacked);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private LivingEntity getAttacker(EntityDamageByEntityEvent event) {
		Entity e = event.getDamager();
		if (e instanceof LivingEntity) return (LivingEntity) e;
		if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Projectile) e).getShooter();
		}
		return null;
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}

}
