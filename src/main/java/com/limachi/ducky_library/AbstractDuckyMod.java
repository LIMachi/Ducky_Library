package com.limachi.ducky_library;

import com.limachi.ducky_library.common.AssetsGenerator;
import com.limachi.ducky_library.common.Registries;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public abstract class AbstractDuckyMod {
    protected Registries registries;
    protected AssetsGenerator assetsGenerator;
    protected final String modID;

    public AbstractDuckyMod(String modID) {
        this.modID = modID;
        registries = new Registries(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    protected void commonSetup(FMLCommonSetupEvent event) { //this event is fired after the registries event, so it is now safe to access the registries class
        assetsGenerator = new AssetsGenerator(this);
    }

    public String getModId() {
        return modID;
    }

    public abstract ItemGroup getCommonItemGroup();

    public Registries getRegistries() {
        return registries;
    }
}
