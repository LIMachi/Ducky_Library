package com.limachi.ducky_library.common.utils;

import com.limachi.ducky_library.DuckyLib;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Worlds {

    public static ServerWorld getOverWorld() { return DuckyLib.getServer() != null ? DuckyLib.getServer().getWorld(World.OVERWORLD) : null; }

    public static ServerWorld getWorld(@Nullable MinecraftServer server, @Nonnull String regName) {
        if (server == null) server = DuckyLib.getServer();
        if (server == null) return null;
        return server.getWorld(stringToWorldRK(regName));
    }

    public static ServerWorld getWorld(@Nullable MinecraftServer server, @Nonnull RegistryKey<World> reg) {
        if (server == null) server = DuckyLib.getServer();
        if (server == null) return null;
        return server.getWorld(reg);
    }

    public static RegistryKey<World> stringToWorldRK(String str) {
        return RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(str));
    }

    public static String worldRKToString(RegistryKey<World> reg) {
        return reg.getLocation().toString();
    }

    private static void teleport(Entity entityIn, ServerWorld worldIn, double x, double y, double z, float yaw, float pitch) { //modified version of TeleportCommand.java: 123: TeleportCommand#teleport(CommandSource source, Entity entityIn, ServerWorld worldIn, double x, double y, double z, Set<SPlayerPositionLookPacket.Flags> relativeList, float yaw, float pitch, @Nullable TeleportCommand.Facing facing) throws CommandSyntaxException
        if (entityIn.removed) return;
        Set<SPlayerPositionLookPacket.Flags> set = EnumSet.noneOf(SPlayerPositionLookPacket.Flags.class);
        set.add(SPlayerPositionLookPacket.Flags.X_ROT);
        set.add(SPlayerPositionLookPacket.Flags.Y_ROT);
        if (entityIn instanceof ServerPlayerEntity) {
            ChunkPos chunkpos = new ChunkPos(new BlockPos(x, y, z));
            worldIn.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, chunkpos, 1, entityIn.getEntityId());
            entityIn.stopRiding();
            if (((ServerPlayerEntity)entityIn).isSleeping())
                ((ServerPlayerEntity)entityIn).stopSleepInBed(true, true);
            if (worldIn == entityIn.world)
                ((ServerPlayerEntity)entityIn).connection.setPlayerLocation(x, y, z, yaw, pitch, set);
            else
                ((ServerPlayerEntity)entityIn).teleport(worldIn, x, y, z, yaw, pitch);
            entityIn.setRotationYawHead(yaw);
        } else {
            float f1 = MathHelper.wrapDegrees(yaw);
            float f = MathHelper.wrapDegrees(pitch);
            f = MathHelper.clamp(f, -90.0F, 90.0F);
            if (worldIn == entityIn.world) {
                entityIn.setLocationAndAngles(x, y, z, f1, f);
                entityIn.setRotationYawHead(f1);
            } else {
                entityIn.detach();
                Entity entity = entityIn;
                entityIn = entityIn.getType().create(worldIn);
                if (entityIn == null)
                    return;
                entityIn.copyDataFromOld(entity);
                entityIn.setLocationAndAngles(x, y, z, f1, f);
                entityIn.setRotationYawHead(f1);
                worldIn.addFromAnotherDimension(entityIn);
                entity.removed = true;
            }
        }
        if (!(entityIn instanceof LivingEntity) || !((LivingEntity)entityIn).isElytraFlying()) {
            entityIn.setMotion(entityIn.getMotion().mul(1.0D, 0.0D, 1.0D));
            entityIn.setOnGround(true);
        }
        if (entityIn instanceof CreatureEntity) {
            ((CreatureEntity)entityIn).getNavigator().clearPath();
        }
    }

    public static void teleportEntity(Entity entity, RegistryKey<World> destType, BlockPos destPos) {
        if (entity == null || entity.world.isRemote()) return;
        ServerWorld world;
        if (destType != null)
            world = entity.getServer().getWorld(destType);
        else
            world = (ServerWorld)entity.getEntityWorld();
        teleport(entity, world, destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5, entity.rotationYaw, entity.rotationPitch);
    }

    public static void teleportEntity(Entity entity, RegistryKey<World> destType, Vector3d vec) {
        teleportEntity(entity, destType, vec.x, vec.y, vec.z);
    }

    public static void teleportEntity(Entity entity, RegistryKey<World> destType, double x, double y, double z) {
        if (entity == null || entity.world.isRemote()) return;
        ServerWorld world;
        if (destType != null)
            world = entity.getServer().getWorld(destType);
        else
            world = (ServerWorld)entity.getEntityWorld();
        teleport(entity, world, x, y, z, entity.rotationYaw, entity.rotationPitch);
    }

    public static Entity getEntityByUUIDInChunk(Chunk chunk, UUID entityId) {
        if (chunk == null || entityId.equals(Constants.NULLID)) return null;
        ClassInheritanceMultiMap<Entity>[] LayeredEntityList = chunk.getEntityLists();
        for (ClassInheritanceMultiMap<Entity> map : LayeredEntityList)
            for (Entity tested : map.getByClass(Entity.class))
                if (tested.getUniqueID().equals(entityId))
                    return tested;
        return null;
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(World world, Vector3d pos, double radius, Class<? extends T> entities) {
        return world.getEntitiesWithinAABB(entities, new AxisAlignedBB(pos.add(-radius, -radius, -radius), pos.add(radius, radius, radius)), e->e.getPosX() * e.getPosX() + e.getPosY() * e.getPosY() + e.getPosZ() * e.getPosZ() <= radius * radius);
    }
}
