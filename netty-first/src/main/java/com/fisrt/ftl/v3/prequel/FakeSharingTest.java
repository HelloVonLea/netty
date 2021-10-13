package com.fisrt.ftl.v3.prequel;

import java.util.concurrent.Phaser;

/**
 * @author libo
 * @since 2021/9/22 15:29
 */
public class FakeSharingTest {

    public static void main(String[] args) throws InterruptedException {
        Point point = new Point();
        Phaser phaser = new Phaser();
        phaser.bulkRegister(2);
        long start = System.currentTimeMillis();
        new Thread(() -> {
            int i = 0;
            while (i++ < 10_0000_0000) {
                point.x = 1;
            }
            phaser.arrive();
        }, "线程1").start();
        new Thread(() -> {
            int i = 0;
            while (i++ < 10_0000_0000) {
                point.y = 2;
            }
            phaser.arrive();
        }, "线程2").start();
        phaser.awaitAdvance(2);
        System.out.println("耗时：" + (System.currentTimeMillis() - start));

    }

    public static class Point {
        //        public long p1, p2, p3, p4, p5, p6, p7,px;
        public int x = 0;
        //        public long p8, p9, p10, p11, p12, p13, p14,py;
        public int y = 0;
    }


}
