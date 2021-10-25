package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环数组实现的队列
 * <p>
 * 在ArrayQueue5中我们引入了锁来保证正确性。
 * 但是性能不够。
 * 这个时候，就出现了无锁队列。
 * 在前面我们知道队列在碰到多线程的时候会有问题，
 * 所以，我们引入了锁来解决问题。
 * 在生产者生产数据的时候，假如线程A拿到的putIndex是1，
 * 线程B拿到的putIndex也是1，就出问题了。
 * 针对putIndex、takeIndex、size这些，我们用自旋+CAS操作
 * 就可以保证正确性，从而保证了整个queue的正确。
 *
 *
 * </p>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue6<E> implements Queue<E> {
    Lock lock = new ReentrantLock();
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();
    private Object[] elements;
    //元素个数
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger putIndex = new AtomicInteger(0);
    private AtomicInteger takeIndex = new AtomicInteger(0);

    public ArrayQueue6(int capacity) {
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
            while (size.get() == 0 && (System.currentTimeMillis() - start) < 10_000) {
                //空了，消费者等待
                try {
                    System.out.println("消费者Deng");
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            E e = (E) elements[takeIndex.get()];
//            if (e == null) {
//                System.out.println("数组的样子："+Arrays.toString(elements)+"\t 大小size："+size);
//            System.out.println("拿出数据时，takeIndex="+takeIndex+"\t数据："+e+"\tputIndex="+putIndex+"\t数据="+elements[putIndex]);
//            }
            elements[takeIndex.get()] = null;
            if (takeIndex.incrementAndGet() == elements.length)
                takeIndex.set(0);
            size.decrementAndGet();
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
