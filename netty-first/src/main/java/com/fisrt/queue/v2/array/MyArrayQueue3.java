package com.fisrt.queue.v2.array;

import com.fisrt.queue.v2.MyQueue;

import java.util.NoSuchElementException;

/**
 * 参考ArrayBlockingQueue
 *
 * @since 2021/10/11 15:52
 */
public class MyArrayQueue3<E> implements MyQueue<E> {
    private Object[] elements;
    //元素个数
    private int size;
    //咱们的只有一个index，环形数组需要两个
    private int putIndex;
    private int takeIndex;
    //容量
    private int capacity;

    public MyArrayQueue3(int capacity) {
        this.capacity = capacity;
        this.elements = new Object[capacity];
    }

    @Override
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("队列已满");
    }

    @Override
    public boolean offer(E e) {
        if (size == elements.length)
            return false;
        else {
            enqueue(e);
            return true;
        }
    }

    private void enqueue(E e) {
        Object[] elements = this.elements;
        elements[putIndex] = e;
        if (++putIndex == elements.length)
            putIndex = 0;
        size++;
    }

    @Override
    public E remove() {
        E e = poll();
        if (e != null) {
            return e;
        } else {
            throw new NoSuchElementException("队列无该元素");
        }
    }

    @Override
    public E poll() {
        return size == 0 ? null : dequeue();
    }

    private E dequeue() {
        Object[] elements = this.elements;
        E e = (E) elements[takeIndex];
        elements[takeIndex] = null;
        if (++takeIndex == elements.length)
            takeIndex = 0;
        size--;
        return e;
    }

    @Override
    public E element() {
        E e = peek();
        if (e != null)
            return e;
        else
            throw new NoSuchElementException();
    }

    @Override
    public E peek() {
        return elementAt(takeIndex);
    }

    final E elementAt(int i) {
        return (E) elements[i];
    }

    @Override
    public int size() {
        return this.size;
    }
}
