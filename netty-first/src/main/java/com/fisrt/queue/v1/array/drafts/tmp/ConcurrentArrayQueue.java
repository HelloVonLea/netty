package com.fisrt.queue.v1.array.drafts.tmp;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 2021/10/15 17:52
 */
public class ConcurrentArrayQueue<T> {
    // 环状缓存
    private final Object[] ring;
    private final AtomicInteger maximumReadIndex = new AtomicInteger(0);
    private final AtomicInteger readIndex = new AtomicInteger(0);
    private final AtomicInteger writeIndex = new AtomicInteger(0);

    public ConcurrentArrayQueue(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("Illegal capacity " + capacity);
        ring = new Object[capacity + 1];
    }

    public static void main(String[] args) throws InterruptedException {
        test3(4);

    }

    private static void test3(int n) throws InterruptedException {
        ConcurrentArrayQueue<Integer> queue = new ConcurrentArrayQueue<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(n + n + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);

        for (int i = 0; i < n; i++) {
//            int finalI = i;
            new Thread(() -> {
//                int x= finalI;
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((j + 1)*x);
                    queue.enqueue((j + 1));
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
                    Integer num = queue.dequeue();
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
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }

    public boolean enqueue(T e) {
        int currentReadIndex, currentWriteIndex;

        do {
            currentReadIndex = readIndex.get();
            currentWriteIndex = writeIndex.get();

            // check if queue is full
            if (((currentWriteIndex + 1) % ring.length) ==
                    (currentReadIndex % ring.length))
                return false;
        } while (!writeIndex.compareAndSet(currentWriteIndex, currentWriteIndex + 1));

        // We know now that this index is reserved for us. Use it to save the data
        ring[currentWriteIndex % ring.length] = e;

        // update the maximum read index after saving the data.
        // It might fail if there are more than 1 producer threads because this
        // operation has to be done in the same order as the previous CAS
        while (!maximumReadIndex.compareAndSet(currentWriteIndex, currentWriteIndex + 1)) {
            // this is a good place to yield the thread in case there are more
            // software threads than hardware processors and you have more
            // than 1 producer thread
            // have a look at sched_yield() (POSIX.1b)
            Thread.yield();
        }

        return true;
    }

    public T dequeue() {
        int currentMaximumReadIndex;
        int currentReadIndex;

        while (true) {
            currentReadIndex = readIndex.get();
            currentMaximumReadIndex = maximumReadIndex.get();

            // The queue is empty or a producer thread has allocate space in the queue
            // but is waiting to commit the data into it
            if ((currentReadIndex % ring.length) ==
                    (currentMaximumReadIndex % ring.length))
                return null;

            // retrieve the data from the queue
            @SuppressWarnings("unchecked")
            T ret = (T) ring[currentReadIndex % ring.length];

            if (readIndex.compareAndSet(currentReadIndex, currentReadIndex + 1))
                return ret; // 这里没有办法清理残余的引用，可能导致内存泄露
        }
    }

    public int size() {
        int ret = writeIndex.get() - readIndex.get();
        if (ret < 0)
            return 0;
        return ret;
    }

    public void clear() {
        while (size() > 0)
            dequeue();
    }

    public int capacity() {
        return ring.length - 1;
    }


}
