package com.nisovin.magicspells.spelleffects;

import java.util.HashMap;
import java.util.Map;

public enum EffectPosition {

	/** Can be referenced as: start, startcast **/
	START_CAST(0, "start", "startcast"),
	
	/** Can be referenced as: pos1, position1, caster, actor **/
	CASTER(1, "pos1", "position1", "caster", "actor"),
	
	/** Can be referenced as: pos2, position2, target **/
	TARGET(2, "pos2", "position2", "target"),
	
	/** Can be referenced as: line, trail **/
	TRAIL(3, "line", "trail"),
	
	/** Can be referenced as: disabled
	 *  Used in:
	 *      - SteedSpell
	 *      - ExternalCommandSpell
	 *      - BuffSpell (and all sub classes)
	 *      - DisguiseSpell
	 *	- TotemSpell
	 **/
	DISABLED(4, "disabled"),
	
	/** Can be referenced as: delayed **/
	DELAYED(5, "delayed"),
	
	/** Can be referenced as: special
	 *  Is used differently based upon each spell.
	 *  Behavior is defined in:
	 *  - MenuSpell
	 *  - FlightPathSpell
	 *  - ProjectileSpell
	 *  - PortalSpell
	 *  - ConjureSpell
	 *  - ConjureFireworkSpell
	 *  - ConjureBookSpell
	 *  - BeamSpell
	 *  - EntombSpell
	 *  - ForcebombSpell
	 *  - HomingMissileSpell
	 *  - HomingArrowSpell
	 *  - BombSpell
	 *  - AreaEffectSpell
	 *  - FirenovaSpell
	 *  - ParticleProjectileSpell
	 *  - DrainlifeSpell
	 **/
	SPECIAL(6, "special"),
	
	/** Can be referenced as: buff, active 
	 *  Used in:
	 *      - BuffSpell (and all subclasses)
	 *      - StunSpell
	 **/
	BUFF(7, "buff", "active"),
	
	/** Can be referenced as: orbit
	 *  Used in:
	 *      - BuffSpell (and all subclasses)
	 *      - StunSpell
	 *  	- BeamSpell
	 *  	- ConjureSpell
	 *  	- ConjureBookSpell
	 *  	- ConjureFireworkSpell
	 *  	- FirenovaSpell
	 *  	- FlightPathSpell
	 *  	- ParticleProjectileSpell
	 *  	- MenuSpell
	 *  	- AreaEffectSpell
	 *  	- DrainlifeSpell
	 *  	- EntombSpell
	 *  	- ForcebombSpell
	 *  	- HomingArrowSpell
	 *  	- HomingMissileSpell
	 *  	- OrbitSpell
	 *  	- RewindSpell
	 *  	- TotemSpell
	 **/
	ORBIT(8, "orbit"),
	
	/** Can be referenced as: reverse_line, reverseline, rline **/
	REVERSE_LINE(9, "reverse_line", "reverseline", "rline"),
	
	
	/** May be referenced as: projectile
	 * Some spells may use this to play an effect on projectile entities.
	 * Currently enabled in:
	 *   - ArrowSpell
	 *   - DestroySpell
	 *   - FireballSpell
	 *   - FreezeSpell
	 *   - HomingArrowSpell
	 *   - ItemProjectileSpell
	 *   - ProjectileSpell
	 *   - SpawnTntSpell
	 *   - ThrowBlockSpell
	 *   - VolleySpell
	 *   - WitherSkullSpell
	 *   - Magnetspell
	 **/
	PROJECTILE(10, "projectile"),
	
	/**
	 * May be referenced as: casterprojectile or casterprojectileline
	 * Currently supported effects:
	 *    - effectlibline
	 * Currently enabled in:
	 *    - ArrowSpell
	 *    - DestroySpell
	 *    - FireballSpell
	 *    - FreezeSpell
	 *    - HomingArrowSpell
	 *    - ItemProjectileSpell
	 *    - ProjectileSpell
	 *    - SpawnTntSpell
	 *    - ThrowBlockSpell
	 *    - VolleySpell
	 *    - WitherSkullSpell
	 *    - LevitateSpell
	 */
	DYNAMIC_CASTER_PROJECTILE_LINE(11, "casterprojectile", "casterprojectileline"),
	
	/**
	 * May be referenced as: blockdestroy or blockdestruction
	 * Spells supported in:
	 *     - ThrowBlockSpell
	 *     - SpawnTntSpell
	 *     - MaterializeSpell
	 *     - PulserSpell
	 *     - EntombSpell
	 */
	BLOCK_DESTRUCTION(12, "blockdestroy", "blockdestruction");
	//TODO add this effect position to the WallSpell
	
	private int id;
	private String[] names;
	
	private static Map<String, EffectPosition> nameMap = new HashMap<>();
	private static boolean initialized = false;
	
	EffectPosition(int num, String... names) {
		this.id = num;
		this.names = names;
	}
	
	public int getId() {
		return id;
	}
	
	private static void initializeNameMap() {
		if (nameMap == null) nameMap = new HashMap<>();
		nameMap.clear();
		for (EffectPosition pos: EffectPosition.values()) {
			// Make sure the number id can be mapped
			nameMap.put(pos.id + "", pos);
			
			// For all of the names
			for (String name: pos.names) {
				nameMap.put(name.toLowerCase(), pos);
			}
		}
		initialized = true;
	}
	
	public static EffectPosition getPositionFromString(String pos) {
		if (!initialized) initializeNameMap();
		return nameMap.get(pos.toLowerCase());
	}
	
}
