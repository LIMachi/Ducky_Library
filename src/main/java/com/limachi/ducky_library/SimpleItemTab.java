package com.limachi.ducky_library;

import com.limachi.ducky_library.common.items.BaseItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

public class SimpleItemTab {
    public static ItemGroup create(String registryName, Supplier<Item> icon) {
        return new ItemGroup(ItemGroup.GROUPS.length, registryName) {
            @Override
            public ItemStack createIcon() {
                return new ItemStack(icon.get());
            }
        };
    }

    public static ItemGroup create(AbstractDuckyMod mod, Class<? extends BaseItem> icon) {
        return create("tab_" + mod.getModId(), mod.getRegistries().getGroupIcon(icon));
    }
}
