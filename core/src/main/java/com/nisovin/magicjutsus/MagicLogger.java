package com.nisovin.magicjutsus;

import java.io.File;
import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuForgetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class MagicLogger implements Listener {

	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private FileWriter writer;
	
	MagicLogger(MagicJutsus plugin) {
		File file = new File(plugin.getDataFolder(), "log-" + System.currentTimeMillis() + ".txt");
		try {
			writer = new FileWriter(file, true);
			MagicJutsus.registerEvents(this);
		} catch (IOException e) {
			MagicJutsus.handleException(e);
		}
	}
	
	void disable() {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				MagicJutsus.handleException(e);
			}
		}
		writer = null;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuLearn(JutsuLearnEvent event) {
		log("LEARN" + 
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; player=" + event.getLearner().getName() + 
				"; loc=" + formatLoc(event.getLearner().getLocation()) +
				"; source=" + event.getSource().name() +
				"; teacher=" + getTeacherName(event.getTeacher()) +
				"; cancelled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuForget(JutsuForgetEvent event) {
		log("FORGET" + 
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; player=" + event.getForgetter().getName() + 
				"; loc=" + formatLoc(event.getForgetter().getLocation()) +
				"; cancelled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuCast(JutsuCastEvent event) {
		log("BEGIN CAST" + 
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; caster=" + event.getCaster().getName() + 
				"; loc=" + formatLoc(event.getCaster().getLocation()) +
				"; state=" + event.getJutsuCastState().name() +
				"; power=" + event.getPower() +
				"; cancelled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuTarget(JutsuTargetEvent event) {
		LivingEntity caster = event.getCaster();
		log("TARGET ENTITY" +
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; caster=" + (caster != null ? caster.getName() : "null") + 
				"; casterLoc=" + (caster != null ? formatLoc(caster.getLocation()) : "null") +
				": target=" + getTargetName(event.getTarget()) + 
				"; targetLoc=" + formatLoc(event.getTarget().getLocation()) +
				"; cancelled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuTargetLocation(JutsuTargetLocationEvent event) {
		log("TARGET LOCATION" +
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; caster=" + event.getCaster().getName() + 
				"; casterLoc=" + formatLoc(event.getCaster().getLocation()) +
				"; targetLoc=" + formatLoc(event.getTargetLocation()) +
				"; cancelled=" + event.isCancelled());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuCasted(JutsuCastedEvent event) {
		log("END CAST" +
				"; jutsu=" + event.getJutsu().getInternalName() +
				"; caster=" + event.getCaster().getName() + 
				"; loc=" + formatLoc(event.getCaster().getLocation()) +
				"; state=" + event.getJutsuCastState().name() +
				"; power=" + event.getPower() +
				"; result=" + event.getPostCastAction().name());
	}
	
	private String formatLoc(Location location) {
		return location.getWorld().getName() + ',' + location.getBlockX() + ',' + location.getBlockY() + ',' + location.getBlockZ();
	}
	
	private String getTargetName(LivingEntity target) {
		if (target instanceof Player) return target.getName();
		return target.getType().name();
	}
	
	private String getTeacherName(Object o) {
		if (o == null) return "none";
		if (o instanceof Player) return "player-" + ((Player)o).getName();
		if (o instanceof Jutsu) return "jutsu-" + ((Jutsu)o).getInternalName();
		if (o instanceof Block) return "block-" + formatLoc(((Block)o).getLocation());
		return o.toString();
	}
	
	private void log(String string) {
		if (writer == null) return;
		try {
			writer.write('[' + dateFormat.format(new Date()) + "] " + string + '\n');
		} catch (IOException e) {
			DebugHandler.debugIOException(e);
		}
	}
	
}
