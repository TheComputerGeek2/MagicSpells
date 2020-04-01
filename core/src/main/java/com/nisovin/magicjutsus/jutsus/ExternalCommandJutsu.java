package com.nisovin.magicjutsus.jutsus;

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

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.MessageBlocker;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class ExternalCommandJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
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

	public ExternalCommandJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

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
				convoFac = new ConversationFactory(MagicJutsus.plugin)
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
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Player target = null;
			if (requirePlayerTarget) {
				TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
				if (targetInfo == null) {
					sendMessage(strNoTarget, livingEntity, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				target = targetInfo.getTarget();
			}
			process(livingEntity, target, args);
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
					actualSender.addAttachment(MagicJutsus.plugin, perm.trim(), true, 5);
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
				if (sender instanceof Player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer((Player) sender);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation((Player) sender);
						convo.begin();
					}
				}
				
				int delay = 0;
				Player varOwner;
				if (!useTargetVariablesInstead) varOwner = sender instanceof Player ? (Player) sender : null;
				else varOwner = target;

				for (String comm : commandToExecute) {
					if (comm == null || comm.isEmpty()) continue;
					if (doVariableReplacement) comm = MagicJutsus.doArgumentAndVariableSubstitution(comm, varOwner, args);
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
						MagicJutsus.scheduleDelayedTask(() -> Bukkit.dispatchCommand(s, c), delay);
					} else {
						Bukkit.dispatchCommand(actualSender, comm);
					}
				}
				if (blockChatOutput && messageBlocker != null && sender instanceof Player) messageBlocker.removePlayer((Player) sender);
				else if (convo != null) convo.abandon();
			}
		} catch (Exception e) {
			// Catch all exceptions to make sure we don't leave someone opped
			e.printStackTrace();
		}
		
		// Deop
		if (opped) actualSender.setOp(false);
		
		// Effects
		if (sender instanceof Player) {
			if (target != null) playJutsuEffects((Player) sender, target);
			else playJutsuEffects(EffectPosition.CASTER, (Player) sender);
		} else if (sender instanceof BlockCommandSender) {
			playJutsuEffects(EffectPosition.CASTER, ((BlockCommandSender) sender).getBlock().getLocation());
		}
		// Add delayed command
		if (commandToExecuteLater != null && !commandToExecuteLater.isEmpty() && !commandToExecuteLater.get(0).isEmpty()) {
			MagicJutsus.scheduleDelayedTask(new DelayedCommand(sender, target), commandDelay);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player) {
			process(caster, (Player) target, MagicJutsus.NULL_ARGS);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player) {
			process(null, (Player) target, MagicJutsus.NULL_ARGS);
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
			sendMessage(strCantUseCommand, event.getPlayer(), MagicJutsus.NULL_ARGS);
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
						actualSender.addAttachment(MagicJutsus.plugin, perm, true, 5);
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
				if (sender instanceof Player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer((Player) sender);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation((Player) sender);
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
				if (blockChatOutput && messageBlocker != null && sender instanceof Player) messageBlocker.removePlayer((Player) sender);
				else if (convo != null) convo.abandon();
			} catch (Exception e) {
				// Catch exceptions to make sure we don't leave someone opped
				e.printStackTrace();
			}
			
			// Deop
			if (opped) actualSender.setOp(false);
			
			// Graphical effect
			if (sender == null) return;
			if (sender instanceof Player) playJutsuEffects(EffectPosition.DISABLED, (Player) sender);
			else if (sender instanceof BlockCommandSender) playJutsuEffects(EffectPosition.DISABLED, ((BlockCommandSender) sender).getBlock().getLocation());
		}
		
	}

}
