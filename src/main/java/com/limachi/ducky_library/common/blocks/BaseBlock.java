package com.limachi.ducky_library.common.blocks;

import com.limachi.ducky_library.AbstractDuckyMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public abstract class BaseBlock extends Block {
    protected final Properties originalProperties;
    protected final Item.Properties itemProperties;
    protected final AbstractDuckyMod mod;

    public BaseBlock(AbstractDuckyMod mod, Properties properties, Item.Properties itemProperties) {
        super(properties);
        this.mod = mod;
        this.originalProperties = properties;
        this.itemProperties = itemProperties;
    }

    public BaseBlock(AbstractDuckyMod mod, Properties properties) {
        super(properties);
        this.mod = mod;
        this.originalProperties = properties;
        this.itemProperties = null;
    }

    public Item.Properties getItemProperties() { return itemProperties; }
}
