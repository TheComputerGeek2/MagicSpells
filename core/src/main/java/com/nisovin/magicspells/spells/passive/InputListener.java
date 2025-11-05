package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.List;
import java.util.EnumSet;
import java.util.function.Predicate;

import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.InputPredicate;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("input")
public class InputListener extends PassiveListener {

	private final Set<InputType> onPress = EnumSet.noneOf(InputType.class);
	private final Set<InputType> onRelease = EnumSet.noneOf(InputType.class);

	private InputPredicate oldInputPredicate;
	private InputPredicate newInputPredicate;

	@Override
	public void initialize(@NotNull String var) {
		MagicSpells.error("PassiveSpell '" + passiveSpell.getInternalName() + "' attempted to create a 'input' trigger using the string format, which it does not support.");
	}

	@Override
	public boolean initialize(@NotNull ConfigurationSection config) {
		List<String> onPressStrings = config.getStringList("on-press");
		for (String typeString : onPressStrings) {
			InputType type;
			try {
				type = InputType.valueOf(typeString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid input type '" + typeString + "' specified in 'on-press' in 'input' trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
				return false;
			}

			onPress.add(type);
		}

		List<String> onReleaseStrings = config.getStringList("on-release");
		for (String typeString : onReleaseStrings) {
			InputType type;
			try {
				type = InputType.valueOf(typeString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid input type '" + typeString + "' specified in 'on-release' in 'input' trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
				return false;
			}

			onRelease.add(type);
		}

		String oldInputString = config.getString("old-input");
		if (oldInputString != null) {
			oldInputPredicate = InputPredicate.fromString(oldInputString);

			if (oldInputPredicate == null) {
				MagicSpells.error("Invalid value '" + oldInputString + "' specified for 'old-input' in 'input' trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
				return false;
			}
		}

		String newInputString = config.getString("new-input");
		if (newInputString != null) {
			newInputPredicate = InputPredicate.fromString(newInputString);

			if (newInputPredicate == null) {
				MagicSpells.error("Invalid value '" + newInputString + "' specified for 'new-input' in 'input' trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
				return false;
			}
		}

		return true;
	}

	@OverridePriority
	@EventHandler
	public void onInput(PlayerInputEvent event) {
		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		Input oldInput = caster.getCurrentInput();
		if (oldInputPredicate != null && !oldInputPredicate.test(oldInput)) return;

		Input newInput = event.getInput();
		if (newInputPredicate != null && !newInputPredicate.test(newInput)) return;

		trigger_check:
		if (!onPress.isEmpty() || !onRelease.isEmpty()) {
			for (InputType type : onPress)
				if (!type.isPressed(oldInput) && type.isPressed(newInput))
					break trigger_check;

			for (InputType type : onRelease)
				if (type.isPressed(oldInput) && !type.isPressed(newInput))
					break trigger_check;

			return;
		}

		passiveSpell.activate(caster);
	}

	private enum InputType {

		FORWARD(Input::isForward),
		BACKWARD(Input::isBackward),
		LEFT(Input::isLeft),
		RIGHT(Input::isRight),
		JUMP(Input::isJump),
		SNEAK(Input::isSneak),
		SPRINT(Input::isSprint);

		private final Predicate<Input> predicate;

		InputType(Predicate<Input> predicate) {
			this.predicate = predicate;
		}

		public boolean isPressed(Input input) {
			return predicate.test(input);
		}

	}

}
