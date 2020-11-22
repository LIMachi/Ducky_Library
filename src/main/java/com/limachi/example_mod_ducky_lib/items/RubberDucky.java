package com.limachi.example_mod_ducky_lib.items;

import com.limachi.ducky_library.AbstractDuckyMod;
import com.limachi.ducky_library.annotations.RegisterItem;
import com.limachi.ducky_library.common.items.BaseItem;
import com.limachi.example_mod_ducky_lib.example;
import net.minecraft.item.Item;

@RegisterItem(mod = example.MOD_ID)
public class RubberDucky extends BaseItem {
    public RubberDucky(AbstractDuckyMod mod) {
        super(mod, new Item.Properties().group(mod.getCommonItemGroup()));
    }
}
