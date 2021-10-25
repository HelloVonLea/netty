package com.fisrt.queue.v1.array.release;

import org.jctools.util.UnsafeAccess;

/**
 * @since 2021/10/18 18:15
 */
public class UnsafeRefArrayAccess<E> {
    public static final long REF_ARRAY_BASE;
    public static final int REF_ELEMENT_SHIFT;

    static {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            REF_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size: " + scale);
        }
        REF_ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class);
    }

    public static <E> E[] allocateRefArray(int capacity) {
        return (E[]) new Object[capacity];
    }

    public static long calcCircularRefElementOffset(long index, long mask) {
        return REF_ARRAY_BASE + ((index & mask) << REF_ELEMENT_SHIFT);
    }
}
