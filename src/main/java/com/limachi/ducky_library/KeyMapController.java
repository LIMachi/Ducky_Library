package com.limachi.ducky_library;

import com.limachi.ducky_library.common.network.PacketHandler;
import com.limachi.ducky_library.common.network.packets.KeyStateMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KeyMapController {

    public static final String KEY_CATEGORY = "Dimensional Bags";

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        KeyMapController.syncKeyMap(event.getButton(), 0, true, event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        KeyMapController.syncKeyMap(event.getKey(), event.getScanCode(), false, event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT);
    }

    public static final ArrayList<KeyBindingEntry> KEYBINDINGS = new ArrayList<>();

    public static class KeyBindingEntry {
        final private boolean needRegister;
        final private KeyBinding keybinding;
        final private int ordinal;

        protected KeyBindingEntry(int ordinal, boolean needRegister, Supplier<Callable<KeyBinding>> keybinding) {
            this.ordinal = ordinal;
            this.needRegister = needRegister;
            this.keybinding = DistExecutor.callWhenOn(Dist.CLIENT, keybinding);
        }

        public static KeyBindingEntry create(boolean needRegister, Supplier<Callable<KeyBinding>> keybinding) {
            KeyBindingEntry out = new KeyBindingEntry(KEYBINDINGS.size(), needRegister, keybinding);
            KEYBINDINGS.add(out);
            return out;
        }

        public KeyBinding getKeybinding() { return keybinding; }

        public int getOrdinal() { return ordinal; }

        @SuppressWarnings("ConstantConditions")
        public boolean getState(PlayerEntity player) {
            return DuckyLib.runLogicalSide(null,
                    ()-> keybinding::isKeyDown,
                    ()->()->player != null && playerKeyStateMap.getOrDefault(player.getUniqueID(), new boolean[KEYBINDINGS.size()])[ordinal]);
        }

        public void forceKeyState(PlayerEntity player, boolean state) {
            DuckyLib.runLogicalSide(player != null ? player.world : null, ()->()->{
                setLocalKeyState(ordinal, state);
                KEYBINDINGS.get(ordinal).keybinding.setPressed(state);
                return null;
            }, ()->()->{
                PacketHandler.toClient((ServerPlayerEntity)player, new KeyStateMsg(ordinal, state));
                return null;
            });
        }
    }

    public static void registerKeybindings() {
        for (KeyBindingEntry entry : KEYBINDINGS)
            if (entry.needRegister)
                ClientRegistry.registerKeyBinding(entry.keybinding);
    }

    public static int keybindCount() { return KEYBINDINGS.size(); }
    protected static boolean[] local_key_map = new boolean[keybindCount()];
    public static void setLocalKeyState(int ordinal, boolean state) {
        if (ordinal >= 0 && ordinal < keybindCount()) {
            if (local_key_map.length < keybindCount()) {
                boolean[] tmp = new boolean[keybindCount()];
                for (int i = 0; i < local_key_map.length; ++i)
                    tmp[i] = local_key_map[i];
                local_key_map = tmp;
            }
            local_key_map[ordinal] = state;
        }
    }
    public static boolean getLocalKeyState(int ordinal) {
        return ordinal >= 0 && ordinal < local_key_map.length && local_key_map[ordinal];
    }

    private static final Map<UUID, boolean[]> playerKeyStateMap = new HashMap<>(); //only used server side

    @OnlyIn(Dist.CLIENT)
    public static void syncKeyMap(int key, int scan, boolean mouse, boolean state) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null)
            return;
        for (int i = 0; i < keybindCount(); ++i) {
            if (/*TRACKED_KEYBINDS[i].getKeyConflictContext().isActive() &&*/ mouse ? KEYBINDINGS.get(i).getKeybinding().matchesMouseKey(key) : KEYBINDINGS.get(i).getKeybinding().matchesKey(key, scan)) {
                if (state != getLocalKeyState(i)) {
                    setLocalKeyState(i, state);
                    PacketHandler.toServer(new KeyStateMsg(i, state));
                }
                return;
            }
        }
    }

    public static class KeyMapChangedEvent extends Event {
        private final PlayerEntity player;
        private final boolean[] keys;
        private final boolean[] previousKeys;

        KeyMapChangedEvent(PlayerEntity player, boolean[] keys, boolean[] previousKeys) {
            this.player = player;
            this.keys = keys;
            this.previousKeys = previousKeys;
        }

        public PlayerEntity getPlayer() { return player; }

        public boolean[] getKeys() { return keys; }

        public boolean[] getPreviousKeys() { return previousKeys; }

        public boolean[] getChangedKeys() {
            boolean[] out = new boolean[Math.max(keys.length, previousKeys.length)];
            for (int i = 0; i < out.length; ++i)
                out[i] = (i < keys.length && keys[i]) != (i < previousKeys.length && previousKeys[i]);
            return out;
        }
    }

    public static void syncKeyMapMsg(ServerPlayerEntity player, int key, boolean state) {
        boolean[] previousKeys = playerKeyStateMap.getOrDefault(player.getUniqueID(), new boolean[keybindCount()]);
        boolean[] keys = previousKeys.clone();
        keys[key] = state;
        playerKeyStateMap.put(player.getUniqueID(), keys);
        MinecraftForge.EVENT_BUS.post(new KeyMapChangedEvent(player, keys, previousKeys));
    }
}
