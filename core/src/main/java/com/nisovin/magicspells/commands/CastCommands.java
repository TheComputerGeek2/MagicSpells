package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

import io.leangen.geantyref.TypeToken;

import net.kyori.adventure.text.Component;

import org.incendo.cloud.Command;
import org.incendo.cloud.type.Either;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.EitherParser;
import org.incendo.cloud.paper.parser.KeyedWorldParser;
import org.incendo.cloud.bukkit.data.SingleEntitySelector;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.bukkit.parser.selector.SingleEntitySelectorParser;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Rotation;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;

import org.incendo.cloud.component.CommandComponent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.commands.parsers.RotationParser;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;
import com.nisovin.magicspells.commands.parsers.SpellCastArgumentsParser;
import com.nisovin.magicspells.commands.exceptions.GenericCommandException;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class CastCommands {

	public static final CloudKey<String[]> SPELL_CAST_ARGUMENTS_KEY = CloudKey.of("cast arguments", String[].class);
	public static final CloudKey<SingleEntitySelector> TARGET_ENTITY_KEY = CloudKey.of("entity", SingleEntitySelector.class);
	public static final CloudKey<Either<World, World>> WORLD_KEY = CloudKey.of("world", new TypeToken<>() {});
	public static final CloudKey<Location> TARGET_LOCATION_KEY = CloudKey.of("location", Location.class);
	public static final CloudKey<Rotation> TARGET_ROTATION_KEY = CloudKey.of("rotation", Rotation.class);
	public static final CloudKey<Spell> SPELL_KEY = CloudKey.of("spell", Spell.class);

	public static final CommandFlag<Float> POWER_FLAG = CommandFlag.builder("power")
		.withComponent(FloatParser.floatParser())
		.withDescription(Description.of("Specify the power of the casted spell."))
		.withPermission(Perm.COMMAND_CAST_POWER)
		.withAliases("p")
		.build();

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		Command.Builder<CommandSourceStack> base = manager
			.commandBuilder("ms", "magicspells")
			.literal("cast")
			.flag(POWER_FLAG);

		var spellComponent = CommandComponent.<CommandSourceStack, Spell>builder()
			.key(SPELL_KEY)
			.parser(SpellParser.spellParser())
			.description(Description.of("The spell to cast."))
			.required()
			.build();

		var castArgumentsComponent = CommandComponent.<CommandSourceStack, String[]>builder()
			.key(SPELL_CAST_ARGUMENTS_KEY)
			.parser(SpellCastArgumentsParser.spellCastArgumentsParser())
			.description(Description.of("Arguments to pass to the spell when casted."))
			.optional()
			.build();

		Command<CommandSourceStack> castSelfCommand = base
			.literal("self")
			.required(SPELL_KEY, OwnedSpellParser.ownedSpellParser(), Description.of("The spell to cast."))
			.argument(castArgumentsComponent)
			.commandDescription(Description.of("Cast a spell."))
			.permission(Perm.COMMAND_CAST_SELF)
			.handler(CastCommands::onCastSelf)
			.build();

		manager.command(castSelfCommand);

		manager.command(manager.commandBuilder("c", "cast")
			.commandDescription(Description.of("Cast a spell."))
			.proxies(castSelfCommand)
		);

		Command<CommandSourceStack> castAsCommand = base
			.literal("as")
			.required(
				TARGET_ENTITY_KEY,
				SingleEntitySelectorParser.singleEntitySelectorParser(),
				Description.of("The entity to cast the spell as.")
			)
			.argument(spellComponent)
			.argument(castArgumentsComponent)
			.commandDescription(Description.of("Force an entity to cast a spell."))
			.permission(Perm.COMMAND_CAST_AS)
			.handler(CastCommands::onCastAs)
			.build();

		manager.command(castAsCommand);

		// Legacy alias for /ms cast as.
		manager.command(manager.commandBuilder("c", "cast")
			.literal("forcecast")
			.meta(HelpCommand.FILTER_FROM_HELP, true)
			.proxies(castAsCommand)
		);

		manager.command(base
			.literal("on")
			.required(
				TARGET_ENTITY_KEY,
				SingleEntitySelectorParser.singleEntitySelectorParser(),
				Description.of("The entity to cast the spell on.")
			)
			.argument(spellComponent)
			.argument(castArgumentsComponent)
			.commandDescription(Description.of("Cast a spell on an entity."))
			.permission(Perm.COMMAND_CAST_ON)
			.handler(CastCommands::onCastOn)
		);

		manager.command(base
			.literal("at")
			.argument(spellComponent)
			.required(
				WORLD_KEY,
				EitherParser.eitherParser(KeyedWorldParser.keyedWorldParser(), WorldParser.worldParser()),
				Description.of("The world the spell will be casted in.")
			)
			.required(
				TARGET_LOCATION_KEY,
				LocationParser.locationParser(),
				Description.of("The coordinates the spell will be casted at, formatted as <x> <y> <z>.")
			)
			.optional(
				TARGET_ROTATION_KEY,
				RotationParser.rotationParser(),
				Description.of("The orientation the spell will be casted with, formatted as <yaw> <pitch>.")
			)
			.argument(castArgumentsComponent)
			.commandDescription(Description.of("Cast a spell at a location."))
			.permission(Perm.COMMAND_CAST_AT)
			.handler(CastCommands::onCastAt)
		);
	}

	private static void onCastSelf(CommandContext<CommandSourceStack> context) {
		String[] arguments = context.getOrDefault(SPELL_CAST_ARGUMENTS_KEY, new String[0]);
		float power = context.flags().getValue(POWER_FLAG).orElse(1f);
		Spell spell = context.get(SPELL_KEY);

		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getSender();
		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), sender);

		if (executor instanceof LivingEntity caster) {
			spell.hardCast(new SpellData(caster, power, arguments));
			return;
		}

		if (executor instanceof ConsoleCommandSender console) {
			if (spell.castFromConsole(console, arguments)) sender.sendMessage("Spell casted!");
			else sender.sendMessage("Spell cast failed.");

			return;
		}

		throw new GenericCommandException("Cannot cast spell.");
	}

	private static void onCastAs(CommandContext<CommandSourceStack> context) {
		String[] arguments = context.getOrDefault(SPELL_CAST_ARGUMENTS_KEY, new String[0]);
		SingleEntitySelector selector = context.get(TARGET_ENTITY_KEY);
		float power = context.flags().getValue(POWER_FLAG).orElse(1f);
		Spell spell = context.get(SPELL_KEY);

		Entity entity = selector.single();
		if (!(entity instanceof LivingEntity caster))
			throw new InvalidCommandArgumentException("Target is not a living entity");

		spell.hardCast(new SpellData(caster, power, arguments));
	}

	private static void onCastOn(CommandContext<CommandSourceStack> context) {
		SingleEntitySelector selector = context.get(TARGET_ENTITY_KEY);
		String[] arguments = context.getOrDefault(SPELL_CAST_ARGUMENTS_KEY, new String[0]);
		float power = context.flags().getValue(POWER_FLAG).orElse(1f);
		Spell spell = context.get(SPELL_KEY);

		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getSender();
		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), sender);

		LivingEntity caster = executor instanceof LivingEntity le ? le : null;

		Entity entity = selector.single();
		if (!(entity instanceof LivingEntity target))
			throw new InvalidCommandArgumentException("Target is not a living entity");

		if (!(spell instanceof TargetedEntitySpell targetedEntitySpell))
			throw new InvalidCommandArgumentException("Spell is not a targeted entity spell");

		CastResult result = targetedEntitySpell.castAtEntity(new SpellData(caster, target, power, arguments));
		if (result.action() == Spell.PostCastAction.ALREADY_HANDLED)
			sender.sendMessage(Util.getMessageText(Component.text("Spell cast failed.")));
	}

	private static void onCastAt(CommandContext<CommandSourceStack> context) {
		String[] arguments = context.getOrDefault(SPELL_CAST_ARGUMENTS_KEY, new String[0]);
		float power = context.flags().getValue(POWER_FLAG).orElse(1f);
		Spell spell = context.get(SPELL_KEY);

		World world = context.get(WORLD_KEY).primaryOrMapFallback(Function.identity());
		Location location = context.get(TARGET_LOCATION_KEY);
		location.setWorld(world);

		Rotation rotation = context.getOrDefault(TARGET_ROTATION_KEY, null);
		if (rotation != null) rotation.apply(location);

		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getSender();
		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), sender);

		LivingEntity caster = executor instanceof LivingEntity le ? le : null;

		if (!(spell instanceof TargetedLocationSpell targetedLocationSpell))
			throw new InvalidCommandArgumentException("Spell is not a targeted location spell");

		CastResult result = targetedLocationSpell.castAtLocation(new SpellData(caster, location, power, arguments));
		if (result.action() == Spell.PostCastAction.ALREADY_HANDLED)
			sender.sendMessage(Util.getMessageText(Component.text("Spell cast failed.")));
	}

}
