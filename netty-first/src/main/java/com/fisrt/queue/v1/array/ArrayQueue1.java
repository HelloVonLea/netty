package com.fisrt.queue.v1.array;

import com.fisrt.queue.v1.Queue;

/**
 * 一个简单的数组队列
 * NOTE：为了简单，省去了相关的边界校验
 *
 * @since 2021/10/12 15:57
 */
public class ArrayQueue1<E> implements Queue<E> {
    private Object[] elements;
    private int size;
    private int index;
    private int capacity;

    public ArrayQueue1(int capacity) {
        this.capacity = capacity;
        index = 0;
        size = 0;
        elements = new Object[capacity];
    }

    @Override
    public void enqueue(E e) {
        elements[index] = e;
        index++;
        size++;
    }

    @Override
    public E dequeue() {
        E e = (E) elements[0];
        moveLeftOneStep();
        index--;
        size--;
        return e;
    }

    //左移一步
    private void moveLeftOneStep() {
        for (int i = 1; i < elements.length - 1; i++) {
            elements[i] = elements[i + 1];
        }
    }

    @Override
    public int size() {
        return this.size;
    }

}
