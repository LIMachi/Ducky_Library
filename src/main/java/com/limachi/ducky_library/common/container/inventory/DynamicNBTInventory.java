package com.limachi.ducky_library.common.container.inventory;

import com.limachi.ducky_library.common.utils.Inventories;
import com.limachi.ducky_library.common.utils.NBTs;
import com.limachi.ducky_library.interfaces.IMarkDirty;
import javafx.util.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * universal inventory meant to be stored and sync via nbt, can store both itemstacks and fluidstacks (and might also be made compatible with other types of storages, like mekanism gas, in the future)
 */
public class DynamicNBTInventory implements Inventories.IFormatAwareItemHandler, IMarkDirty, IFluidHandler {

    public static class ShulkerBoxItemHandler extends DynamicNBTInventory {

        public ShulkerBoxItemHandler(@Nonnull ItemStack shulkerBox, @Nullable IMarkDirty makeDirty) {
            super(()->{
                if (shulkerBox.getTag() == null) {
                    shulkerBox.setTag(new CompoundNBT());
                }
                return shulkerBox.getTag().getCompound("BlockEntityTag");
            }, nbt->{
                shulkerBox.getTag().put("BlockEntityTag", nbt);
                if (makeDirty != null)
                    makeDirty.markDirty();
            }, nbt -> {
                return shulkerBox.getTag().getCompound("BlockEntityTag");
            });
        }
    }

    public static final int DEFAULT_SIZE = 27;
    protected FluidStack[] fluids;
    protected Inventories.TankIORights[] fluidRights;
    protected ItemStack[] stacks;
    protected Inventories.ItemStackIORights[] rights;
    protected final NBTs.DelegatedCompoundNBT delegatedCompoundNBT;
    protected boolean wasFormatted;
    protected Inventories.ItemHandlerFormat format = Inventories.ItemHandlerFormat.CHEST;
    protected int columns = 9;
    protected int rows = 3;
    protected boolean hadRights;
    protected boolean hadFluids;
    protected boolean hadFluidRights;

    /**
     * this class should be compatible with the shulker box, but can also be used for any nbt based itemprovider with the ability to change this nbt
     * @param read: a lambda to read the nbt, the supplied value can be null (treated as an uninitialized inventory of `DEFAULT_SIZE` (27) slots, like a single chest/shulkerbox)
     * @param write: the parameter will contain the serialized inventory, merged on the compound returned by read (for easier assignation)
     */
    public DynamicNBTInventory(@Nonnull Supplier<CompoundNBT> read, @Nonnull Consumer<CompoundNBT> write, Function<CompoundNBT, CompoundNBT> resync) {
        this.delegatedCompoundNBT = new NBTs.DelegatedCompoundNBT(read, write, resync);
        this.reload();
    }

