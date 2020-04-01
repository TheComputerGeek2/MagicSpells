package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;

public class ResourcePackJutsu extends TargetedJutsu {

	private static final int HASH_LENGTH = 20;

	private String url;
	private byte[] hash = null;
	
	public ResourcePackJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		url = getConfigString("url", null);
		String hashString = getConfigString("hash", null);

		if (hashString != null) {
			hash = hexStringToByteArray(hashString);
			if (hash.length != HASH_LENGTH) {
				MagicJutsus.error("Incorrect length for resource pack hash: " + hash.length);
				MagicJutsus.error("Avoiding use of the hash to avoid further problems.");
				hash = null;
			}
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			TargetInfo<Player> target = getTargetedPlayer(player, power);
			Player targetPlayer = target.getTarget();
			if (targetPlayer == null) return noTarget(player);

			sendResourcePack(player);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void sendResourcePack(Player player) {
		if (hash == null) player.setResourcePack(url);
		else player.setResourcePack(url, hash);
	}
	
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

}
