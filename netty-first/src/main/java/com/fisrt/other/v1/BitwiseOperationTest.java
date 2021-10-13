package com.fisrt.other.v1;

/**
 * 位运算
 *
 * @author libo
 * @since 2021/9/23 11:17
 */
public class BitwiseOperationTest {


    public static void main(String[] args) {

        int n = -11;
        //判断奇偶,原理：奇数最后一位一定是1，偶数是0
        if ((n & 1) == 1) {
            System.out.println("奇数");
        } else {
            System.out.println("偶数");
        }

        //交换两个数
        int a = 1, b = 2;
        //传统 int t=a;a=b,b=t;
        a = a ^ b;//a=a^b
        b = a ^ b;//b=(a^b)^b=a^0=a
        a = a ^ b;//a=(a^b)^((a^b)^b)=0^b=b
        System.out.println("a=" + a + ",b=" + b);

        //加法运算
        int x = 11, y = 22;
        int sum = add(x, y);
        System.out.println(sum);

    }

    /**
     * 位运算计算加法
     *
     * @param x
     * @param y
     * @return
     */
    public static int add(int x, int y) {
        int a = x ^ y;
        int b = x & y;
        b = b << 1;
        if (b == 0) return a;
        return add(a, b);
    }


}
