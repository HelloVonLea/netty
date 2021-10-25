package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 根据文章
 * http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.53.8674&rep=rep1&type=pdf
 * 具体思路
 * 1.数组队列是一个循环数组，队列少用一个元素，
 * 当头等于尾标示队空，尾加1等于头标示队满。
 * 2.数组的元素用EMPTY（无数据，标示可以入队）
 * 和FULL（有数据，标示可以出队）标记指示，
 * 数组一开始全部初始化成 EMPTY标示空队列。
 * 3.EnQue 操作：如果当前队尾位置为EMPTY，
 * 标示线程可以在当前位置入队，通过CAS原子操作把该位置设置为FULL，
 * 避免其它线程操作这个位置，操作完后修改队尾位置。
 * 各个线程竞争新的队尾位置。
 * <p>
 * 1）数组队列应该是一个ring buffer形式的数组（环形数组）
 * <p>
 * 2）数组的元素应该有三个可能的值：HEAD，TAIL，EMPTY（当然，还有实际的数据）
 * <p>
 * 3）数组一开始全部初始化成EMPTY，有两个相邻的元素要初始化成HEAD和TAIL，这代表空队列。
 * <p>
 * 4）EnQueue操作。假设数据x要入队列，定位TAIL的位置，使用double-CAS方法把(TAIL, EMPTY) 更新成 (x, TAIL)。需要注意，如果找不到(TAIL, EMPTY)，则说明队列满了。
 * <p>
 * 5）DeQueue操作。定位HEAD的位置，把(HEAD, x)更新成(EMPTY, HEAD)，并把x返回。同样需要注意，如果x是TAIL，则说明队列为空。
 * <p>
 * 算法的一个关键是——如何定位HEAD或TAIL？
 * <p>
 * 1）我们可以声明两个计数器，一个用来计数EnQueue的次数，一个用来计数DeQueue的次数。
 * <p>
 * 2）这两个计算器使用使用Fetch&ADD来进行原子累加，在EnQueue或DeQueue完成的时候累加就好了。
 * <p>
 * 3）累加后求个模什么的就可以知道TAIL和HEAD的位置了。
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue12_Tmp<E> implements Queue<E> {

    //代表为空，没有元素
    public static final Object EMPTY = new Object();
    //头指针,尾指针
    AtomicInteger head, tail;
    private AtomicReferenceArray<E> atomicReferenceArray;

    public ArrayQueue12_Tmp(int capacity) {
        atomicReferenceArray = new AtomicReferenceArray(capacity + 1);
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
    }

    public void print() {
        StringBuffer buffer = new StringBuffer("[");
        for (int i = 0; i < atomicReferenceArray.length(); i++) {
            if (i == head.get() || atomicReferenceArray.get(i) == null) {
                continue;
            }
            buffer.append(atomicReferenceArray.get(i) + ",");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("]");
        System.out.println("队列内容:" + buffer.toString());

    }


    @Override
    public void enqueue(E e) {
        int index = (tail.get() + 1) % atomicReferenceArray.length();
        if (index == head.get() % atomicReferenceArray.length()) {
            System.out.println("当前队列已满," + e + "无法入队!");
            return;
        }
        while (!atomicReferenceArray.compareAndSet(index, (E) EMPTY, e)) {
            enqueue(e);
        }
        tail.incrementAndGet(); //移动尾指针
        System.out.println("入队成功!" + e);
    }

    @Override
    public E dequeue() {
        if (head.get() == tail.get()) {
            System.out.println("当前队列为空");
            return null;
        }
        int index = (head.get() + 1) % atomicReferenceArray.length();
        E ele = atomicReferenceArray.get(index);
        if (ele == null) { //有可能其它线程也在出队
            return dequeue();
        }
        while (!atomicReferenceArray.compareAndSet(index, ele, (E) EMPTY)) {
            return dequeue();
        }
        head.incrementAndGet();
        System.out.println("出队成功!" + ele);
        return ele;

    }

    @Override
    public int size() {
        //这里又怎么实现呢？
        return 0;
    }


}
