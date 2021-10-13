package com.fisrt.queue.v1;

/**
 * @since 2021/10/12 15:45
 */
public interface Queue<E> {
    void enqueue(E e);

    E dequeue();

    int size();
}
