package com.limachi.example_mod_ducky_lib;

import com.limachi.ducky_library.AbstractDuckyMod;
import com.limachi.ducky_library.SimpleItemTab;
import com.limachi.example_mod_ducky_lib.items.RubberDucky;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.common.Mod;

@Mod(example.MOD_ID)
public class example extends AbstractDuckyMod {
    public static final String MOD_ID = "example_mod_ducky_lib";
    public ItemGroup ITEM_GROUP = SimpleItemTab.create("tab_" + MOD_ID, getRegistries().getGroupIcon(RubberDucky.class));

    public example() {
        super(MOD_ID);
    }

    @Override
    public ItemGroup getCommonItemGroup() {
        return ITEM_GROUP;
    }
}
