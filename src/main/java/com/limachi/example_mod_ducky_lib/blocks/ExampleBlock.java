package com.limachi.example_mod_ducky_lib.blocks;

import com.limachi.ducky_library.AbstractDuckyMod;
import com.limachi.ducky_library.annotations.RegisterBlock;
import com.limachi.ducky_library.common.blocks.BaseBlock;
import com.limachi.example_mod_ducky_lib.example;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;

@RegisterBlock(mod = example.MOD_ID)
public class ExampleBlock extends BaseBlock {
    public ExampleBlock(AbstractDuckyMod mod) {
        super(mod, AbstractBlock.Properties.create(Material.ROCK), new Item.Properties().food(Foods.HONEY).group(mod.getCommonItemGroup()));
    }
}
