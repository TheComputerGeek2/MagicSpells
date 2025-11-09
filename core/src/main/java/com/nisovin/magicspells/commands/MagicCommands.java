package com.nisovin.magicspells.commands;

import java.util.Map;

import org.incendo.cloud.parser.standard.*;
import org.jetbrains.annotations.NotNull;

import io.leangen.geantyref.TypeToken;
import io.leangen.geantyref.GenericTypeReflector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.mojang.brigadier.arguments.StringArgumentType;

import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.services.type.ConsumerService;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.brigadier.argument.BrigadierMapping;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.brigadier.argument.BrigadierMappings;
import org.incendo.cloud.bukkit.internal.BukkitBrigadierMapper;
import org.incendo.cloud.minecraft.extras.caption.RichVariable;
import org.incendo.cloud.brigadier.argument.ArgumentTypeFactory;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;

import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.commands.parsers.*;
import com.nisovin.magicspells.commands.exceptions.GenericCommandException;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;
import com.nisovin.magicspells.util.Util;

public class MagicCommands {

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		CloudBrigadierManager<CommandSourceStack, ?> brigadierManager = manager.brigadierManager();
		brigadierManager.setNativeNumberSuggestions(true);

		brigadierManager.registerMapping(new TypeToken<SpellCastArgumentsParser>() {}, builder -> builder
			.cloudSuggestions()
			.toConstant(StringArgumentType.greedyString())
		);

		brigadierManager.registerMapping(new TypeToken<VarargsParser<CommandSourceStack, ?>>() {}, builder -> builder
			.cloudSuggestions()
			.toConstant(StringArgumentType.greedyString())
		);

		brigadierManager.registerMapping(new TypeToken<EitherParser<CommandSourceStack, ?, ?>>() {}, builder -> builder
			.cloudSuggestions()
			.to(parser -> {
				BrigadierMappings<CommandSourceStack, ?> mappings = brigadierManager.mappings();

				ArgumentParser<CommandSourceStack, ?> primaryParser = parser.primary().parser();
				Class<? extends ArgumentParser> primaryParserClass = primaryParser.getClass();

				BrigadierMapping<?, ArgumentParser<?, ?>, ?> primaryMapping = mappings.mapping(primaryParserClass);
				if (primaryMapping != null && primaryMapping.mapper() != null)
					return primaryMapping.mapper().apply(primaryParser);

				Map<Class<?>, ArgumentTypeFactory<?>> defaultArgumentFactories = brigadierManager.defaultArgumentTypeFactories();

				ArgumentTypeFactory<?> primaryFactory = defaultArgumentFactories.get(GenericTypeReflector.erase(primaryParserClass));
				if (primaryFactory != null)
					return primaryFactory.create();

				ArgumentParser<?, ?> fallbackParser = parser.fallback().parser();
				Class<? extends ArgumentParser> fallbackParserClass = fallbackParser.getClass();

				BrigadierMapping<?, ArgumentParser<?, ?>, ?> fallbackMapping = mappings.mapping(fallbackParserClass);
				if (fallbackMapping != null && fallbackMapping.mapper() != null)
					return fallbackMapping.mapper().apply(fallbackParser);

				ArgumentTypeFactory<?> fallbackFactory =  defaultArgumentFactories.get(GenericTypeReflector.erase(fallbackParserClass));
				if (fallbackFactory != null)
					return fallbackFactory.create();

				return StringArgumentType.word();
			})
		);

		BukkitBrigadierMapper<CommandSourceStack> brigadierMapper = new BukkitBrigadierMapper<>(MagicSpells.getInstance().getLogger(), brigadierManager);
		brigadierMapper.mapSimpleNMS(new TypeToken<AngleParser<CommandSourceStack>>() {}, "angle");
		brigadierMapper.mapSimpleNMS(new TypeToken<RotationParser<CommandSourceStack>>() {}, "rotation");

		// Less restrictive in terms of valid non-quoted characters compared to the brigadier string type.
		brigadierMapper.mapSimpleNMS(new TypeToken<OwnedSpellParser>() {}, "nbt_path", true);
		brigadierMapper.mapSimpleNMS(new TypeToken<SpellParser<CommandSourceStack>>() {}, "nbt_path", true);

		MinecraftExceptionHandler.create(CommandSourceStack::getSender)
			.defaultHandlers()
			.handler(GenericCommandException.class, (formatter, context) -> {
				GenericCommandException exception = context.exception();
				return Util.getMessageText(Component.text(exception.getMessage()));
			})
			.handler(InvalidCommandArgumentException.class, (formatter, context) -> {
				InvalidCommandArgumentException exception = context.exception();

				return Component.text()
					.color(NamedTextColor.RED)
					.append(context.context().formatCaption(
						formatter,
						StandardCaptionKeys.EXCEPTION_INVALID_ARGUMENT,
						RichVariable.of("cause", Component.text(exception.getMessage(), NamedTextColor.GRAY))
					));
			})
			.registerTo(manager);

		ExceptionController<CommandSourceStack> controller = manager.exceptionController();
		controller.registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(GenericCommandException.class));
		controller.registerHandler(CommandExecutionException.class, ExceptionHandler.unwrappingHandler(GenericCommandException.class));
		controller.registerHandler(CommandExecutionException.class, ExceptionHandler.unwrappingHandler(InvalidCommandArgumentException.class));

		manager.registerCommandPreProcessor(context -> {
			if (!MagicSpells.isLoaded()) {
				CommandSender sender = context.commandContext().sender().getSender();
				sender.sendMessage(Component.text("MagicSpells is not currently loaded.", NamedTextColor.RED));

				ConsumerService.interrupt();
			}
		});

		manager.appendSuggestionMapper(suggestion -> {
			if (!(suggestion instanceof ComponentTooltipSuggestion componentTooltipSuggestion))
				return suggestion;

			return TooltipSuggestion.suggestion(
				suggestion.suggestion(),
				MessageComponentSerializer.message().serializeOrNull(componentTooltipSuggestion.tooltip())
			);
		});

		CastCommands.register(manager);
		DebugCommand.register(manager);
		HelpCommand.register(manager);
		MagicItemCommand.register(manager);
		MagicXpCommand.register(manager);
		ManaCommands.register(manager);
		ProfileReportCommand.register(manager);
		ReloadCommands.register(manager);
		ResetCooldownCommand.register(manager);
		TaskInfoCommand.register(manager);
		UtilCommands.register(manager);
		VariableCommands.register(manager);
	}

}
