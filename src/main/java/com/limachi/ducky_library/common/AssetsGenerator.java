package com.limachi.ducky_library.common;

import com.limachi.ducky_library.AbstractDuckyMod;
import com.limachi.ducky_library.DuckyLib;
import com.limachi.ducky_library.annotations.GenerateBlockStateFile;
import com.limachi.ducky_library.common.utils.Worlds;
import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.forgespi.language.ModFileScanData;

/**
 * class meant to be used to generate default assets for annotated classes
 * targets:
 * blockstates (simple blockstate, only point to a single model)
 * lang files (en_us, generate item, block and itemgroup names)
 * block models (simple full block)
 */
public class AssetsGenerator {

    private final AbstractDuckyMod mod;

//    private final String datapacksPath = Worlds.getOverWorld().getServer()
//    serverPlayer.getServerWorld().getSaveHandler().getWorldDirectory().getPath() + "\\datapacks"

    public AssetsGenerator(AbstractDuckyMod mod) {
        this.mod = mod;
//        generateBlockStates();
    }

    public void generateBlockStates() {
        for (ModFileScanData.AnnotationData data : DuckyLib.getFilteredAnnotations(GenerateBlockStateFile.class)) {
            if (data.getAnnotationData().get("mod").equals(mod.getModId())) {
                try {
                    String classPath = data.getClassType().getClassName();
                    Class<?> clazz = Class.forName(classPath);
                    Object instance = clazz.newInstance();
                    if (!(instance instanceof Block))
                        continue;
                    Block block = (Block)mod.getRegistries().getRegisteredBlock(clazz);

                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
