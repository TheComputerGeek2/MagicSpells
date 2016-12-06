package com.nisovin.magicspells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class MagicLogger implements Listener {

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	FileWriter writer;
	
	public MagicLogger(MagicSpells plugin) {
		File file = new File(plugin.getDataFolder(), "log-" + System.currentTimeMillis() + ".txt");
		try {
			writer = new FileWriter(file, true);
			MagicSpells.registerEvents(this);
		} catch (IOException e) {
			MagicSpells.handleException(e);
		}
	}
	
	public void disable() {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				MagicSpells.handleException(e);
			}
		}
		writer = null;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellLearn(SpellLearnEvent event) {
		log("LEARN" + 
				"; spell=" + event.getSpell().getInternalName() + 
				"; player=" + event.getLearner().getName() + 
				"; loc=" + formatLoc(event.getLearner().getLocation()) +
				"; source=" + event.getSource().name() +
				"; teacher=" + getTeacherName(event.getTeacher()) +
				"; canceled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellForget(SpellForgetEvent event) {
		log("FORGET" + 
				"; spell=" + event.getSpell().getInternalName() + 
				"; player=" + event.getForgetter().getName() + 
				"; loc=" + formatLoc(event.getForgetter().getLocation()) +
				"; canceled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellCast(SpellCastEvent event) {

		log("BEGIN CAST" +
				"; spell=" + event.getSpell().getInternalName() + 
				"; caster=" + event.getCaster() +
				"; loc=" + formatLoc(event.getCaster().getLocation()) +
				"; state=" + event.getSpellCastState().name() +
				"; power=" + event.getPower() +
				"; canceled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellTarget(SpellTargetEvent event) {
		Player caster = event.getCaster();
		log("  TARGET ENTITY" + 
				"; spell=" + event.getSpell().getInternalName() + 
				"; caster=" + (caster != null ? caster.getName() : "null") + 
				"; casterloc=" + (caster != null ? formatLoc(caster.getLocation()) : "null") +
				": target=" + getTargetName(event.getTarget()) + 
				"; targetloc=" + formatLoc(event.getTarget().getLocation()) +
				"; canceled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellTargetLocation(SpellTargetLocationEvent event) {
		log("  TARGET LOCATION" + 
				"; spell=" + event.getSpell().getInternalName() + 
				"; caster=" + event.getCaster().getName() + 
				"; casterloc=" + formatLoc(event.getCaster().getLocation()) +
				"; targetloc=" + formatLoc(event.getTargetLocation()) +
				"; canceled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellCasted(SpellCastedEvent event) {
		log("  END CAST" + 
				"; spell=" + event.getSpell().getInternalName() + 
				"; caster=" + event.getCaster().getName() + 
				"; loc=" + formatLoc(event.getCaster().getLocation()) +
				"; state=" + event.getSpellCastState().name() +
				"; power=" + event.getPower() +
				"; result=" + event.getPostCastAction().name());
	}
	
	private String formatLoc(Location location) {
		return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
	}
	
	private String getTargetName(LivingEntity target) {
		if (target instanceof Player) {
			return ((Player)target).getName();
		} else {
			return target.getType().name();
		}
	}
	
	private String getTeacherName(Object o) {
		if (o == null) {
			return "none";
		} else if (o instanceof Player) {
			return "player-" + ((Player)o).getName();
		} else if (o instanceof Spell) {
			return "spell-" + ((Spell)o).getInternalName();
		} else if (o instanceof Block) {
			return "block-" + formatLoc(((Block)o).getLocation());
		} else {
			return o.toString();
		}
	}
	
	private void log(String string) {
		if (writer != null) {
			try {
				writer.write("[" + dateFormat.format(new Date()) + "] " + string + "\n");
			} catch (IOException e) {
				DebugHandler.debugIOException(e);
			}
		}
	}
	
}
