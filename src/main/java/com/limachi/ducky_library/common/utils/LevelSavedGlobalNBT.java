package com.limachi.ducky_library.common.utils;

import com.limachi.ducky_library.DuckyLib;
import com.limachi.ducky_library.common.network.PacketHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

import static net.minecraftforge.eventbus.api.EventPriority.HIGHEST;
import static net.minecraftforge.eventbus.api.EventPriority.LOWEST;

/**
 * this class will try to keep a CompoundNBT saved on the server and sync to the clients (using both worldsaveddata and network messages)
 * the only 3 methods you should use are: LevelSavedGlobalNBT::getInstance(String name), LevelSavedGlobalNBT#getNBT() and LevelSavedGlobalNBT#setNBT(CompoundNBT)
 * all others are public because of compatibility with minecraft/forge but should not be used
 * the returns of getInstance are stored in a global array of weak reference, so we can keep them sync for as long as you use them
 * players to be sync are automatically added and removed via forge event listeners
 * the data is sync and saved when needed (sync to clients with a maximum of 1 tick, saved when the server saves the overworld)
 * sync is only handled from server to client, if you need to sync data from the client to the server, do it yourself
 */
@Mod.EventBusSubscriber
public class LevelSavedGlobalNBT extends WorldSavedData {

    private static final HashSet<PlayerEntity> listeners = new HashSet<>();
    private static CompoundNBT clientNbt = new CompoundNBT();

    private static final ArrayList<WeakReference<LevelSavedGlobalNBT>> trackedNbts = new ArrayList<>();

    protected CompoundNBT nbt;
    protected CompoundNBT cmp;

    public static class LevelSavedGlobalNBTSyncMsg extends PacketHandler.Message {

        String name;
        CompoundNBT nbt;

        public LevelSavedGlobalNBTSyncMsg(PacketBuffer buffer) {
            this.name = buffer.readString();
            this.nbt = buffer.readCompoundTag();
        }

        public LevelSavedGlobalNBTSyncMsg(String name, CompoundNBT nbt) {
            this.name = name;
            this.nbt = nbt;
        }

        @Override
        public void toBytes(PacketBuffer buffer) {
            buffer.writeString(name);
            buffer.writeCompoundTag(nbt);
        }

        @Override
        public void clientWork() {
            clientNbt.put(name, nbt);
        }
    }

    protected LevelSavedGlobalNBTSyncMsg syncMsg() {
        return new LevelSavedGlobalNBTSyncMsg(this.getName(), nbt);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        for (WeakReference<LevelSavedGlobalNBT> ref : trackedNbts) {
            LevelSavedGlobalNBT wsg = ref.get();
            if (wsg != null && !wsg.cmp.equals(wsg.nbt)) {
                LevelSavedGlobalNBTSyncMsg msg = wsg.syncMsg();
                for (PlayerEntity p : listeners)
                    PacketHandler.toClient((ServerPlayerEntity) p, msg);
                wsg.markDirty();
                wsg.cmp = wsg.nbt.copy();
            } else
                trackedNbts.remove(ref);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
        if (DuckyLib.isServer(event.getPlayer().world) && event.getPhase() == LOWEST) {
            listeners.add(event.getPlayer());
            for (WeakReference<LevelSavedGlobalNBT> ref : trackedNbts) {
                LevelSavedGlobalNBT wsg = ref.get();
                if (wsg != null)
                    PacketHandler.toClient((ServerPlayerEntity) event.getPlayer(), wsg.syncMsg());
                else
                    trackedNbts.remove(ref);
            }
        } else if (event.getPhase() == HIGHEST)
            clientNbt = new CompoundNBT();
    }

    @SubscribeEvent
    public static void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!DuckyLib.isServer(event.getPlayer().world) && event.getPhase() == HIGHEST)
            clientNbt = new CompoundNBT();
        else
            listeners.remove(event.getPlayer());
    }

    public LevelSavedGlobalNBT(String mod_id) {
        super(mod_id);
        if (DuckyLib.isServer(null))
            trackedNbts.add(new WeakReference<>(this));
    }

    public static LevelSavedGlobalNBT getInstance(String mod_id) {
        if (DuckyLib.isServer(null)) {
            ServerWorld world = Worlds.getOverWorld();
            return world != null ? world.getSavedData().getOrCreate(() -> new LevelSavedGlobalNBT(mod_id), mod_id) : null;
        } else {
            LevelSavedGlobalNBT out = new LevelSavedGlobalNBT(mod_id);
            out.nbt = clientNbt.getCompound(mod_id);
            return out;
        }
    }

    public CompoundNBT getNbt() { return nbt; }

    public void setNbt(CompoundNBT nbt) { this.nbt = nbt; }

    @Override
    public void read(CompoundNBT nbt) {
        if (!this.nbt.equals(nbt)) {
            LevelSavedGlobalNBTSyncMsg msg = this.syncMsg();
            for (PlayerEntity p : listeners)
                PacketHandler.toClient((ServerPlayerEntity) p, msg);
        }
        this.nbt = nbt;
        this.cmp = nbt.copy();
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        nbt = (CompoundNBT)NBTs.deepMergeNBTInternal(compound, nbt);
        if (nbt == null) nbt = new CompoundNBT();
        if (!nbt.equals(cmp)) {
            LevelSavedGlobalNBTSyncMsg msg = this.syncMsg();
            for (PlayerEntity p : listeners)
                PacketHandler.toClient((ServerPlayerEntity) p, msg);
            cmp = nbt.copy();
        }
        return nbt;
    }
}
