package com.nisovin.magicspells.spells.instant;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Collections;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

import io.papermc.paper.command.brigadier.CommandSourceStack;

public class EnderchestSpell extends InstantSpell implements TargetedEntitySpell, BlockingSuggestionProvider.Strings<CommandSourceStack> {

	public EnderchestSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (data.hasArgs() && data.args().length == 1 && MagicSpells.getSpellbook(caster).hasAdvancedPerm(internalName)) {
			Player target = Bukkit.getPlayer(data.args()[0]);
			if (target == null) {
				sendMessage(caster, "Invalid player target.");
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			data = data.target(target);
			caster.openInventory(target.getEnderChest());
		} else caster.openInventory(caster.getEnderChest());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.caster() instanceof Player caster) || !(data.target() instanceof Player target))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		caster.openInventory(target.getEnderChest());
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (!(executor instanceof Player player) || !executor.hasPermission(Perm.ADVANCED.getNode(this)))
			return Collections.emptyList();

		return TxtUtil.tabCompletePlayerName(player);
	}

}
