package com.nisovin.magicspells.spells.targeted.cleanse.util;

import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.spells.targeted.cleanse.*;

public class CleanserManager {

	private final List<Class<? extends Cleanser>> cleanserClasses = new ArrayList<>();

	public CleanserManager() {
		initialize();
	}

	public List<Cleanser> createCleansers() {
		List<Cleanser> cleansers = new ArrayList<>();
		try {
			for (Class<? extends Cleanser> clazz : cleanserClasses) {
				cleansers.add(clazz.getDeclaredConstructor().newInstance());
			}
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
		return cleansers;
	}

	public void addCleanser(@NotNull Class<? extends Cleanser> clazz) {
		cleanserClasses.add(clazz);
	}

	private void initialize() {
		addCleanser(BuffSpellCleanser.class);
		addCleanser(DotSpellCleanser.class);
		addCleanser(FireCleanser.class);
		addCleanser(FreezeCleanser.class);
		addCleanser(LevitateSpellCleanser.class);
		addCleanser(LoopSpellCleanser.class);
		addCleanser(OrbitSpellCleanser.class);
		addCleanser(PotionCleanser.class);
		addCleanser(SilenceSpellCleanser.class);
		addCleanser(StunSpellCleanser.class);
		addCleanser(TotemSpellCleanser.class);
		addCleanser(RewindSpellCleanser.class);
	}

}
