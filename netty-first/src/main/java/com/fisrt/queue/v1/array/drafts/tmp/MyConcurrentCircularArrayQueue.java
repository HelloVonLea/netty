package com.fisrt.queue.v1.array.drafts.tmp;

import com.fisrt.util.UnsafeUtil;
import org.jctools.util.Pow2;
import sun.misc.Unsafe;

/**
 * @since 2021/10/18 18:12
 */
abstract class MyConcurrentCircularArrayQueue<E> {
    protected static final Unsafe UNSAFE = UnsafeUtil.UNSAFE;
    protected final long mask;
    protected final E[] buffer;

    public MyConcurrentCircularArrayQueue(int capacity) {
        //1 << (32 - Integer.numberOfLeadingZeros(value - 1))
        //numberOfLeadingZeros用来计算int的二进制值从左到右有连续多少个0。
        //返回32时，说明是0。负数返回0。
        //1 << (32 - Integer.numberOfLeadingZeros(value - 1))寻找到比value大的下一个2的幂次
        //算法：求一个数的临近的较大的2的整数次幂
        //秒啊！
        int actualCapacity = Pow2.roundToPowerOfTwo(capacity);
        mask = actualCapacity - 1;
//        buffer=UnsafeRefArrayAccess.allocateRefArray(actualCapacity);
        buffer = (E[]) new Object[capacity];
    }


    public static void main(String[] args) {
        int value = 3;
        int number = Integer.numberOfLeadingZeros(value - 1);
        System.out.println(number);
        int nextPow2 = 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
        System.out.println(nextPow2);

    }


}
