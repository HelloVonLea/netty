package com.fisrt.queue.v1.array.drafts.test;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue8;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ArrayQueue7测试
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest5 {

    public static void main(String[] args) throws InterruptedException {
        int n = 1 << 4;
        arrayQueue8Test1(n);
    }

    /**
     * 单生产者-多消费者
     * 我们放N个数据，起N个消费者，希望消费N个数据。
     * N=16，放16个数据，起16个消费者，希望拿到16个数据，
     * 结果只拿到12个。Why？
     * <p>
     * 经查发现是测试程序的问题，
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue8Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue8<>(4);
        Phaser phaser = new Phaser(n + 1 + 1);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int j = 0; j < n; j++) {
                queue.enqueue(j + 1);
                try {
                    Thread.sleep(300);//0.3s
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
                long start = System.currentTimeMillis();
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
//        phaser.arriveAndAwaitAdvance();
        //由于代码的原因，我们不好让消费者线程终止，
        //于是我们直接等待60秒，让消费者消费完然后看结果就可以了
        phaser.arrive();
        Thread.sleep(60_000);
        System.out.println("总共拿到的数据：" + count.get());

    }


}
