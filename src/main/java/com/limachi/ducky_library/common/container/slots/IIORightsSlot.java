package com.limachi.ducky_library.common.container.slots;

import com.limachi.ducky_library.common.utils.Inventories;

public interface IIORightsSlot {
    Inventories.ItemStackIORights getRights();
    void setRights(Inventories.ItemStackIORights rights);

    /**
     * change the rights to a new state (usually cycle the input and output flags)
     */
    void nextRights();
}