    @Override
    public int getTanks() {
        return fluids.length;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank < 0 || tank >= fluids.length ? FluidStack.EMPTY : fluids[tank];
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank < 0 || tank >= fluids.length ? 0 : fluidRights[tank].maxStack;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return tank >= 0 && tank < fluids.length && (fluids[tank].getAmount() > 0 ? stack.isFluidEqual(fluids[tank]) : fluidRights[tank].isFluidValid(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return null;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return null;
    }

    /**
     * reloads the inventory from the nbt, used on creation and to manually update if the delegated nbt was changed outside of this class
     */
    public void reload() {
        CompoundNBT nbt = delegatedCompoundNBT.read();
        if (nbt == null) nbt = new CompoundNBT();
        int size = DEFAULT_SIZE;
        if (wasFormatted = nbt.keySet().contains("Size"))
            size = nbt.getInt("Size");
        stacks = new ItemStack[size];
        rights = new Inventories.ItemStackIORights[size];
        for (int i = 0; i < size; ++i) {
            stacks[i] = ItemStack.EMPTY;
            rights[i] = new Inventories.ItemStackIORights();
        }
        if (wasFormatted && nbt.keySet().contains("Columns"))
            columns = nbt.getInt("Columns");
        if (wasFormatted && nbt.keySet().contains("Rows"))
            rows = nbt.getInt("Rows");
        if (wasFormatted && nbt.keySet().contains("Format"))
            format = Inventories.ItemHandlerFormat.values()[nbt.getInt("Format")];
        ListNBT list = nbt.getList("Items", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundNBT entry = list.getCompound(i);
            stacks[entry.getInt("Slot")] = ItemStack.read(entry);
        }
        if (hadRights = nbt.keySet().contains("Rights")) {
            list = nbt.getList("Rights", 10);
            for (int i = 0; i < list.size(); ++i) {
                CompoundNBT entry = list.getCompound(i);
                rights[entry.getInt("Slot")].readNBT(entry);
            }
        }
        size = nbt.getInt("SizeFluids");
        if (hadFluids = size > 0) {
            fluids = new FluidStack[size];
            fluidRights = new Inventories.TankIORights[size];
            if (nbt.keySet().contains("Fluids")) {
                list = nbt.getList("Fluids", 10);
                for (int i = 0; i < list.size(); ++i) {
                    CompoundNBT entry = list.getCompound(i);
                    fluids[entry.getInt("Slot")] = FluidStack.loadFluidStackFromNBT(entry);
                }
            }
            if (nbt.keySet().contains("FluidRights")) {
                list = nbt.getList("Fluids", 10);
                for (int i = 0; i < list.size(); ++i)
                    fluidRights[i].readNBT(list.getCompound(i));
            }
        }
    }

    public NBTs.DelegatedCompoundNBT getDelegatedCompoundNBT() { return delegatedCompoundNBT; }

    @Override
    public int getRows() { return rows; }

    @Override
    public int getColumns() { return columns; }

    @Override
    public Inventories.ItemHandlerFormat getFormat() { return format; }

    @Override
    public void setRows(int rows) {
        this.rows = rows;
        markDirty();
    }

    @Override
    public void setColumns(int columns) {
        this.columns = columns;
        markDirty();
    }

    @Override
    public void setFormat(Inventories.ItemHandlerFormat format) {
        this.format = format;
        markDirty();
    }

    /**
     * simple helper constructor that will link a compound to an item handler (the writes will use a nbt merge)
     * @param nbt the compound to be used as inventory (must already have been initialized with at least an int in the key 'Size')
     * @return an ItemHandler linked to the given nbt
     */
    public static DynamicNBTInventory createInPlace(CompoundNBT nbt) {
        return new DynamicNBTInventory(()->nbt, w-> NBTs.deepMergeNBTInternal(nbt, w), o->NBTs.replace(nbt, o));
    }

    /**
     * same as createInPlace(CompoundNBT nbt), but use a weak reference instead, so if the nbt is no longer available, the inventory will no longer be modifiable
     * @param nbtHolder
     * @return
     */
    public static DynamicNBTInventory createInPlace(WeakReference<CompoundNBT> nbtHolder) {
        return new DynamicNBTInventory(nbtHolder::get, w-> NBTs.deepMergeNBTInternal(nbtHolder.get(), w), o->NBTs.replace(nbtHolder.get(), o));
    }

    public static DynamicNBTInventory createInPlace(int size) {
        return createInPlace(NBTs.newCompound("Size", size));
    }

    public DynamicNBTInventory resize(int newSize) {
        if (newSize == stacks.length) return this;
        ItemStack[] tmp = new ItemStack[newSize];
        Inventories.ItemStackIORights[] tmpRights = new Inventories.ItemStackIORights[newSize];
        if (newSize > stacks.length) {
            int i = 0;
            for (; i < stacks.length; ++i) {
                tmp[i] = stacks[i];
                tmpRights[i] = rights[i];
            }
            for (; i < newSize; ++i) {
                tmp[i] = ItemStack.EMPTY;
                tmpRights[i] = new Inventories.ItemStackIORights();
            }
        } else
            for (int i = 0; i < newSize; ++i) {
                tmp[i] = stacks[i];
                tmpRights[i] = rights[i];
            }
        stacks = tmp;
        rights = tmpRights;
        markDirty();
        return this;
    }

    @Override
    public void markDirty() {
        CompoundNBT out = delegatedCompoundNBT.read();
        if (out == null)
            out = new CompoundNBT();
        ListNBT list = new ListNBT();
        for (int i = 0; i < stacks.length; ++i)
            if (!stacks[i].isEmpty()) {
                CompoundNBT entry = stacks[i].write(new CompoundNBT());
                entry.putInt("Slot", i);
                list.add(entry);
            }
        out.put("Items", list);
        if (wasFormatted || stacks.length != DEFAULT_SIZE)
            out.putInt("Size", stacks.length);
        if (wasFormatted || columns != 9)
            out.putInt("Columns", columns);
        if (wasFormatted || rows != 3)
            out.putInt("Rows", rows);
        if (wasFormatted || format != Inventories.ItemHandlerFormat.CHEST)
            out.putInt("Format", format.ordinal());
        if (hadRights) {
            list = new ListNBT();
            for (int i = 0; i < rights.length; ++i)
                if (!rights[i].isVanilla()) {
                    CompoundNBT entry = rights[i].writeNBT(new CompoundNBT());
                    entry.putInt("Slot", i);
                    list.add(entry);
                }
            out.put("Rights", list);
        }
        if (hadFluids) {
            out.putInt("SizeFluids", fluids.length);
            list = new ListNBT();
            for (int i = 0; i < fluids.length; ++i)
                if (!fluids[i].isEmpty()) {
                    CompoundNBT entry = fluids[i].writeToNBT(new CompoundNBT());
                    entry.putInt("Slot", i);
                    list.add(entry);
                }
            out.put("Fluids", list);
            list = new ListNBT();
            for (Inventories.TankIORights fluidRight : fluidRights) list.add(fluidRight.writeNBT(new CompoundNBT()));
            out.put("FluidRights", list);
        }
        delegatedCompoundNBT.write(out);
    }

    @Override
    public int getSlots() { return stacks.length; }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) { return slot < 0 || slot >= stacks.length ? ItemStack.EMPTY : stacks[slot]; }

    @Nonnull
    @Override
    public Inventories.ItemStackIORights getRightsInSlot(int slot) { return slot < 0 || slot >= rights.length ? Inventories.ItemStackIORights.VANILLA : rights[slot]; }

    @Override
    public void setRightsInSlot(int slot, Inventories.ItemStackIORights right) {
        if (slot >= 0 && slot < rights.length) {
            rights[slot] = right;
            markDirty();
        }
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= stacks.length) return stack;
        Pair<ItemStack, ItemStack> p = rights[slot].mergeIn(stacks[slot], stack);
        if (!simulate) {
            stacks[slot] = p.getKey();
            markDirty();
        }
        return p.getValue();
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= stacks.length || amount == 0) return ItemStack.EMPTY;
        Pair<ItemStack, ItemStack> p = rights[slot].mergeOut(stacks[slot], amount, ItemStack.EMPTY);
        if (!simulate) {
            stacks[slot] = p.getKey();
            markDirty();
        }
        return p.getValue();
    }

    @Override
    public int getSlotLimit(int slot) { return slot < 0 || slot >= stacks.length ? 64 : rights[slot].maxStack; }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) { return slot >= 0 && slot < stacks.length && rights[slot].canInsert(stacks[slot], stack); }
}
