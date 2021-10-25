package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环数组实现的队列
 * <p>
 * 前面的代码有两个问题：
 * 1.队列容量有限，满了的时候，生产者不能等待，
 * 空了的时候消费者不能等待
 * 2.队列存在并发问题
 * 针对这个问题，我们想到了第一种解决办法：加锁。
 *
 * @since 2021/10/11 15:52
 */
public class ArrayQueue4<E> implements Queue<E> {
    Lock lock = new ReentrantLock();
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();
    private Object[] elements;
    //元素个数
    private int size;
    private int putIndex;
    private int takeIndex;

    public ArrayQueue4(int capacity) {
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
                    System.out.println("生产者等");
                    notFull.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            elements[putIndex] = e;
//            System.out.println("放入数据时，takeIndex="+takeIndex+"\t数据："+elements[takeIndex]+"\tputIndex="+putIndex+"\t数据="+elements[putIndex]);
            if (++putIndex == elements.length)
                putIndex = 0;
            size++;
            System.out.println("生产者添加数据后数组的样子：" + Arrays.toString(elements) + "\t 大小size：" + size);
            notEmpty.signal();

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
                    System.out.println("消费者Deng");
                    notEmpty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            E e = (E) elements[takeIndex];
            System.out.println("拿出数据时，takeIndex=" + takeIndex + "\t数据：" + e + "\tputIndex=" + putIndex + "\t数据=" + elements[putIndex]);
//            if(e==null){
//                System.out.println("takeIndex="+takeIndex);
//            }
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
