package com.nisovin.magicspells.util.messagelistener;

import org.bukkit.entity.Player;
import org.bukkit.conversations.*;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;

/**
 * A Conversation implementation that sends one {@link StringPrompt} with prompt text and blocks chat until abandoned.
 * Prompt message output is overridden to send MiniMessage to players or nothing if prompt text is blank.
 */
@SuppressWarnings("removal")
public class MagicConversation extends Conversation {

	public MagicConversation(@NotNull Conversable forWhom, String strBlockedOutput) {
		super(MagicSpells.getInstance(), forWhom, fromPromptText(strBlockedOutput));
	}

	private static Prompt fromPromptText(String promptText) {
		return new StringPrompt() {

			@NotNull
			@Override
			public String getPromptText(@NotNull ConversationContext context) {
				return promptText;
			}

			@Override
			public Prompt acceptInput(@NotNull ConversationContext context, String input) {
				return Prompt.END_OF_CONVERSATION;
			}

		};
	}

	@Override
	public void outputNextPrompt() {
		if (currentPrompt == null) {
			abandon(new ConversationAbandonedEvent(this));
		} else {
			String message = prefix.getPrefix(context) + currentPrompt.getPromptText(context);

			// Override:
			if (!message.isEmpty()) {
				if (context.getForWhom() instanceof Player player)
					player.sendMessage(Util.getMiniMessage(message));
				else context.getForWhom().sendRawMessage(message);
			}

			if (!currentPrompt.blocksForInput(context)) {
				currentPrompt = currentPrompt.acceptInput(context, null);
				outputNextPrompt();
			}
		}

	}

}
