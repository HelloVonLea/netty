package com.fisrt.queue.v1.array.release;

import com.fisrt.util.UnsafeUtil;

/**
 * @since 2021/10/19 10:43
 */
public class UnsafeLongArrayAccess {
    //long[]的offset
    public static final long LONG_ARRAY_BASE;
    //long[]的每个元素的偏移量，就是8，1<<3，故这个shift是3
    public static final int LONG_ELEMENT_SHIFT;

    static {
        //返回每个元素的大小
        int scale = UnsafeUtil.UNSAFE.arrayIndexScale(long[].class);
        if (8 == scale) {
            LONG_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size：" + scale);
        }
        //返回long数组中第一个元素的偏移地址
        LONG_ARRAY_BASE = UnsafeUtil.UNSAFE.arrayBaseOffset(long[].class);
    }

    /**
     * 计算循环数组中元素的起始位置。
     * baseOffset+偏移量（index*scale）
     *
     * @param index 指针,元素下标
     * @param mask  mask值是容量-1（2的幂次-1）
     *              数组被指定为2的幂次大小
     * @return
     */
    public static long calcCircularLongElementOffset(long index, long mask) {
        //index是0到mask，
        //index&mask=index%capacity
        //假定capacity是8，mask是7
        //index=0，baseOffset+0*8=base+((0&7)<<3)
        //index=1，baseOffset+1*8=base+((1&7)<<3)=base+8
        //index=2，baseOffset+2*8=base+((2&7)<<3)=base+16
        //Question: index& mask的意义在哪儿？为啥不直接用index？
        //这里巧妙在循环数组处。如果index>=mask时，index&mask=0
        //回到了初始处，数组循环了。秒啊！
        return LONG_ARRAY_BASE + ((index & mask) << LONG_ELEMENT_SHIFT);
    }

    public static void main(String[] args) {
        int capacity = 16;
        int mask = capacity - 1;
        for (int index = 0; index < mask; index++) {
            System.out.println((index & mask) << 3);
            System.out.println(index << 3);
            System.out.println();
        }
        System.out.println((capacity & mask) << 3);
        System.out.println(capacity << 3);
    }


    /**
     * 给数组buffer的offset赋值为e
     *
     * @param buffer
     * @param offset
     * @param e
     */
    public static void soLongElement(long[] buffer, long offset, long e) {
        //putOrderedLong是一个有序有延迟的 putLongVolatile(Object, long, long)
        //只有在field被<code>volatile</code>修饰并且期望被意外修改的时候使用才有用。
        //putLongVolatile就是putLong的volatile版。相当于那个long用volatile修饰了
        UnsafeUtil.UNSAFE.putOrderedLong(buffer, offset, e);
    }


}
