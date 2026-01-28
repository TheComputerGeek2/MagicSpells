package com.nisovin.magicspells.volatilecode.v1_21_10;

import java.util.*;
import java.lang.invoke.VarHandle;
import java.util.function.Consumer;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;

import net.kyori.adventure.text.Component;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.threadedregions.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler;

import com.nisovin.magicspells.util.glow.GlowManager;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper;

import net.minecraft.util.ARGB;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
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
import org.jetbrains.annotations.NotNull;

public class VolatileCode_v1_21_10 extends VolatileCodeHandle {

	private final ResourceLocation TOAST_KEY = ResourceLocation.fromNamespaceAndPath("magicspells", "toast_effect");

	private final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES;
	private final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
	private final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;
	private final MethodHandle UPDATE_EFFECT_PARTICLES;

	private final Long2ObjectOpenHashMap<List<ScheduledTask>> GLOBAL_REGION_TASKS;
	private final VarHandle CURRENTLY_EXECUTING_HANDLE;
	private final VarHandle ONE_TIME_DELAYED_HANDLE;
	private final VarHandle RUN_HANDLE;

	@SuppressWarnings("unchecked")
	public VolatileCode_v1_21_10(VolatileCodeHelper helper) throws Exception {
		super(helper);

		MethodHandles.Lookup lookup = MethodHandles.lookup();

		Class<?> leClass = net.minecraft.world.entity.LivingEntity.class;
		Class<?> eClass = net.minecraft.world.entity.Entity.class;

		DATA_SHARED_FLAGS_ID = (EntityDataAccessor<@NotNull Byte>) MethodHandles.privateLookupIn(eClass, lookup)
			.findStaticVarHandle(eClass, "DATA_SHARED_FLAGS_ID", EntityDataAccessor.class).get();

		MethodHandles.Lookup leLookup = MethodHandles.privateLookupIn(leClass, lookup);

		DATA_EFFECT_PARTICLES = (EntityDataAccessor<@NotNull List<ParticleOptions>>) leLookup
			.findStaticVarHandle(leClass, "DATA_EFFECT_PARTICLES", EntityDataAccessor.class).get();

		DATA_EFFECT_AMBIENCE_ID = (EntityDataAccessor<@NotNull Boolean>) leLookup
			.findStaticVarHandle(leClass, "DATA_EFFECT_AMBIENCE_ID", EntityDataAccessor.class).get();

		UPDATE_EFFECT_PARTICLES = leLookup.findVirtual(leClass, "updateSynchronizedMobEffectParticles", MethodType.methodType(void.class));

		GLOBAL_REGION_TASKS = (Long2ObjectOpenHashMap<List<ScheduledTask>>) MethodHandles.privateLookupIn(FoliaGlobalRegionScheduler.class, lookup)
			.findVarHandle(FoliaGlobalRegionScheduler.class, "tasksByDeadline", Long2ObjectOpenHashMap.class)
			.get(Bukkit.getGlobalRegionScheduler());

		MethodHandles.Lookup esLookup = MethodHandles.privateLookupIn(EntityScheduler.class, lookup);

		CURRENTLY_EXECUTING_HANDLE = esLookup.findVarHandle(EntityScheduler.class, "currentlyExecuting", ArrayDeque.class);
		ONE_TIME_DELAYED_HANDLE = esLookup.findVarHandle(EntityScheduler.class, "oneTimeDelayed", Long2ObjectOpenHashMap.class);

		Class<?> scheduledTaskClass = esLookup.findClass("io.papermc.paper.threadedregions.EntityScheduler$ScheduledTask");
		RUN_HANDLE = esLookup.findVarHandle(scheduledTaskClass, "run", Consumer.class);
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
			} catch (Throwable e) {
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
		return new VolatileGlowManager_v1_21_10(helper);
	}

	@Override
	public long countGlobalRegionSchedulerTasks() {
		Plugin plugin = helper.getPlugin();

		return GLOBAL_REGION_TASKS.values().stream()
			.flatMap(List::stream)
			.filter(task -> task.getOwningPlugin() == plugin)
			.count();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public long countEntitySchedulerTasks() {
		EntityScheduler.EntitySchedulerTickList entitySchedulerTickList = MinecraftServer.getServer().entitySchedulerTickList;
		EntityScheduler[] schedulers = entitySchedulerTickList.getAllSchedulers();
		Plugin plugin = helper.getPlugin();

		int count = 0;
		for (EntityScheduler scheduler : schedulers) {
			Long2ObjectOpenHashMap<List> oneTimeDelayed = (Long2ObjectOpenHashMap<List>) ONE_TIME_DELAYED_HANDLE.get(scheduler);
			ArrayDeque currentlyExecuting = (ArrayDeque) CURRENTLY_EXECUTING_HANDLE.get(scheduler);

			for (List taskList : oneTimeDelayed.values()) {
				for (Object taskObject : taskList) {
					ScheduledTask task = (ScheduledTask) (Consumer) RUN_HANDLE.get(taskObject);
					if (task.getOwningPlugin() == plugin) count++;
				}
			}

			for (Object taskObject : currentlyExecuting) {
				ScheduledTask task = (ScheduledTask) (Consumer) RUN_HANDLE.get(taskObject);
				if (task.getOwningPlugin() == plugin) count++;
			}
		}

		return count;
	}

}
