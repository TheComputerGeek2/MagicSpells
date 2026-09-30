package com.nisovin.magicspells.util.pdc;

import java.util.UUID;
import java.nio.ByteBuffer;

import org.jspecify.annotations.NullMarked;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataAdapterContext;

@NullMarked
public class UUIDTagType implements PersistentDataType<byte[], UUID> {

	public static final UUIDTagType INSTANCE = new UUIDTagType();

	private UUIDTagType() {}

	@Override
	public Class<byte[]> getPrimitiveType() {
		return byte[].class;
	}

	@Override
	public Class<UUID> getComplexType() {
		return UUID.class;
	}

	@Override
	public byte[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 2);
		bb.putLong(complex.getMostSignificantBits());
		bb.putLong(complex.getLeastSignificantBits());
		return bb.array();
	}

	@Override
	public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
		ByteBuffer bb = ByteBuffer.wrap(primitive);
		long firstLong = bb.getLong();
		long secondLong = bb.getLong();
		return new UUID(firstLong, secondLong);
	}

}
