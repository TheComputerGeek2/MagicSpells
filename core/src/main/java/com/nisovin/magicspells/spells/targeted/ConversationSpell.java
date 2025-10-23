package com.nisovin.magicspells.spells.targeted;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

@SuppressWarnings("removal")
public class ConversationSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConversationFactory conversationFactory;

	public ConversationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		conversationFactory = ConfigReaderUtil.readConversationFactory(getConfigSection("conversation"));
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		conversate(info.target(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player)) return noTarget(data);

		conversate(player, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void conversate(Player target, SpellData data) {
		Conversation conversation = conversationFactory
			.withInitialSessionData(Map.of("magicspells.spell_data", data))
			.buildConversation(target);

		conversation.begin();

		playSpellEffects(data);
	}

}
