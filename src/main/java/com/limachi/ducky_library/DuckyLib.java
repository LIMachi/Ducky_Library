package com.limachi.ducky_library;

import com.limachi.ducky_library.common.Registries;
import com.limachi.example_mod_ducky_lib.items.RubberDucky;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;

@Mod(DuckyLib.MOD_ID)
public class DuckyLib {

    public static final String MOD_ID = "ducky_library";
    public static final Logger LOGGER = LogManager.getLogger();
    public static DuckyLib INSTANCE;

    public DuckyLib() {
        INSTANCE = this;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface onSetup {}

    /**
     * simple method to retrieve all annotations of a given type on all mods
     * simply put: will get all the annotations on a single stream (temporary discarding from which class the annotation comes) before filtering and collecting back the information
     * you still have to test if the annotations are correct (ElementType, containing class, static, etc...)
     */
    public static List<ModFileScanData.AnnotationData> getFilteredAnnotations(Class<?> annotationClass) {
        Type type = Type.getType(annotationClass);
        return ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a-> type.equals(a.getAnnotationType())).collect(Collectors.toList());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        for (ModFileScanData.AnnotationData data : getFilteredAnnotations(onSetup.class))
            if (data.getTargetType() == ElementType.METHOD)
                try {
                    String methodName = data.getMemberName().split("\\(")[0];
                    Class.forName(data.getClassType().getClassName()).getMethod(methodName).invoke(null);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
    }

    /** try by all means to know if the current invocation is on a logical client or logical server */
    public static boolean isServer(@Nullable World world) {
        if (world != null)
            return !world.isRemote();
        return EffectiveSide.get() == LogicalSide.SERVER;
    }

    /** execute the first wrapped callable only on logical client + physical client, and the second wrapped callable on logical server (any physical side) */
    public static <T> T runLogicalSide(@Nullable World world, Supplier<Callable<T>> client, Supplier<Callable<T>> server) {
        if (isServer(world))
            try {
                return server.get().call();
            } catch (Exception e) { return null; }
        else
            return DistExecutor.callWhenOn(Dist.CLIENT, client);
    }

    /** get the local minecraft player (only on client logical and physical side, returns null otherwise) */
    public static PlayerEntity getPlayer() {
        return runLogicalSide(null, ()->()-> Minecraft.getInstance().player, ()->()->null);
    }

    /** try to get the current server we are connected on, return null if we aren't connected (hanging in main menu for example) */
    public static MinecraftServer getServer() { return ServerLifecycleHooks.getCurrentServer(); }

    /**
     * will run the given runnable in X ticks (on the client/server thread depending on witch thread called this method)
     */
    public static <T> void delayedTask(int ticksToWait, Runnable run) {
        runLogicalSide(null,
                ()->()->{com.limachi.ducky_library.client.EventManager.delayedTask(ticksToWait, run); return null;},
                ()->()->{com.limachi.ducky_library.common.EventManager.delayedTask(ticksToWait, run); return null;});
    }

    public static void breakPoint() {
        LOGGER.error("breakpoint reached, please debug this error ASAP");
        int noop = 0; //put a breakpoint there
    }
}
