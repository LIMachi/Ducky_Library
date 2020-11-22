package com.limachi.ducky_library.common;

import com.limachi.ducky_library.AbstractDuckyMod;
import com.limachi.ducky_library.DuckyLib;
import com.limachi.ducky_library.StringUtils;
import com.limachi.ducky_library.annotations.RegisterBlock;
import com.limachi.ducky_library.annotations.RegisterItem;
import com.limachi.ducky_library.common.blocks.BaseBlock;
import com.limachi.ducky_library.common.items.BaseItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.function.Supplier;

public class Registries {

    private final AbstractDuckyMod mod;
    private final HashMap<Class<?>, HashMap<Class<?>, Object>> INSTANCES = new HashMap<>(); //the content of this map cannot be guaranteed until all registry event have been processed

    public Registries(AbstractDuckyMod mod) {
        this.mod = mod;
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    public <T, C extends T> T getRegisteredInstance(Class<T> type, Class<C> clazz) { return INSTANCES.containsKey(type) ? (T)INSTANCES.get(type).get(clazz) : null; }

    public <T> T getRegisteredItem(Class<T> clazz) { return INSTANCES.containsKey(Item.class) ? (T)INSTANCES.get(Item.class).get(clazz) : null; }

    public <T> T getRegisteredBlock(Class<T> clazz) { return INSTANCES.containsKey(Block.class) ? (T)INSTANCES.get(Block.class).get(clazz) : null; }

    public <T extends BaseItem> Supplier<Item> getGroupIcon(Class<T> clazz) { return ()->getRegisteredItem(clazz); }

    protected <T, C> void storeRegisteredInstance(Class<T> type, Class<C> clazz, Object value) {
        if (!INSTANCES.containsKey(type))
            INSTANCES.put(type, new HashMap<>());
        INSTANCES.get(type).put(clazz, value);
    }

    @SubscribeEvent
    protected void registerBlocks(RegistryEvent.Register<Block> event) {
        for (ModFileScanData.AnnotationData data : DuckyLib.getFilteredAnnotations(RegisterBlock.class))
            if (data.getAnnotationData().get("mod").equals(mod.getModId())) {
                try {
                    String classPath = data.getClassType().getClassName();
                    String className;
                    if (!data.getAnnotationData().containsKey("value")) {
                        String[] classNames = classPath.split("\\.");
                        className = StringUtils.camelToSnake(classNames[classNames.length - 1]);
                    } else
                        className = (String)data.getAnnotationData().get("value");
                    Class<?> clazz = Class.forName(classPath);
                    Block instance = (Block)clazz.getConstructor(AbstractDuckyMod.class).newInstance(mod);
                    instance.setRegistryName(className);
                    storeRegisteredInstance(Block.class, clazz, instance);
                    event.getRegistry().register(instance);
                    if ((Boolean)data.getAnnotationData().getOrDefault("withItem", true)) {
                        Item item;
                        if (instance instanceof BaseBlock && ((BaseBlock)instance).getItemProperties() != null)
                            item = new BlockItem((Block)getRegisteredBlock(clazz), ((BaseBlock) instance).getItemProperties());
                        else
                            item = new BlockItem((Block)getRegisteredBlock(clazz), new Item.Properties().group(mod.getCommonItemGroup()));
                        item.setRegistryName(className);
                        storeRegisteredInstance(Item.class, clazz, item);
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
    }

    @SubscribeEvent
    protected void registerItems(RegistryEvent.Register<Item> event) {
        for (Object i : INSTANCES.getOrDefault(Item.class, new HashMap<>()).values()) //first, register items that might have been generated by the register block event (automatic block items)
            event.getRegistry().register((Item)i);
        for (ModFileScanData.AnnotationData data : DuckyLib.getFilteredAnnotations(RegisterItem.class))
            if (data.getAnnotationData().get("mod").equals(mod.getModId())) {
                try {
                    String classPath = data.getClassType().getClassName();
                    String className;
                    if (!data.getAnnotationData().containsKey("value")) {
                        String[] classNames = classPath.split("\\.");
                        className = StringUtils.camelToSnake(classNames[classNames.length - 1]);
                    } else
                        className = (String)data.getAnnotationData().get("value");
                    Class<?> clazz = Class.forName(classPath);
                    Object instance = clazz.getConstructor(AbstractDuckyMod.class).newInstance(mod);
                    if (instance instanceof Block) {
                        if (instance instanceof BaseBlock && ((BaseBlock)instance).getItemProperties() != null)
                            instance = new BlockItem((Block)getRegisteredBlock(clazz), ((BaseBlock) instance).getItemProperties());
                        else
                            instance = new BlockItem((Block)getRegisteredBlock(clazz), new Item.Properties().group(mod.getCommonItemGroup()));
                    }
                    if (instance instanceof Item) {
                        ((Item)instance).setRegistryName(className);
                        storeRegisteredInstance(Item.class, clazz, instance);
                        event.getRegistry().register((Item)instance);
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
    }
}