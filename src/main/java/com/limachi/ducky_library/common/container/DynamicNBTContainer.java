package com.limachi.ducky_library.common.container;

import com.google.common.collect.Lists;
import com.limachi.ducky_library.common.container.inventory.DynamicNBTInventory;
import com.limachi.ducky_library.common.container.slots.DisabledSlot;
import com.limachi.ducky_library.common.container.slots.FluidSlot;
import com.limachi.ducky_library.common.references.GUIs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DynamicNBTContainer extends Container {

    protected boolean isClient;
    protected PlayerEntity player;

    private int inventoryStart = -1;
    private int armorStart = -1;
    private int offHandStart = -1;

    protected DynamicNBTInventory nbtInventory;

    protected NonNullList<ItemStack> inventoryItemStacksO = NonNullList.create();
    public final List<FluidSlot> remoteFluidSlots = Lists.newArrayList();
    NonNullList<FluidStack> trackedFluidStacks = NonNullList.create();

    protected CompoundNBT cachedNBT;

    public DynamicNBTContainer(@Nullable ContainerType<? extends DynamicNBTContainer> type, int id, PlayerInventory playerInv, PacketBuffer extraData) {
        super(type, id);
        this.isClient = true;
        this.player = playerInv.player;
        this.open(DynamicNBTInventory.createInPlace(extraData.readCompoundTag()));
    }

    public DynamicNBTContainer(@Nullable ContainerType<? extends DynamicNBTContainer> type, int id, PlayerInventory playerInv, DynamicNBTInventory inventory) {
        super(type, id);
        this.isClient = false;
        this.player = playerInv.player;
        this.open(inventory);
    }

    /**
     * open/reload this container with the new CompoundNBT
    */
    public void open(@Nullable DynamicNBTInventory inventory) {
        if (inventory != null)
            this.nbtInventory = inventory;
        else
            this.nbtInventory = DynamicNBTInventory.createInPlace(new CompoundNBT());
        this.cachedNBT = this.nbtInventory.getDelegatedCompoundNBT().read();
        this.inventorySlots.clear();
        this.inventoryItemStacksO.clear();
        //TODO: reload slots
    }

    protected ArrayList<Integer> disabledSlots() {
        ArrayList<Integer> out = new ArrayList<>();
//        if (connectionType == BaseContainer.ContainerConnectionType.ITEM) //by default, add the item linked to the container to the blacklisted slots
//            out.add(itemSlot);
        return out;
    }

    protected void addPlayerArmor(int x, int y, boolean horizontal) {
        this.armorStart = this.inventorySlots.size();
        if (horizontal) {
            for (int i = 0; i < 4; ++i)
                if (disabledSlots().contains(36 + i))
                    addSlot(new DisabledSlot(player.inventory, 36 + i, x + i * GUIs.ScreenParts.SLOT_SIZE_X, y));
                else
                    addSlot(new Slot(player.inventory, 36 + i, x + i * GUIs.ScreenParts.SLOT_SIZE_X, y));
        } else {
            for (int i = 0; i < 4; ++i)
                if (disabledSlots().contains(36 + i))
                    addSlot(new DisabledSlot(player.inventory, 36 + i, x, y + i * GUIs.ScreenParts.SLOT_SIZE_Y));
                else
                    addSlot(new Slot(player.inventory, 36 + i, x, y + i * GUIs.ScreenParts.SLOT_SIZE_Y));
        }
    }

    protected void addPlayerOffHand(int x, int y) {
        this.offHandStart = this.inventorySlots.size();
        addSlot(disabledSlots().contains(40) ? new DisabledSlot(player.inventory, 40, x, y) : new Slot(player.inventory, 40, x, y));
    }

    protected void addPlayerInventory(int x, int y) {
        this.inventoryStart = this.inventorySlots.size();
        for (int i = 0; i < 9; ++i)
            if (disabledSlots().contains(i))
                addSlot(new DisabledSlot(player.inventory, i, x + i * GUIs.ScreenParts.SLOT_SIZE_X, y + 4 + 3 * GUIs.ScreenParts.SLOT_SIZE_Y));
            else
                addSlot(new Slot(player.inventory, i, x + i * GUIs.ScreenParts.SLOT_SIZE_X, y + 4 + 3 * GUIs.ScreenParts.SLOT_SIZE_Y));
        for (int ty = 0; ty < 3; ++ty)
            for (int tx = 0; tx < 9; ++tx)
                if (disabledSlots().contains(x + y * 9 + 9))
                    addSlot(new DisabledSlot(player.inventory, tx + ty * 9 + 9, x + tx * GUIs.ScreenParts.SLOT_SIZE_X, y + ty * GUIs.ScreenParts.SLOT_SIZE_Y));
                else
                    addSlot(new Slot(player.inventory, tx + ty * 9 + 9, x + tx * GUIs.ScreenParts.SLOT_SIZE_X, y + ty * GUIs.ScreenParts.SLOT_SIZE_Y));
    }

    @Override //overide to replace the original 'inventoryItemStacks' private list out of the equation (because this container might need to reload all the stacks by clearing the list)
    protected Slot addSlot(Slot slotIn) {
        slotIn.slotNumber = this.inventorySlots.size();
        this.inventorySlots.add(slotIn);
        this.inventoryItemStacksO.add(ItemStack.EMPTY);
        if (slotIn instanceof FluidSlot) {
            trackedFluidStacks.add(FluidStack.EMPTY);
            remoteFluidSlots.add((FluidSlot)slotIn);
            return slotIn;
        }
        return slotIn;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        if (slotIn instanceof FluidSlot) return false;
        return super.canMergeSlot(stack, slotIn);
    }

    @Override //do not use the default behavior, there is no int or other data to track except the nbt of this inventory/container
    public void detectAndSendChanges() {
        if (isClient) return; //for now, only sync down (server overides the client)

    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }
}
