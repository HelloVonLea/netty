package com.fisrt.queue.v2.array;

import com.fisrt.queue.v2.MyQueue;

/**
 * @since 2021/10/11 15:48
 */
public class MyArrayQueue<E> implements MyQueue<E> {
    private E[] elements;
    private int size;
    private int index;
    private int capacity;

    public MyArrayQueue(int capacity) {
        this.capacity = capacity;
        index = 0;
        size = 0;
        //这里是个难点，怎么初始化？
        //解决办法强转
        elements = (E[]) new Object[capacity];
    }


    @Override
    public boolean add(E e) {
        if (size == capacity)
            throw new IllegalStateException("队列已满");
        elements[index] = e;
        index++;
        size++;
        return true;
    }

    @Override
    public boolean offer(E e) {
        if (size == capacity)
            return false;
        elements[index] = e;
        index++;
        size++;
        return true;
    }

    @Override
    public E remove() {
        if (size <= 0)
            throw new IllegalStateException("队列为空");
        E e = elements[0];
        for (int i = 1; i < elements.length; i++) {
            elements[i] = elements[i + 1];
        }
        index--;
        size--;
        return e;
    }

    @Override
    public E poll() {
        if (size <= 0)
            return null;
        E e = elements[0];
        for (int i = 1; i < elements.length; i++) {
            elements[i] = elements[i + 1];
        }
        index--;
        size--;
        return e;
    }

    @Override
    public E element() {
        if (size <= 0)
            throw new IllegalStateException("队列为空");
        E e = elements[0];
        return e;
    }

    @Override
    public E peek() {
        if (size <= 0)
            return null;
        E e = elements[0];
        return e;
    }

    @Override
    public int size() {
        return this.size;
    }
}
