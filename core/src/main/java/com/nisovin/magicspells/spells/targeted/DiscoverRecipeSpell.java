package com.nisovin.magicspells.spells.targeted;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.recipes.RecipeDiscoverListener;

public class DiscoverRecipeSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Boolean> discover;
	private final ConfigData<Boolean> suppressToast;

	private final ConfigData<NamespacedKey> recipe;

	public DiscoverRecipeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		discover = getConfigDataBoolean("discover", true);
		suppressToast = getConfigDataBoolean("suppress-toast", false);

		recipe = getConfigDataNamespacedKey("recipe", null);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);
		return discover(info.target(), info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.caster() instanceof Player target)) return noTarget(data);
		return discover(target, data);
	}

	private CastResult discover(Player target, SpellData data) {
		NamespacedKey recipe = this.recipe.get(data);
		if (recipe == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (discover.get(data)) {
			Runnable op = () -> target.discoverRecipe(recipe);
			if (suppressToast.get(data)) RecipeDiscoverListener.suppressToastWhile(op);
			else op.run();
		}
		else target.undiscoverRecipe(recipe);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
