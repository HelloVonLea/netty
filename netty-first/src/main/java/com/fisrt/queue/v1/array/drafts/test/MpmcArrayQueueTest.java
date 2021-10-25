package com.fisrt.queue.v1.array.drafts.test;

import org.jctools.queues.MpmcArrayQueue;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 2021/10/18 14:10
 */
public class MpmcArrayQueueTest {

    public static void main(String[] args) throws InterruptedException {
        int n = 16;

        MpmcArrayQueue<Integer> queue = new MpmcArrayQueue(4);
        Phaser closePhaser = new Phaser(n + n + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);

        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    while (!queue.offer(j + 1)) {
                        System.out.println("插入失败");
                        Thread.yield();
                    }
//                    queue.enqueue((j + 1));
                }
                closePhaser.arrive();
            }).start();

        }
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                startLatch.awaitAdvance(0);
                int c = 0;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 20_000) {
//                    Integer num = queue.dequeue();
                    Integer num = queue.poll();
                    if (num != null) {
                        count.getAndIncrement();
//                    System.out.println("数据：" + num);
                    } else {
                        c++;
                    }
                }

//            System.out.println("c="+c);
                closePhaser.arrive();
            }).start();


        }

        Thread.sleep(3_000);
        startLatch.arrive();
        System.out.println("size=" + queue.size());
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }


}
