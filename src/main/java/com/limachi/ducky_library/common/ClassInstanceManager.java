package com.limachi.ducky_library.common;

import com.limachi.ducky_library.DuckyLib;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;

public class ClassInstanceManager {
    /**
     * keep an instance of this class
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Instanciate {}

    /**
     * inject a known instance of a class into this static field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface InjectInstance {}

    private static final HashMap<Class<?>, Object> instances = new HashMap<>();

    @DuckyLib.onSetup
    public static void scanData() {
        for (ModFileScanData.AnnotationData data : DuckyLib.getFilteredAnnotations(Instanciate.class))
            if (data.getTargetType() == ElementType.TYPE)
                try {
                    Class<?> clazz = Class.forName(data.getClassType().getClassName());
                    instances.put(clazz, clazz.newInstance());
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
        for (ModFileScanData.AnnotationData data : DuckyLib.getFilteredAnnotations(InjectInstance.class))
            if (data.getTargetType() == ElementType.FIELD) {
                try {
                    Class<?> clazz = Class.forName(data.getClassType().getClassName());
                    Field field = clazz.getDeclaredField(data.getMemberName());
                    Object i = instances.get(field.getType());
                    if (i == null) return;
                    field.set(null, i);
                } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
    }

    public static <T> T getInstance(Class<T> clazz) { return (T)instances.get(clazz); }
}
