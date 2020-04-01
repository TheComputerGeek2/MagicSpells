package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ConfigReaderUtil;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.util.prompt.ConversationContextUtil;

public class ConversationJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private ConversationFactory conversationFactory;
	
	public ConversationJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		conversationFactory = ConfigReaderUtil.readConversationFactory(getConfigSection("conversation"));
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(livingEntity);

			conversate(targetInfo.getTarget());
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		conversate(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void conversate(LivingEntity target) {
		if (target == null || !(target instanceof Player)) return;
		Conversation c = conversationFactory.buildConversation((Player) target);
		ConversationContextUtil.setconversable(c.getContext(), (Player) target);
		c.begin();
	}

}
