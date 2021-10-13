package com.fisrt.queue.v1;

import com.fisrt.queue.v1.array.ArrayQueue1;
import com.fisrt.queue.v1.array.ArrayQueue2;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;

/**
 * @author libo
 * @since 2021/10/12 16:06
 */
public class QueueTest {

    public static void main(String[] args) throws InterruptedException {

//       arrayQueue1Test ();
//        arrayQueue2Test();
//        arrayQueue2QuestionTest();
        arrayQueue2Question2Test();
    }

    /**
     * 这个测试演示一个问题：
     * 多个生产者生产数据不加锁的情况下会出问题。
     * 会出现两个生产者取得putIndex相同的情况，造成取出的数据变少。
     *
     * @throws InterruptedException
     */
    private static void arrayQueue2Question2Test() throws InterruptedException {
        //起1024个线程，每个线程放1024个数据，理论上set应该有1024*1024个数据
        int n = 1 << 10;//1024
        Queue<Integer> queue = new ArrayQueue2<>(1024);
        Phaser phaser = new Phaser(n + 1);
        //latch控制同时开始
        CountDownLatch latch = new CountDownLatch(1);
        Set<Integer> set = new HashSet<>();
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
            for (int i = 0; i < n << 1; i++) {
                Integer num = queue.dequeue();
                if (num != null) {
//                    System.out.println(num);
                    set.add(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(10_000);
        latch.countDown();
        phaser.arriveAndAwaitAdvance();
        System.out.println(set.size());//期望：1,048,576

    }


    /**
     * 这个测试我演示了一个问题：
     * 生产者遇到满了没法等待
     *
     * @throws InterruptedException
     */
    private static void arrayQueue2QuestionTest() throws InterruptedException {
        //1<<27=134,217,728
        int n = 1 << 25;//33,554,432
        Queue<Integer> queue = new ArrayQueue2<>(1024);
        Phaser phaser = new Phaser(3);
        long start = System.currentTimeMillis();
        new Thread(() -> {
            for (int i = 0; i < n; i++) {
                queue.enqueue(i + 1);
            }
            phaser.arrive();
        }).start();
        new Thread(() -> {
            for (int i = 0; i < n; i++) {
                queue.dequeue();
            }
            phaser.arrive();
        }).start();
        phaser.arriveAndAwaitAdvance();
        System.out.println("耗时：" + (System.currentTimeMillis() - start));

    }

    private static void doSleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 经测32,768个数据操作耗时大约16毫秒
     * 经测33,554,432个数据操作耗时大约20473毫秒
     * 优势明显，且不占内存哇
     */
    private static void arrayQueue2Test() {
        //1<<27=134,217,728
        int n = 1 << 25;//33,554,432
        Queue<Integer> queue = new ArrayQueue2<>(n);
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            queue.enqueue(i + 1);
        }
        for (int i = 0; i < n; i++) {
            queue.dequeue();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - start));
    }

    /**
     * 经测32,768个数据操作耗时大约1335毫秒，1秒多一点。
     * 要是上1亿的数据就别指望了。
     * 这个时间复杂度扛不住哇。
     */
    private static void arrayQueue1Test() {
        //1<<27=134,217,728
        int n = 1 << 15;//32,768
        Queue<Integer> queue = new ArrayQueue1<>(n);
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            queue.enqueue(i + 1);
        }
        for (int i = 0; i < n; i++) {
            queue.dequeue();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - start));
    }

}
