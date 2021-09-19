package com.fisrt.ftl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class MyInternalThreadLocalMap {
    private static final AtomicInteger UPDATER = new AtomicInteger(0);
    private Object[] VALUES = new Object[32];

    public static int nextIndex() {
        int index = UPDATER.getAndIncrement();
        if (index >= Integer.MAX_VALUE) {
            throw new IllegalStateException("too many variables");
        }
        return index;
    }

    public <T> T getIndexedObject(int index) {
        if (index < VALUES.length) {
            return (T) VALUES[index];
        }
        return null;
    }

    public <T> void set(T t, int index) {
        if (index < VALUES.length) {
            VALUES[index] = t;
            return;
        }
        Object[] old = VALUES;
        int oldLen = old.length;
        int newLen = oldLen << 1;
        Object[] newArr = new Object[newLen];
        System.arraycopy(old, 0, newArr, 0, oldLen);
        newArr[index] = t;
    }

    public void remove(int index) {
        if (index < VALUES.length)
            VALUES[index] = null;
    }

    public void removeAll() {
        for (int i = 0; i < VALUES.length; i++) {
            VALUES[i] = null;
        }
    }
}
