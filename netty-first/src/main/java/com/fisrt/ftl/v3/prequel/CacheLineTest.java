package com.fisrt.ftl.v3.prequel;

import java.util.concurrent.CountDownLatch;

/**
 * 缓存行测试
 *
 * @author libo
 * @since 2021/9/22 9:56
 */
public class CacheLineTest {

    public static void main(String[] args) throws InterruptedException {
        final long count = 10_0000_0000L;
        Student student = new Student();
        CountDownLatch latch = new CountDownLatch(2);
        long start = System.currentTimeMillis();
        new Thread(() -> {
            long n = 0;
            while (n++ < count) {
                student.id = 1L;
            }
            latch.countDown();
        }).start();
        new Thread(() -> {
            long n = 0;
            while (n++ < count) {
                student.money = 10L;
            }
            latch.countDown();
        }).start();
        latch.await();
        System.out.println("耗时：" + (System.currentTimeMillis() - start));


    }

    private static class Student {
        public long p1, p2, p3, p4, p5, p6, p7;
        public long id = 0;
        public long p8, p9, p10, p11, p12, p13, p14;
        public long money = 100;
        public long p15, p16, p17, p18, p19, p20, p21;
    }

}
