package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环数组实现的队列
 * <p>
 * 在ArrayQueue4中为了解决并发问题，我们引入了锁。
 * 在ArrayQueue4中存在一个问题，就是获取元素时竟然有空。
 * 经查发现是满空条件判断时用了if而不是while导致。
 * <p>
 * 改正之后，此代码完善，功能正确。满足了第一个要求正确。
 * 但是第二个要求：性能，就不能了。
 *
 * </p>
 *
 * @since 2021/10/11 15:52
 */
public class ArrayQueue5<E> implements Queue<E> {
    Lock lock = new ReentrantLock();
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();
    private Object[] elements;
    //元素个数
    private int size;
    private int putIndex;
    private int takeIndex;

    public ArrayQueue5(int capacity) {
        this.elements = new Object[capacity];
    }

    @Override
    public void enqueue(E e) {
        try {
            lock.lock();
            Object[] elements = this.elements;
            //队列满判断
//            if (size == elements.length) {
//            while (size == elements.length) {
            long start = System.currentTimeMillis();
            //为了让测试的消费者结束，加了个时间控制
            while (size == 0 && (System.currentTimeMillis() - start) < 10_000) {
                //队列满了，生产者等待
                try {
                    System.out.println("生产者等");
                    notFull.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            elements[putIndex] = e;
            if (++putIndex == elements.length)
                putIndex = 0;
            size++;
            System.out.println("生产者添加数据后数组的样子：" + Arrays.toString(elements) + "\t 大小size：" + size);
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
//            if (size == 0) {
            while (size == 0) {
                //空了，消费者等待
                try {
                    System.out.println("消费者Deng");
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            E e = (E) elements[takeIndex];
            if (e == null) {
                System.out.println("数组的样子：" + Arrays.toString(elements) + "\t 大小size：" + size);
                System.out.println("拿出数据时，takeIndex=" + takeIndex + "\t数据：" + e + "\tputIndex=" + putIndex + "\t数据=" + elements[putIndex]);
            }
            elements[takeIndex] = null;
            if (++takeIndex == elements.length)
                takeIndex = 0;
            size--;
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return this.size;
    }
}
