package com.fisrt.util;

/**
 * 2的幂次工具类
 *
 * @author libo
 * @since 2021/10/25 13:35
 */
public class Pow2Util {
    public static final int MAX_POW2 = 1 << 30;

    /**
     * 寻找比一个数大的2的幂次
     * <p>
     * 在HashMap中也有一个算法和这个是差不多的。
     * 思路不同而已。
     *
     * @param c 数
     * @return 2的幂次
     */
    public static int calPow2(int c) {
        if (c > MAX_POW2) {
            throw new IllegalArgumentException("参数过大");
        }
        if (c < 2) {
            throw new IllegalArgumentException("参数过小");
        }
        return 1 << (32 - Integer.numberOfLeadingZeros(c));
    }

    /**
     * 判断一个数是否是2的幂次
     *
     * @param c 数
     * @return true/false
     */
    public static boolean isPow2(int c) {
        return (c & (c - 1)) == 0;
    }

    /**
     * 内存对齐算法
     * 2的幂次内存对齐
     * <p>
     * 字节对齐是在分配内存时需要考虑的问题，两个小算法
     * <p>
     * 2字节对齐，要求地址位为2,4,6,8...，要求二进制位最后一位为0（2的1次方）
     * 4字节对齐，要求地址位为4,8,12,16...，要求二进制位最后两位为0（2的2次方）
     * 8字节对齐，要求地址位为8,16,24,32...，要求二进制位最后三位为0（2的3次方）
     * 16字节对齐，要求地址位为16,32,48,64...，要求二进制位最后四位为0（2的4次方）
     * ...
     * 由此可见，我们只要对数据补齐对齐所需最少数据，然后将补齐位置0就可以实现对齐计算。
     * <p>
     * （1）(align-1)，表示对齐所需的对齐位，如：2字节对齐为1，4字节为11，8字节为111，16字节为1111...
     * （2）(x+(align-1))，表示x补齐对齐所需数据
     * （3）&~(align-1)，表示去除由于补齐造成的多余数据
     * （4） (x+(align-1))&~(align-1)，表示对齐后的数据
     * <p>
     * 举个例子：如8字节对齐。起始地始是6
     * 6 + （8 - 1）=0000 0110 + 0000 0111 = 0000 1101
     * 0000 1101 & ~(0000 0111) = 0000 1000  //去除由于补齐造成的多余数据
     *
     * @param value     对齐的数
     * @param alignment 必须是2的幂次
     * @return
     */
    public static long align(final long value, final int alignment) {
        if (!isPow2(alignment)) {
            throw new IllegalArgumentException("alignment must be a power of 2:" + alignment);
        }
        return (value + (alignment - 1)) & ~(alignment - 1);
    }


}
