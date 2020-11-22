package com.limachi.ducky_library.common.items;

import com.limachi.ducky_library.AbstractDuckyMod;
import net.minecraft.item.Item;

public class BaseItem extends Item {

    protected final AbstractDuckyMod mod;

    public BaseItem(AbstractDuckyMod mod, Properties properties) {
        super(properties);
        this.mod = mod;
    }
}
