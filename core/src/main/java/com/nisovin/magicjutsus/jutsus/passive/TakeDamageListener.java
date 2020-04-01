package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable of a comma separated list that can contain
// Damage causes to accept or damaging weapons to accept
public class TakeDamageListener extends PassiveListener {

	Map<DamageCause, List<PassiveJutsu>> damageCauses = new HashMap<>();
	Set<Material> types = new HashSet<>();
	Map<MagicMaterial, List<PassiveJutsu>> weapons = new LinkedHashMap<>();
	List<PassiveJutsu> always = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			always.add(jutsu);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				s = s.trim();
				boolean isDamCause = false;
				for (DamageCause c : DamageCause.values()) {
					if (!s.equalsIgnoreCase(c.name())) continue;
					List<PassiveJutsu> jutsus = damageCauses.computeIfAbsent(c, cause -> new ArrayList<>());
					jutsus.add(jutsu);
					isDamCause = true;
					break;
				}
				if (!isDamCause) {
					MagicMaterial mat;
					if (s.contains("|")) {
						String[] stuff = s.split("\\|");
						mat = MagicJutsus.getItemNameResolver().resolveItem(stuff[0]);
						if (mat != null) {
							mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
						}
					} else {
						mat = MagicJutsus.getItemNameResolver().resolveItem(s);
					}
					if (mat != null) {
						List<PassiveJutsu> list = weapons.computeIfAbsent(mat, m -> new ArrayList<>());
						list.add(jutsu);
						types.add(mat.getMaterial());
					}
				}
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player)event.getEntity();
		LivingEntity attacker = null;
		Jutsubook jutsubook = null;
		
		if (!always.isEmpty()) {
			jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : always) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (jutsubook.hasJutsu(jutsu, false)) {
					boolean casted = jutsu.activate(player, attacker);
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) {
						event.setCancelled(true);
					}
				}
			}
		}
		
		if (!damageCauses.isEmpty()) {
			List<PassiveJutsu> causeJutsus = damageCauses.get(event.getCause());
			if (causeJutsus != null && !causeJutsus.isEmpty()) {
				attacker = getAttacker(event);
				if (jutsubook == null) jutsubook = MagicJutsus.getJutsubook(player);
				for (PassiveJutsu jutsu : causeJutsus) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (jutsubook.hasJutsu(jutsu, false)) {
						boolean casted = jutsu.activate(player, attacker);
						if (PassiveListener.cancelDefaultAction(jutsu, casted)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
		
		if (!weapons.isEmpty()) {
			if (attacker == null) attacker = getAttacker(event);
			if (attacker instanceof Player) {
				Player playerAttacker = (Player)attacker;
				ItemStack item = playerAttacker.getEquipment().getItemInMainHand();
				if (item != null && item.getType() != Material.AIR) {
					List<PassiveJutsu> list = getJutsus(item);
					if (list != null) {
						if (jutsubook == null) jutsubook = MagicJutsus.getJutsubook(player);
						for (PassiveJutsu jutsu : list) {
							if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
							if (!jutsubook.hasJutsu(jutsu, false)) continue;
							boolean casted = jutsu.activate(player, attacker);
							if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	private LivingEntity getAttacker(EntityDamageEvent event) {
		if (!(event instanceof EntityDamageByEntityEvent)) return null;
		Entity e = ((EntityDamageByEntityEvent)event).getDamager();
		
		if (e instanceof LivingEntity) return (LivingEntity)e;
		
		if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof LivingEntity) {
			return (LivingEntity)((Projectile)e).getShooter();
		}
		
		return null;
	}
	
	private List<PassiveJutsu> getJutsus(ItemStack item) {
		if (!types.contains(item.getType())) return null;
		for (MagicMaterial m : weapons.keySet()) {
			if (m.equals(item)) return weapons.get(m);
		}
		return null;
	}

}
