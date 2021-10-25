package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环数组实现的队列
 * <p>
 * </p>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue7<E> implements Queue<E> {
    Lock lock = new ReentrantLock();
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();
    private Object[] elements;
    //元素个数
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger putIndex = new AtomicInteger(0);
    private AtomicInteger takeIndex = new AtomicInteger(0);

    public ArrayQueue7(int capacity) {
        this.elements = new Object[capacity];
    }

    @Override
    public void enqueue(E e) {
        try {
            lock.lock();
            Object[] elements = this.elements;
            //队列满判断
            while (size.get() == elements.length) {
                //队列满了，生产者等待
                try {
                    System.out.println("生产者等");
                    notFull.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            System.out.println("生产数据：" + e);
            elements[putIndex.get()] = e;
            if (putIndex.incrementAndGet() == elements.length)
                putIndex.set(0);
            ;
            size.getAndIncrement();
//            System.out.println("生产者添加数据后数组的样子："+Arrays.toString(elements)+"\t 大小size："+size);
            notEmpty.signalAll();

        } finally {
            lock.unlock();
        }
    }

    @Override
    public E dequeue() {
        try {
            lock.lock();
            Object[] elements = this.elements;
            //队列空判断
            long start = System.currentTimeMillis();
            //为了让测试的消费者结束，加了个时间控制
            while (size.get() == 0 && (System.currentTimeMillis() - start) < 2_000) {
                //空了，消费者等待
                try {
                    System.out.println(Thread.currentThread().getName() + "消费者Deng");
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (size.get() <= 0) return null;
            E e = (E) elements[takeIndex.get()];
            if (e != null) {
                System.out.println("数组的样子：" + Arrays.toString(elements) + "\t 大小size：" + size.get());
                System.out.println("拿出数据时，takeIndex=" + takeIndex.get() + "\t数据：" + e + "\tputIndex=" + putIndex.get() + "\t数据=" + elements[putIndex.get()]);
            }
            elements[takeIndex.get()] = null;
            if (takeIndex.incrementAndGet() == elements.length)
                takeIndex.set(0);
            int andGet = size.decrementAndGet();
            System.out.println(Thread.currentThread().getName() + "拿出数据后，size=" + andGet + "\t takeIndex=" + takeIndex.get());
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return this.size.get();
    }
}
