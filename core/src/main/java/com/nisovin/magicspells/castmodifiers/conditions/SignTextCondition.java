package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

/*
 * Format: side;world,x,y,z,line__1\nline__2
 * "side" is optional and can be "front" (def) or "back".
 * world,x,y,z is optional, "x,y,z" must be integers.
 * "lines" should follow strict MiniMessage format.
 */
@Name("signtext")
public class SignTextCondition extends Condition {

	private static final Pattern FORMAT = Pattern.compile("(?:(?<side>front|back);)?(?:(?<location>[^,]+,-?\\d+,-?\\d+,-?\\d+),)?(?<lines>.+)", Pattern.DOTALL);

	private Side side = Side.FRONT;
	private Location location;
	private final List<String> text = new ArrayList<>();

	@Override
	public boolean initialize(@NotNull String var) {
		Matcher matcher = FORMAT.matcher(var);
		if (!matcher.find()) return false;
		String sideName = matcher.group("side");
		String locationString = matcher.group("location");
		String lines = matcher.group("lines");

		if (sideName != null && sideName.equals("back")) side = Side.BACK;
		if (locationString != null) {
			location = LocationUtil.fromString(locationString);
		}
		for (String line : lines.split("\\\\n|\\n")) {
			text.add(line.replaceAll("__", " "));
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkSignText(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkSignText(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkSignText(location);
	}

	public boolean checkSignText(Location targetedLocation) {
		Location signLocation = location == null ? targetedLocation : location;
		Block block = signLocation.getBlock();
		if (!block.getType().name().contains("SIGN")) return false;

		List<Component> lines = ((Sign) block.getState()).getSide(side).lines();
		for (int i = 0; i < lines.size(); i++) {
			String signLine = Util.getStrictString(lines.get(i));
			String checkLine = text.size() - 1 >= i ? text.get(i) : "";
			if (signLine.equals(checkLine)) continue;
			return false;
		}

		return true;
	}

}
