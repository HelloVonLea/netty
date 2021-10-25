package com.fisrt.util;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * @author yourDad
 * @since 2021/10/18 17:41
 */
public class UnsafeUtil {

    public static final Unsafe UNSAFE;

    static {
        UNSAFE = getUnsafe();
    }

    /**
     * 由于Java中Unsafe在外部由其静态方法获得，
     * 故使用反射大法。
     *
     * @return
     */
    private static Unsafe getUnsafe() {
        Unsafe unsafe;
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                Constructor<Unsafe> constructor = Unsafe.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                unsafe = constructor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return unsafe;
    }

    /**
     * 返回field的内存起始地址
     *
     * @param clz
     * @param fieldName
     * @return
     */
    public static long fieldOffset(Class clz, String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(clz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
