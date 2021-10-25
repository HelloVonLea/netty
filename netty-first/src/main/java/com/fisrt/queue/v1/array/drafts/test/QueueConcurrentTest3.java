package com.fisrt.queue.v1.array.drafts.test;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue6;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest3 {

    public static void main(String[] args) throws InterruptedException {

        int n = 1 << 4;

        //我们起N个线程，每个线程放N个数据，我们期待queue中有N*N个数据
        //N=512，实际：262,144，期望：262,144
        //N=256，实际：65,536，期望：65,536
        //可以看到，数据少了。理论和实际相符。
//        arrayQueue6Test1(n);
        //在测试方法1中获得了正确的结果。
        //但是在测试方法2中获得了错误的结果
        //仔细思考下，发现我们忽略了一个问题。
        //生产者和消费者速度匹配问题？
        //想象一下，在单生产者-单消费者的情况下，
        //假如生产者比较慢，消费者比较快，如测试方法3
//        arrayQueue6Test2(n);

//        arrayQueue6Test3(n);
        arrayQueue6Test4(n);

    }

    /**
     * 单生产者-多消费者
     * 我们放N个数据，起N个消费者，希望消费N个数据。
     * N=16，放16个数据，起16个消费者，希望拿到16个数据，
     * 结果只拿到12个。Why？
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue6Test4(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue6<>(4);
        Phaser phaser = new Phaser(n + 1 + 1);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int j = 0; j < n; j++) {
                queue.enqueue(j + 1);
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            phaser.arrive();
        }).start();
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                int c = 0;
                for (int j = 0; j < (n); j++) {
                    Integer num = queue.dequeue();
                    if (num != null) {
                        count.getAndIncrement();
                    } else {
                        c++;
                    }
                }
//            System.out.println("c="+c);
                phaser.arrive();
            }).start();

        }

        Thread.sleep(3_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到的数据：" + count.get());

    }

    /**
     * 单生产者-单消费者
     * 这个测试演示了，生产者比消费者慢的情况。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue6Test3(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue6<>(4);
        Phaser phaser = new Phaser(3);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int j = 0; j < n; j++) {
                queue.enqueue(j + 1);
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            phaser.arrive();
        }).start();
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            latch.awaitAdvance(0);
            int c = 0;
            for (int j = 0; j < (n); j++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
                } else {
                    c++;
                }
            }
            System.out.println("c=" + c);
            phaser.arrive();
        }).start();

        Thread.sleep(3_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到的数据：" + count.get());

    }

    /**
     * 多生产者-多消费者。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue6Test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue6<>(4);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                int c = 0;
                for (int j = 0; j < (n); j++) {
                    Integer num = queue.dequeue();
                    if (num != null) {
                        count.getAndIncrement();
                    } else {
                        c++;
                    }
                }
                System.out.println("c=" + c);
                phaser.arrive();
            }).start();
        }

        Thread.sleep(3_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到的数据：" + count.get());

    }


    /**
     * 这个方法容量定长
     * 多生产者-单消费者
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue6Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue6<>(4);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            latch.awaitAdvance(0);
            int c = 0;
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
                } else {
                    c++;
                }
            }
            System.out.println("c=" + c);
            phaser.arrive();
        }).start();
        Thread.sleep(1_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
//        System.out.println("put=="+count1.get());
        System.out.println("总共拿到的数据：" + count.get());

    }


}
