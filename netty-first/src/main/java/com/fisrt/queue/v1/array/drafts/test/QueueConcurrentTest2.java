package com.fisrt.queue.v1.array.drafts.test;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue4;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 这个测试类演示了解决了并发问题后的结果
 * 第一次：
 * 演示以ArrayQueue4为例,同时队列中数组保障容量
 * 第二次：
 * 演示以ArrayQueue4为例,同时队列中数组容量有限
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest2 {

    public static void main(String[] args) throws InterruptedException {

        int n = 1 << 4;

//        arrayQueue2Question2Test(n);
        //我们起N个线程，每个线程放N个数据，我们期待queue中有N*N个数据
        //N=512，实际：262,144，期望：262,144
        //N=256，实际：65,536，期望：65,536
        //可以看到，数据少了。理论和实际相符。
//        arrayQueue4Test1(n);
        arrayQueue4Test2(n);
//        arrayBlockingQueueTest3(n);

    }

    //测试失败的，这个代码没跑成功
    private static void arrayBlockingQueueTest3(int n) throws InterruptedException {
        java.util.Queue<Integer> queue = new ArrayBlockingQueue<>(512);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    boolean offer = false;
                    do {
                        offer = queue.offer(j + 1);
                    } while (!offer);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            latch.awaitAdvance(0);
            int c = 0;
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.poll();
                if (num != null) {
//                    System.out.println(num);
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
        System.out.println("queue的大小" + count.get());

    }


    /**
     * 这个方法容量定长
     * 这个时候出现问题：消费者拿到了null数据
     * 当n=4的时候，就有这样的情况出现了。
     * 通过打印日志，我们发现这样一行
     * <b>
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：5
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：6
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：7
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：8
     * 生产者添加数据后数组的样子：[3, 4, 5, 2]	 大小size：9
     * </b>
     * 说明什么，说明队列满了，size还是增加了。
     * 起初我以为时没加volatile的问题，size没刷新到线程。
     * 经测试发现不是。这里明显的多线程有地方没理解。
     * 之后目光注意到，队列满了之后，不应该只判断一次。
     * 判断一次是不对的，wait/notify中就说了应该循环判断，
     * 防止被错误唤醒，这不就是嘛。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue4<>(4);
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
                    System.out.println("循环次数" + i + "值：" + num);
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

    /**
     * 这个测试保证queue的容量够用
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue4<>(n * n);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
//                System.out.println(Thread.currentThread().getName() + "线程开始了");
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger(0);
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
//                    System.out.println(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(10_000);
        int arrive = latch.arrive();
        System.out.println("phaser:" + arrive);
        phaser.arriveAndAwaitAdvance();
        System.out.println(count.get());

    }


}
