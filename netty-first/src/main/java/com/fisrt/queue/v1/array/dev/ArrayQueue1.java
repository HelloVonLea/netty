package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

/**
 * 一个简单的数组队列
 * NOTE：为了简单，省去了相关的边界校验
 * 采用一个index表示元素放到哪儿了
 * size表示元素个数
 * <p>
 * 存在的问题：
 * 1.放入元素的个数受容量限制，只能放capacity个数据
 * 2.每取一个元素都要移动一次元素，O(n)
 * <p>
 * 针对上面的问题，写出了ArrayQueue2。
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
