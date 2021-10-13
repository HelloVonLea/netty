package com.fisrt.queue.v2;

/**
 * 队列
 * 自己定义自己实现的队列
 *
 * @since 2021/10/11 15:43
 */
public interface MyQueue<E> {
    /**
     * 添加一个元素，队列满了抛出异常
     */
    boolean add(E e);

    /**
     * 添加一个元素，队列满了返回一个特殊值
     */
    boolean offer(E e);

    /**
     * 移除第一个元素，队列为空抛出异常
     */
    E remove();

    /**
     * 移除第一个元素，队列为空返回一个特殊值
     */
    E poll();

    /**
     * 查询第一个元素，队列为空抛出异常
     */
    E element();

    /**
     * 查询第一个元素，队列为空返回特殊值
     */
    E peek();

    int size();
    //由于遍历还是要一定功夫的这里暂时留坑
//    Iterator<E> iterator();

}
