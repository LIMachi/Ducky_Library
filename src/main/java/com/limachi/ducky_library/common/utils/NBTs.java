package com.limachi.ducky_library.common.utils;

import com.limachi.ducky_library.DuckyLib;
import net.minecraft.nbt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTs {
    /**
     * create a clean copy of 'content' in 'target' (like target = content but keep the original pointer of target)
     * @param target
     * @param content
     * @return
     */
    public static CompoundNBT replace(CompoundNBT target, CompoundNBT content) {
        if (target == null)
            return content;
        for (String key : target.keySet())
            target.remove(key);
        for (String key : content.keySet()) {
            INBT t = content.get(key);
            if (t != null)
                target.put(key, t);
        }
        return target;
    }

    public static INBT deepMergeNBTInternal(INBT to, INBT from) {
        if (from == null)
            return to;
        if (to == null
                || to.getType() != from.getType()
                || from.getType() == StringNBT.TYPE
                || from.getType() == ByteNBT.TYPE
                || from.getType() == DoubleNBT.TYPE
                || from.getType() == FloatNBT.TYPE
                || from.getType() == IntNBT.TYPE
                || from.getType() == LongNBT.TYPE
                || from.getType() == ShortNBT.TYPE)
            return from;
        if (from.getType() == CompoundNBT.TYPE) {
            CompoundNBT tc = (CompoundNBT) to;
            CompoundNBT fc = (CompoundNBT) from;
            for (String key : fc.keySet()) {
                INBT p = deepMergeNBTInternal(tc.get(key), fc.get(key));
                if (p != null)
                    tc.put(key, p);
            }
            return to;
        }
        if (from.getType() == ByteArrayNBT.TYPE
                || from.getType() == IntArrayNBT.TYPE
                || from.getType() == ListNBT.TYPE
                || from.getType() == LongArrayNBT.TYPE) {
            int sf = ((CollectionNBT) from).size();
            int st = ((CollectionNBT) to).size();
            for (int i = 0; i < sf; ++i)
                if (i < st)
                    ((CollectionNBT) to).set(i, deepMergeNBTInternal((INBT) ((CollectionNBT) to).get(i), (INBT) ((CollectionNBT) from).get(i)));
                else
                    ((CollectionNBT) to).add(i, (INBT) ((CollectionNBT) from).get(i));
            return to;
        }
        return null;
    }

    /**
     * will generate a nbt list from a list, using the function 'conv' applied to each element of the list 'in'
     * @param in the list to convert to a ListNBT
     * @param storeIndexes if non null, will use this key to store the index of the elements inside the ListNBT entry
     * @param conv should convert whatever type the original list's elements are to compound nbt (if null is returned, the element will be skipped)
     * @return a ListNBT that can be used in another convertList to get back the original list
     */
    public static <T> ListNBT convertList(@Nonnull List<T> in, @Nullable String storeIndexes, @Nonnull Function<T, CompoundNBT> conv) {
        ListNBT out = new ListNBT();
        for (int i = 0; i < in.size(); ++i) {
            CompoundNBT entry = conv.apply(in.get(i));
            if (entry != null) {
                if (storeIndexes != null)
                    entry.putInt(storeIndexes, i);
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * will populate a list from a ListNBT (note: the list must already have been initialized with the correct size before population)
     * @param in the ListNBT that should be converted back to elements in the List<T> 'list'
     * @param list the output list with a size big enough to contain all the elements stored (and potentially indexed) in the ListNBT 'in'
     * @param storeIndexes if non null, will use this key to get the index of the elements inside the ListNBT entry
     * @param conv should convert the entries to whatever type the output list is
     */
    public static <T> void populateList(@Nonnull ListNBT in, @Nonnull List<T> list, @Nullable String storeIndexes, @Nonnull Function<CompoundNBT, T> conv) {
        for (int i = 0; i < in.size(); ++i) {
            CompoundNBT entry = in.getCompound(i);
            list.set(storeIndexes == null ? i : entry.getInt(storeIndexes), conv.apply(entry));
        }
    }

    public static CompoundNBT newCompound(Object ...init) {
        Function<CompoundNBT, CompoundNBT> ft = (t)->t;
        CompoundNBT out = new CompoundNBT();
        for (int i = 0; i < init.length; ++i) {
            if (init[i] instanceof String) {
                String key = (String)init[i];
                ++i;
                if (init[i] instanceof String) {out.putString(key, (String)init[i]); continue;}
                if (init[i] instanceof Integer) {out.putInt(key, (Integer)init[i]); continue;}
                if (init[i] instanceof Boolean) {out.putBoolean(key, (Boolean)init[i]); continue;}
                if (init[i] instanceof Byte) {out.putByte(key, (Byte)init[i]); continue;}
                if (ft.getClass().isInstance(init[i])) {out.put(key, ((Function<CompoundNBT, CompoundNBT>)init[i]).apply(new CompoundNBT())); continue;}
                if (init[i] instanceof List<?>) {
                    List<?> list = (List<?>)init[i + 1];
                    if (list.size() == 0) continue;
                    Object o = list.get(0);
                    if (o instanceof String) {out.put(key, convertList((List<String>)list, "ListIndex", t->newCompound("String", t))); continue;}
                }
            }
            DuckyLib.breakPoint();
        }
        return out;
    }

    public static <T> T getOrDefault(CompoundNBT nbt, String key, Function<String, T> method, T def) {
        if (nbt.contains(key))
            return method.apply(key);
        return def;
    }

    public static class DelegatedCompoundNBT {
        protected final Supplier<CompoundNBT> read;
        protected final Consumer<CompoundNBT> write;
        protected final Function<CompoundNBT, CompoundNBT> resync;
        protected CompoundNBT cache;

        public DelegatedCompoundNBT(@Nonnull Supplier<CompoundNBT> read, @Nonnull Consumer<CompoundNBT> write, @Nonnull Function<CompoundNBT, CompoundNBT> resync) {
            this.read = read;
            this.cache = read.get();
            this.write = write;
            this.resync = resync;
        }

        public CompoundNBT read() {
            CompoundNBT t = read.get();
            if (!Objects.equals(t, cache)) {
                cache = resync.apply(cache);
                t = cache;
            }
            return t;
        }

        public void write(CompoundNBT w) {
            cache = w;
            write.accept(w);
        }
    }
}
