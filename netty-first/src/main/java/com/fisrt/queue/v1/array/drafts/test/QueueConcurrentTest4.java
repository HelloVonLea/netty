package com.fisrt.queue.v1.array.drafts.test;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue7;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ArrayQueue7测试
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest4 {

    public static void main(String[] args) throws InterruptedException {

        int n = 1 << 4;

        arrayQueue7Test1(n);

    }

    /**
     * 单生产者-多消费者
     * 我们放N个数据，起N个消费者，希望消费N个数据。
     * N=16，放16个数据，起16个消费者，希望拿到16个数据，
     * 结果只拿到12个。Why？
     * <p>
     * 不知道为啥。只有看日志了，
     * 发现size=-239。我去，肯定减的太多了。
     * 我们知道只有一处进行了减。
     * <p>
     * 其实数组队列存在一个问题。
     * 就是takeIndex跑到了putIndex前面。
     * 设想有一个无限长的数组，由于消费者消费较快，
     * takeIndex就会跑到putIndex前面。这是不对的。
     * 无限长的数组，takeIndex到putIndex就可以停下来了。
     * 对于循环数组，怎么判断这样的情况呢？
     * 其实是有两个问题：
     * 1.生产者追上消费者了，数据还没消费，就被生产者覆盖了
     * 2.消费者追上生产者，数据还没生产，就被消费了。
     * 怎么解决这个问题呢？
     * 其实不会出现，因为size只有在put的时候增长，size=0时，不会消费。
     * 这样就保证了不会出现速度不匹配问题。
     * 回到这个问题，那为啥会出错呢？
     * 还是测试程序写的不好。我们重写下
     * 请看ArrayQueue8和QueueConcurrentTest5。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue7Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue7<>(4);
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


}
