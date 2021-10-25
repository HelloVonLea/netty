package com.fisrt.queue.v1;

/**
 * 队列
 * FIFO
 *
 * @since 2021/10/12 15:45
 */
public interface Queue<E> {
    /**
     * 入队
     *
     * @param e 入队元素
     */
    void enqueue(E e);

    /**
     * 出队
     *
     * @return 出队元素
     */
    E dequeue();

    /**
     * size
     *
     * @return 队列大小
     */
    int size();
}
