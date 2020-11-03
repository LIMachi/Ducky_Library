package com.limachi.ducky_library.common.capabilities;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public interface IFluidHandlerModifiable extends IFluidHandler {
    void setFluidInTank(int tank, FluidStack fluid);
}
