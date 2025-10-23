package com.nisovin.magicspells.volatilecode.latest;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;

import net.kyori.adventure.text.Component;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.advancement.AdvancementDisplay;

import com.nisovin.magicspells.util.glow.GlowManager;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper;

import net.minecraft.util.ARGB;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
// FIXME
//import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
//import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
//import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;

public class VolatileCodeLatest extends VolatileCodeHandle {

	private final ResourceLocation TOAST_KEY = ResourceLocation.fromNamespaceAndPath("magicspells", "toast_effect");

	private final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES;
	private final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
	private final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;
	private final Method UPDATE_EFFECT_PARTICLES;

	@SuppressWarnings("unchecked")
	public VolatileCodeLatest(VolatileCodeHelper helper) throws Exception {
		super(helper);

		Field dataSharedFlagsIdField = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
		dataSharedFlagsIdField.setAccessible(true);
		DATA_SHARED_FLAGS_ID = (EntityDataAccessor<Byte>) dataSharedFlagsIdField.get(null);

		Class<?> nmsEntityClass = net.minecraft.world.entity.LivingEntity.class;

		Field dataEffectParticlesField = nmsEntityClass.getDeclaredField("DATA_EFFECT_PARTICLES");
		dataEffectParticlesField.setAccessible(true);
		DATA_EFFECT_PARTICLES = (EntityDataAccessor<List<ParticleOptions>>) dataEffectParticlesField.get(null);

		Field dataEffectAmbienceIdField = nmsEntityClass.getDeclaredField("DATA_EFFECT_AMBIENCE_ID");
		dataEffectAmbienceIdField.setAccessible(true);
		DATA_EFFECT_AMBIENCE_ID = (EntityDataAccessor<Boolean>) dataEffectAmbienceIdField.get(null);

		UPDATE_EFFECT_PARTICLES = nmsEntityClass.getDeclaredMethod("updateSynchronizedMobEffectParticles");
		UPDATE_EFFECT_PARTICLES.setAccessible(true);
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, long duration) {
		var nmsEntity = (((CraftLivingEntity) entity)).getHandle();
		SynchedEntityData entityData = nmsEntity.getEntityData();

		entityData.set(
				DATA_EFFECT_PARTICLES,
				List.of(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.opaque(color)))
		);

		entityData.set(DATA_EFFECT_AMBIENCE_ID, false);

		if (duration <= 0) return;
		helper.scheduleDelayedTask(() -> {
			try {
				UPDATE_EFFECT_PARTICLES.invoke(nmsEntity);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, duration);
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		var nmsItem = CraftItemStack.asNMSCopy(item);
		ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(0, 0, slot + 36, nmsItem);

		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		ServerLevel world = ((CraftWorld) target.getWorld()).getHandle();
		var igniter = ((CraftLivingEntity) source).getHandle();

		PrimedTnt tnt = new PrimedTnt(world, target.x(), target.y(), target.z(), igniter);
		CraftTNTPrimed craftTNT = new CraftTNTPrimed((CraftServer) Bukkit.getServer(), tnt);

		return !new ExplosionPrimeEvent(craftTNT, explosionSize, fire).callEvent();
	}

	@Override
	public void playDragonDeathEffect(Location location) {
		EnderDragon dragon = new EnderDragon(EntityType.ENDER_DRAGON, ((CraftWorld) location.getWorld()).getHandle());
		dragon.setPos(location.x(), location.y(), location.z());

		BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		ClientboundAddEntityPacket addMobPacket = new ClientboundAddEntityPacket(dragon, 0, pos);
		ClientboundEntityEventPacket entityEventPacket = new ClientboundEntityEventPacket(dragon, (byte) 3);
		ClientboundRemoveEntitiesPacket removeEntityPacket = new ClientboundRemoveEntitiesPacket(dragon.getId());

		List<Player> players = new ArrayList<>();
		for (Player player : location.getNearbyPlayers(64.0)) {
			players.add(player);
			ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
			nmsPlayer.connection.send(addMobPacket);
			nmsPlayer.connection.send(entityEventPacket);
		}

		helper.scheduleDelayedTask(() -> {
			for (Player player : players) {
				if (!player.isValid()) continue;
				((CraftPlayer) player).getHandle().connection.send(removeEntityPacket);
			}
		}, 200);
	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {
		Vec3 pos = new Vec3(velocity.getX(), velocity.getY(), velocity.getZ());
		ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), pos);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void playHurtSound(LivingEntity entity) {
		var nmsEntity = ((CraftLivingEntity) entity).getHandle();
		var sound = nmsEntity.getHurtSound(nmsEntity.damageSources().generic());

		if (sound == null || nmsEntity.isSilent()) return;
		nmsEntity.level().playSound(
				null,
				nmsEntity.blockPosition(),
				sound,
				nmsEntity.getSoundSource(),
				nmsEntity.getSoundVolume(),
				nmsEntity.getVoicePitch()
		);
	}

	@Override
	public void sendToastEffect(Player receiver, ItemStack icon, AdvancementDisplay.Frame frameType, Component text) {
		var iconNms = CraftItemStack.asNMSCopy(icon);
		var textNms = PaperAdventure.asVanilla(text);
		var description = PaperAdventure.asVanilla(Component.empty());
		AdvancementType frame;
		try {
			frame = AdvancementType.valueOf(frameType.name());
		} catch (IllegalArgumentException ignored) {
			frame = AdvancementType.TASK;
		}

		AdvancementHolder advancement = Advancement.Builder.advancement()
				.display(iconNms, textNms, description, null, frame, true, false, true)
				.addCriterion("impossible", new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance()))
				.build(TOAST_KEY);
		AdvancementProgress progress = new AdvancementProgress();
		progress.update(new AdvancementRequirements(List.of(List.of("impossible"))));
		progress.grantProgress("impossible");

		ServerPlayer player = ((CraftPlayer) receiver).getHandle();
		player.connection.send(new ClientboundUpdateAdvancementsPacket(
				false,
				Collections.singleton(advancement),
				Collections.emptySet(),
				Collections.singletonMap(TOAST_KEY, progress),
				true
		));
		player.connection.send(new ClientboundUpdateAdvancementsPacket(
				false,
				Collections.emptySet(),
				Collections.singleton(TOAST_KEY),
				Collections.emptyMap(),
				true
		));
	}

	@Override
	public void addGameTestMarker(Player player, Location location, int color, String name, int lifetime) {
// FIXME
//		GameTestAddMarkerDebugPayload payload = new GameTestAddMarkerDebugPayload(CraftLocation.toBlockPosition(location), color, name, lifetime);
//		((CraftPlayer) player).getHandle().connection.send(new ClientboundCustomPayloadPacket(payload));
	}

	@Override
	public void clearGameTestMarkers(Player player) {
// FIXME
//		GameTestClearMarkersDebugPayload payload = new GameTestClearMarkersDebugPayload();
//		((CraftPlayer) player).getHandle().connection.send(new ClientboundCustomPayloadPacket(payload));
	}

	@Override
	public byte getEntityMetadata(Entity entity) {
		return ((CraftEntity) entity).getHandle().getEntityData().get(DATA_SHARED_FLAGS_ID);
	}

	@Override
	public Entity getEntityFromId(World world, int id) {
		var entity = ((CraftWorld) world).getHandle().moonrise$getEntityLookup().get(id);
		return entity == null ? null : entity.getBukkitEntity();
	}

	@Override
	public GlowManager getGlowManager() {
		return new VolatileGlowManagerLatest(helper);
	}

}
