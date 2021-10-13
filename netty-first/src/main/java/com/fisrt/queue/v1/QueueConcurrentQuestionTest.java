package com.fisrt.queue.v1;

import com.fisrt.queue.v1.array.ArrayQueue2;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;

/**
 * 这个测试类演示了不加锁的数组队列出现的并发问题
 * 演示以ArrayQueue3为例,同时队列中数组保障容量
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentQuestionTest {

    public static void main(String[] args) throws InterruptedException {
        //第一次测试
        // n=1024,
        // 起1024个线程，每个线程放1024个数据，理论上collection应该有1024*1024个数据
        // 期望：1,048,576，实际：1940
//        int n = 1 << 10;//1024
        //第二次测试，n=512
        //期望：262,144，实际：900
        int n = 1 << 8;

//        arrayQueue2Question2Test(n);
        //我们起N个线程，每个线程放N个数据，我们期待queue中有N*N个数据
        //N=512，实际：234081，期望：262,144
        //N=256，实际：65464，期望：65,536
        //可以看到，数据少了。理论和实际相符。
        arrayQueue2Question2Test2(n);
//        arrayQueue2Question2Test3(n);

    }

    /**
     * 用Phaser代替Count DownLatch的测试
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue2Question2Test3(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue2<>(n * n);
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
        Thread.sleep(10_000);
        int arrive = latch.arrive();
        System.out.println("phaser:" + arrive);
        phaser.arriveAndAwaitAdvance();
        System.out.println(queue.size());

    }


    /**
     * 这个测试就看最后queue的大小就可以判断出来了。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue2Question2Test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue2<>(n * n);
        Phaser phaser = new Phaser(n + 1);
        //latch控制同时开始
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "线程开始了");
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        Thread.sleep(10_000);
        latch.countDown();
        phaser.arriveAndAwaitAdvance();
        System.out.println(queue.size());

    }


    /**
     * 这个测试演示一个问题：
     * 多个生产者生产数据不加锁的情况下会出问题。
     * 会出现两个生产者取得putIndex相同的情况，造成取出的数据变少。
     *
     * @throws InterruptedException
     */
    private static void arrayQueue2Question2Test(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue2<>(n * n);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        CountDownLatch latch = new CountDownLatch(1);
        Collection<Integer> collection = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();

        }

        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
//                    System.out.println(num);
                    collection.add(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(10_000);
        latch.countDown();
        phaser.arriveAndAwaitAdvance();
        System.out.println(collection.size());

    }


}
