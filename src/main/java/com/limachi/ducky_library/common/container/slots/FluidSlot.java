package com.limachi.ducky_library.common.container.slots;

import com.limachi.ducky_library.common.capabilities.IFluidHandlerModifiable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class FluidSlot extends Slot {
    protected final IFluidHandlerModifiable fluidHandler;
    protected final int slotIndex;

    public FluidSlot(IFluidHandlerModifiable fluidHandler, int index, int xPosition, int yPosition) {
        super(new Inventory(0), index, xPosition, yPosition);
        this.fluidHandler = fluidHandler;
        this.slotIndex = index;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        LazyOptional<IFluidHandlerItem> cap = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
        if (cap.isPresent()) {
            IFluidHandlerItem handler = cap.orElse(null);
            return fluidHandler.isFluidValid(slotIndex, handler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE));
        }
        return false;
    }

    @Override
    public boolean canTakeStack(PlayerEntity playerIn) { return false; }

    public IFluidHandlerModifiable getFluidHandler() { return fluidHandler; }

    @Override
    public void onSlotChanged() {}

    public FluidStack getFluidStack() { return this.fluidHandler.getFluidInTank(slotIndex); }

    @Override
    public boolean isSameInventory(Slot other) { return other instanceof FluidSlot && ((FluidSlot)other).fluidHandler.equals(fluidHandler); }

    public ItemStack onSlotClick(PlayerEntity player, ClickType clickType) { return ItemStack.EMPTY; }
}
