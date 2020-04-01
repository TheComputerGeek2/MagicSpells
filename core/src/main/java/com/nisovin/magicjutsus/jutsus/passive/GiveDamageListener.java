package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.materials.MagicItemWithNameMaterial;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable that may contain a comma separated list
// Of weapons to trigger on
public class GiveDamageListener extends PassiveListener {

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
				MagicMaterial mat;
				if (s.contains("|")) {
					String[] stuff = s.split("\\|");
					mat = MagicJutsus.getItemNameResolver().resolveItem(stuff[0]);
					if (mat != null) mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
				} else {
					mat = MagicJutsus.getItemNameResolver().resolveItem(s);
				}
				if (mat != null) {
					List<PassiveJutsu> list = weapons.computeIfAbsent(mat, magicMaterial -> new ArrayList<>());
					list.add(jutsu);
					types.add(mat.getMaterial());
				}
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		Player player = getPlayerAttacker(event);
		if (player == null || !(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity attacked = (LivingEntity)event.getEntity();
		Jutsubook jutsubook = null;
		
		if (!always.isEmpty()) {
			jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : always) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu, false)) continue;
				boolean casted = jutsu.activate(player, attacked);
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
		
		if (!weapons.isEmpty()) {
			ItemStack item = player.getEquipment().getItemInMainHand();
			if (item != null && item.getType() != Material.AIR) {
				List<PassiveJutsu> list = getJutsus(item);
				if (list != null) {
					if (jutsubook == null) jutsubook = MagicJutsus.getJutsubook(player);
					for (PassiveJutsu jutsu : list) {
						if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
						if (!jutsubook.hasJutsu(jutsu, false)) continue;
						boolean casted = jutsu.activate(player, attacked);
						if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
		Entity e = event.getDamager();
		if (e instanceof Player) return (Player)e;
		if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof Player) {
			return (Player)((Projectile)e).getShooter();
		}
		return null;
	}
	
	private List<PassiveJutsu> getJutsus(ItemStack item) {
		if (!types.contains(item.getType())) return null;
		for (Entry<MagicMaterial, List<PassiveJutsu>> entry : weapons.entrySet()) {
			if (entry.getKey().equals(item)) return entry.getValue();
		}
		return null;
	}

}
