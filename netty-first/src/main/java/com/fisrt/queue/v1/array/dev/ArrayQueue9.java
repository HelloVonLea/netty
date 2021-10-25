package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 循环数组实现的队列
 * 无锁队列
 * <p>
 * 主要调整的地方：
 * 队列满/空的时候自旋。
 * 但能不能保证没有问题呢？
 * 还是不对，单生产者单消费者都不对。
 * <p>
 * 经过改正，我么们使用AtomicReferenceArray代替了数组，
 * 这样就保证了正确性，在单生产者-单消费者的情况下成功了。
 *
 *
 * </p>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue9<E> implements Queue<E> {

    //    private volatile Object[] elements;
    //AtomicReferenceArray内部的数组,用这个代替了Object[]
    private AtomicReferenceArray<Object> elements;
    //元素个数
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger putIndex = new AtomicInteger(0);
    private AtomicInteger takeIndex = new AtomicInteger(0);

    public ArrayQueue9(int capacity) {
//        this.elements = new Object[capacity];
        elements = new AtomicReferenceArray<>(capacity);

    }

    public static void main(String[] args) throws InterruptedException {

        int n = 4;
//        test1(n);
        test2(n);

    }

    /**
     * 单生产者-单消费者
     * 放N个数据，看能取出来多少个。
     * 期望：N，实际：N
     *
     * @param n
     * @throws InterruptedException
     */
    private static void test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue9<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(1 + 1 + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);
        new Thread(() -> {
            startLatch.awaitAdvance(0);
            for (int j = 0; j < n; j++) {
                queue.enqueue(j + 1);
            }
            closePhaser.arrive();
        }).start();
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            startLatch.awaitAdvance(0);
            int c = 0;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 20_000) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
                    System.out.println("数据：" + num);
                } else {
                    c++;
                }
            }

//            System.out.println("c="+c);
            closePhaser.arrive();
        }).start();

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到的数据：" + count.get());

    }

    /**
     * 多生产者-单消费者
     * <p>
     * 一测试，我们就发现，出现
     * <code>
     * 队列满了,size=4	 数组=[10, 11, 12, null]
     * </code>
     * 很明显，成员变量各自都是原子的，但组合在一起就不是了
     * OK，ArrayQueue10中改正。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue9<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(n + 1 + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                closePhaser.arrive();
            }).start();

        }
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            startLatch.awaitAdvance(0);
            int c = 0;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 20_000) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
                    System.out.println("数据：" + num);
                } else {
                    c++;
                }
            }

//            System.out.println("c="+c);
            closePhaser.arrive();
        }).start();

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        //直接等待60秒，让消费者消费完然后看结果就可以了
//        closePhaser.arrive();
//        Thread.sleep(10_000);
        System.out.println("总共拿到的数据：" + count.get());

    }

    @Override
    public void enqueue(E e) {

        AtomicReferenceArray<Object> elements = this.elements;
        do {
            //队列满，自旋
            while (size.get() == elements.length()) {
                System.out.println("队列满了,size=" + size.get() + "\t 数组=" + elements.toString());
                Thread.yield();
            }

        } while (!elements.compareAndSet(putIndex.get(), null, e));
        if (putIndex.incrementAndGet() == elements.length())
            putIndex.set(0);
        size.getAndIncrement();

    }

    @Override
    public E dequeue() {
        AtomicReferenceArray<Object> elements = this.elements;
        int index = 0;
        Object e;
        do {
            //队列空判断
            long start = System.currentTimeMillis();
            while (size.get() == 0) {
                //超过1秒，放弃
                if (System.currentTimeMillis() - start > 1_000)
                    return null;
                //空了，自旋
//                System.out.println("队列空了");
                Thread.yield();
            }
            index = takeIndex.get();
            e = elements.get(index);
            System.out.println("更新：" + e + "\t index=" + index + "\t ");
        } while (e == null || !elements.compareAndSet(index, e, null));

        if (takeIndex.incrementAndGet() == elements.length())
            takeIndex.set(0);
        size.decrementAndGet();
        return (E) e;

    }

    @Override
    public int size() {
        return this.size.get();
    }


}
