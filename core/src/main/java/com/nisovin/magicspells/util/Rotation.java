package com.nisovin.magicspells.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

import org.bukkit.Location;

/**
 * Represents a rotation that can be applied to a {@link Location}.
 */
public final class Rotation {

	private final Angle yaw;
	private final Angle pitch;

	private Rotation(final @NonNull Angle yaw, final @NonNull Angle pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

	/**
	 * Create a new rotation object.
	 *
	 * @param yaw   yaw
	 * @param pitch pitch
	 * @return Created rotation instance.
	 */
	public static Rotation of(final @NonNull Angle yaw, final @NonNull Angle pitch) {
		return new Rotation(yaw, pitch);
	}

	/**
	 * Returns the yaw of this rotation.
	 *
	 * @return yaw
	 */
	public Angle yaw() {
		return this.yaw;
	}

	/**
	 * Returns the pitch of this rotation
	 *
	 * @return pitch
	 */
	public Angle pitch() {
		return this.pitch;
	}

	/**
	 * Applies this rotation to a location.
	 *
	 * @param location the location to be modified
	 * @return the modified location
	 */
	public @NonNull Location apply(final @NonNull Location location) {
		location.setYaw(this.yaw.apply(location.getYaw()));
		location.setPitch(this.pitch.apply(location.getPitch()));
		return location;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Rotation that = (Rotation) o;
		return this.yaw.equals(that.yaw) && this.pitch.equals(that.pitch);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.yaw, this.pitch);
	}

	@Override
	public String toString() {
		return String.format("Rotation{yaw=%s, pitch=%s}", this.yaw, this.pitch);
	}

}
