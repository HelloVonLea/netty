package com.fisrt.queue.v1.array;

import com.fisrt.queue.v1.Queue;

/**
 * 循环数组实现的队列
 * <p>
 * 循环数组涉及到一个问题：
 * 如何判断数组满了或空了？
 * 在ArrayBlockingQueue中是size=elements.length表示满了，
 * size==0表示空了。
 * 有三种方法：
 * 1.预存长度法
 * 也就是size,入队->size+1,出队->size-1,数组长度==size为满了，
 * size==0为空。
 * 2.空一位法
 *
 * @since 2021/10/11 15:52
 */
public class ArrayQueue2<E> implements Queue<E> {
    private Object[] elements;
    //元素个数
    private int size;
    private int putIndex;
    private int takeIndex;

    public ArrayQueue2(int capacity) {
        this.elements = new Object[capacity];
    }

    @Override
    public void enqueue(E e) {
        Object[] elements = this.elements;
        //队列满判断
        if (size == elements.length) {
            throw new IllegalStateException("队列已满");
        }
        elements[putIndex] = e;
        if (++putIndex == elements.length)
            putIndex = 0;
        size++;
    }

    @Override
    public E dequeue() {
        Object[] elements = this.elements;
        //队列空判断
        if (size == 0) {
            return null;
        }
        E e = (E) elements[takeIndex];
        elements[takeIndex] = null;
        if (++takeIndex == elements.length)
            takeIndex = 0;
        size--;
        return e;
    }

    @Override
    public int size() {
        return this.size;
    }
}
