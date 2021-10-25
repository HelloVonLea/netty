package com.fisrt.queue.v1.array.devtest;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue4;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author
 * @since 2021/10/25 10:35
 */
public class ArrayQueue4Test {

    /**
     * 同样同时启动多个生产者，一个消费者
     * 希望放N*N个数据，拿N*N个数据。
     * 但是，发现有问题，并在ArrayQueue5中解决最终测试正确。
     * <p>
     * 这个排查耗费了我很长时间，特写一篇日记进行了总结。
     * 请看《20211014问题排查总结》
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        int n = 256;
        Queue<Integer> queue = new ArrayQueue4<>(4);
//        Queue<Integer> queue = new ArrayQueue5<>(4);
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
        System.out.println("总共拿到的数据：" + count.get());

    }


}
