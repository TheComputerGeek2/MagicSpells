package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.conversations.Prompt;
import org.bukkit.command.CommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MessageBlocker;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ExternalCommandSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private static MessageBlocker messageBlocker;

	private List<String> commandToBlock;
	private List<String> commandToExecute;
	private List<String> commandToExecuteLater;
	private List<String> temporaryPermissions;

	private int commandDelay;

	private boolean temporaryOp;
	private boolean blockChatOutput;
	private boolean requirePlayerTarget;
	private boolean doVariableReplacement;
	private boolean executeAsTargetInstead;
	private boolean executeOnConsoleInstead;
	private boolean useTargetVariablesInstead;

	private String strNoTarget;
	private String strBlockedOutput;
	private String strCantUseCommand;

	private Prompt convoPrompt;
	private ConversationFactory convoFac;

	public ExternalCommandSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		commandToBlock = getConfigStringList("command-to-block", null);
		commandToExecute = getConfigStringList("command-to-execute", null);
		commandToExecuteLater = getConfigStringList("command-to-execute-later", null);
		temporaryPermissions = getConfigStringList("temporary-permissions", null);

		commandDelay = getConfigInt("command-delay", 0);

		temporaryOp = getConfigBoolean("temporary-op", false);
		blockChatOutput = getConfigBoolean("block-chat-output", false);
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		doVariableReplacement = getConfigBoolean("do-variable-replacement", false);
		executeAsTargetInstead = getConfigBoolean("execute-as-target-instead", false);
		executeOnConsoleInstead = getConfigBoolean("execute-on-console-instead", false);
		useTargetVariablesInstead = getConfigBoolean("use-target-variables-instead", false);

		strNoTarget = getConfigString("str-no-target", "No target found.");
		strBlockedOutput = getConfigString("str-blocked-output", "");
		strCantUseCommand = getConfigString("str-cant-use-command", "&4You don't have permission to do that.");

		if (requirePlayerTarget) validTargetList = new ValidTargetList(true, false);
		
		if (blockChatOutput) {
			if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
				if (messageBlocker == null) messageBlocker = new MessageBlocker();
			} else {
				convoPrompt = new StringPrompt() {
					
					@Override
					public String getPromptText(ConversationContext context) {
						return strBlockedOutput;
					}
					
					@Override
					public Prompt acceptInput(ConversationContext context, String input) {
						return Prompt.END_OF_CONVERSATION;
					}
					
				};
				convoFac = new ConversationFactory(MagicSpells.plugin)
					.withModality(true)
					.withFirstPrompt(convoPrompt)
					.withTimeout(1);
			}
		}
	}

	@Override
	public void turnOff() {
		if (messageBlocker == null) return;
		messageBlocker.turnOff();
		messageBlocker = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Player target = null;
			if (requirePlayerTarget) {
				TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power, args);
				if (targetInfo == null) {
					sendMessage(strNoTarget, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				target = targetInfo.getTarget();
			}
			process(caster, target, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void process(CommandSender sender, Player target, String[] args) {
		// Get actual sender
		CommandSender actualSender;
		if (executeAsTargetInstead) actualSender = target;
		else if (executeOnConsoleInstead) actualSender = Bukkit.getConsoleSender();
		else actualSender = sender;
		if (actualSender == null) return;
		
		// Grant permissions and op
		boolean opped = false;
		if (actualSender instanceof Player) {
			if (temporaryPermissions != null) {
				for (String perm : temporaryPermissions) {
					if (actualSender.hasPermission(perm)) continue;
					actualSender.addAttachment(MagicSpells.plugin, perm.trim(), true, 5);
				}
			}
			if (temporaryOp && !actualSender.isOp()) {
				opped = true;
				actualSender.setOp(true);
			}
		}
		
		// Perform commands
		try {
			if (commandToExecute != null && !commandToExecute.isEmpty()) {

				Conversation convo = null;
				if (sender instanceof Player player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer(player);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation(player);
						convo.begin();
					}
				}
				
				int delay = 0;
				Player varOwner;
				if (!useTargetVariablesInstead) varOwner = sender instanceof Player player ? player : null;
				else varOwner = target;

				for (String comm : commandToExecute) {
					if (comm == null || comm.isEmpty()) continue;
					if (doVariableReplacement) comm = MagicSpells.doArgumentAndVariableSubstitution(comm, varOwner, args);
					if (args != null && args.length > 0) {
						for (int i = 0; i < args.length; i++) {
							comm = comm.replace("%" + (i + 1), args[i]);
						}
					}
					if (sender != null) comm = comm.replace("%a", sender.getName());
					if (target != null) comm = comm.replace("%t", target.getName());
					if (comm.startsWith("DELAY ")) {
						String[] split = comm.split(" ");
						delay += Integer.parseInt(split[1]);
					} else if (delay > 0) {
						final CommandSender s = actualSender;
						final String c = comm;
						MagicSpells.scheduleDelayedTask(() -> Bukkit.dispatchCommand(s, c), delay);
					} else {
						Bukkit.dispatchCommand(actualSender, comm);
					}
				}
				if (blockChatOutput && messageBlocker != null && sender instanceof Player player) messageBlocker.removePlayer(player);
				else if (convo != null) convo.abandon();
			}
		} catch (Exception e) {
			// Catch all exceptions to make sure we don't leave someone opped
			e.printStackTrace();
		}
		
		// Deop
		if (opped) actualSender.setOp(false);
		
		// Effects
		if (sender instanceof Player player) {
			if (target != null) playSpellEffects(player, target);
			else playSpellEffects(EffectPosition.CASTER, player);
		} else if (sender instanceof BlockCommandSender commandBlock) {
			playSpellEffects(EffectPosition.CASTER, commandBlock.getBlock().getLocation());
		}
		// Add delayed command
		if (commandToExecuteLater != null && !commandToExecuteLater.isEmpty() && !commandToExecuteLater.get(0).isEmpty()) {
			MagicSpells.scheduleDelayedTask(new DelayedCommand(sender, target), commandDelay);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player player) {
			process(caster, player, MagicSpells.NULL_ARGS);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player player) {
			process(null, player, MagicSpells.NULL_ARGS);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (!requirePlayerTarget) {
			process(sender, null, args);
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer().isOp()) return;
		if (commandToBlock == null) return;
		if (commandToBlock.isEmpty()) return;
		String msg = event.getMessage();
		for (String comm : commandToBlock) {
			comm = comm.trim();
			if (comm.isEmpty()) continue;
			if (!msg.startsWith("/" + commandToBlock)) continue;

			event.setCancelled(true);
			sendMessage(strCantUseCommand, event.getPlayer(), MagicSpells.NULL_ARGS);
			return;
		}
	}
	
	public boolean requiresPlayerTarget() {
		return requirePlayerTarget;
	}
	
	private class DelayedCommand implements Runnable {

		private CommandSender sender;
		private Player target;
		
		DelayedCommand(CommandSender sender, Player target) {
			this.sender = sender;
			this.target = target;
		}
		
		@Override
		public void run() {
			// Get actual sender
			CommandSender actualSender;
			if (executeAsTargetInstead) actualSender = target;
			else if (executeOnConsoleInstead) actualSender = Bukkit.getConsoleSender();
			else actualSender = sender;
			if (actualSender == null) return;
			
			// Grant permissions
			boolean opped = false;
			if (actualSender instanceof Player) {
				if (temporaryPermissions != null) {
					for (String perm : temporaryPermissions) {
						if (actualSender.hasPermission(perm)) continue;
						actualSender.addAttachment(MagicSpells.plugin, perm, true, 5);
					}
				}
				if (temporaryOp && !actualSender.isOp()) {
					opped = true;
					actualSender.setOp(true);
				}
			}
			
			// Run commands
			try {
				Conversation convo = null;
				if (sender instanceof Player player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer(player);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation(player);
						convo.begin();
					}
				}
				for (String comm : commandToExecuteLater) {
					if (comm == null) continue;
					if (comm.isEmpty()) continue;
					if (sender != null) comm = comm.replace("%a", sender.getName());
					if (target != null) comm = comm.replace("%t", target.getName());
					Bukkit.dispatchCommand(actualSender, comm);
				}
				if (blockChatOutput && messageBlocker != null && sender instanceof Player player) messageBlocker.removePlayer(player);
				else if (convo != null) convo.abandon();
			} catch (Exception e) {
				// Catch exceptions to make sure we don't leave someone opped
				e.printStackTrace();
			}
			
			// Deop
			if (opped) actualSender.setOp(false);
			
			// Graphical effect
			if (sender == null) return;
			if (sender instanceof Player player) playSpellEffects(EffectPosition.DISABLED, player);
			else if (sender instanceof BlockCommandSender commandBlock) playSpellEffects(EffectPosition.DISABLED, commandBlock.getBlock().getLocation());
		}
		
	}

}
