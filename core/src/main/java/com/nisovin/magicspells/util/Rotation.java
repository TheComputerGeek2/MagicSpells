package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;

/**
 * Represents a rotation that can be applied to a {@link Location}.
 */
public record Rotation(Angle yaw, Angle pitch) {

	/**
	 * Create a new rotation object.
	 *
	 * @param yaw   yaw
	 * @param pitch pitch
	 * @return Created rotation instance.
	 */
	public static Rotation of(final @NotNull Angle yaw, final @NotNull Angle pitch) {
		return new Rotation(yaw, pitch);
	}

	/**
	 * Applies this rotation to a location.
	 *
	 * @param location the location to be modified
	 * @return the modified location
	 */
	public @NotNull Location apply(final @NotNull Location location) {
		location.setYaw(this.yaw.apply(location.getYaw()));
		location.setPitch(this.pitch.apply(location.getPitch()));
		return location;
	}

}
