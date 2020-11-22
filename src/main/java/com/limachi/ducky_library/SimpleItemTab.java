package com.limachi.ducky_library;

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
}
