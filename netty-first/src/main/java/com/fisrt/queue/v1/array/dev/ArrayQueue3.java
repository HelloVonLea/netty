package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

/**
 * 循环数组实现的队列
 * <p>
 *
 * </px>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue3<E> implements Queue<E> {
    private Object[] elements;
    //元素个数
    private int size;
    private int putIndex;
    private int takeIndex;

    public ArrayQueue3(int capacity) {
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
