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
 * 主要调整的地方：
 * 去掉了时间的部分，改了测试程序代码。
 *
 * </p>
 * <p>
 * 至此呢，无锁队列就完了，但是我们遗留了一个问题。
 * 这个生产者等待、消费者等待怎么办呢，
 * 你需要唤醒他们就得有锁。
 * 我们来看看MPSC的
 *
 * </P>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue8<E> implements Queue<E> {
    Lock lock = new ReentrantLock();
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();
    private Object[] elements;
    //元素个数
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger putIndex = new AtomicInteger(0);
    private AtomicInteger takeIndex = new AtomicInteger(0);

    public ArrayQueue8(int capacity) {
        this.elements = new Object[capacity];
    }

    public boolean offer(E e) {
        if (size.get() == elements.length) {
            return false;
        } else {
            enqueue(e);
            return true;
        }
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
            while (size.get() == 0) {
                //空了，消费者等待
                try {
                    System.out.println(Thread.currentThread().getName() + "消费者Deng");
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
