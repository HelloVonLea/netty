package com.fisrt.queue.v1.array;

import com.fisrt.queue.v1.Queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环数组实现的队列
 * <p>
 * 在ArrayQueue4中为了解决并发问题，我们引入了锁。
 * 众所周知，锁比较重量级，在速度上，无锁>锁。
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
            if (size == elements.length) {
                //队列满了，生产者等待
                try {
                    notFull.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            elements[putIndex] = e;
            if (++putIndex == elements.length)
                putIndex = 0;
            size++;
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
            if (size == 0) {
                //空了，消费者等待
                try {
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            E e = (E) elements[takeIndex];
            elements[takeIndex] = null;
            if (++takeIndex == elements.length)
                takeIndex = 0;
            size--;
            notFull.signalAll();
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
