package com.fisrt.queue.v1;

import com.fisrt.queue.v1.array.ArrayQueue4;

import java.util.concurrent.Phaser;

/**
 * 这个测试类演示了解决了并发问题后的结果
 * 第一次：
 * 演示以ArrayQueue4为例,同时队列中数组保障容量
 * 第二次：
 * 演示以ArrayQueue4为例,同时队列中数组容量有限
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest {

    public static void main(String[] args) throws InterruptedException {

        int n = 1 << 9;

//        arrayQueue2Question2Test(n);
        //我们起N个线程，每个线程放N个数据，我们期待queue中有N*N个数据
        //N=512，实际：262,144，期望：262,144
        //N=256，实际：65,536，期望：65,536
        //可以看到，数据少了。理论和实际相符。
//        arrayQueue4Test1(n);
        arrayQueue4Test2(n);

    }

    /**
     * 这个方法容量定长
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue4<>(1024);
        Phaser phaser = new Phaser(n + 1);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                System.out.println(Thread.currentThread().getName() + "线程开始了");
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    System.out.println(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(3_000);
        int arrive = latch.arrive();
        System.out.println("phaser:" + arrive);
        phaser.arriveAndAwaitAdvance();
        System.out.println("queue的大小" + queue.size());

    }

    /**
     * 这个测试保证queue的容量够用
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue4<>(n * n);
        Phaser phaser = new Phaser(n + 1);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                System.out.println(Thread.currentThread().getName() + "线程开始了");
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    System.out.println(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(10_000);
        int arrive = latch.arrive();
        System.out.println("phaser:" + arrive);
        phaser.arriveAndAwaitAdvance();
        System.out.println(queue.size());

    }


}
