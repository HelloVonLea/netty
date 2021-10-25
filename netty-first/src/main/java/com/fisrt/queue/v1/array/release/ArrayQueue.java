package com.fisrt.queue.v1.array.release;

import com.fisrt.queue.v1.Queue;
import com.fisrt.util.Pow2Util;
import com.fisrt.util.UnsafeUtil;
import org.jctools.queues.MpmcArrayQueue;
import sun.misc.Unsafe;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 循环数组实现的无锁队列
 *
 * <p>
 * 在并发情况下，我们会遇到包括不限于这些问题：
 * 1.如何防止多个线程覆盖写一个元素？
 * 2.如何防止读到还未写的元素？
 * 3.size()如何统计？
 * 4.队列满或空如何判断？
 * <p>
 * <p>
 * 我们采用jcTools中的MpmcArrayQueue的原理。
 * 1.我们用CAS来保证原子性。
 * 2.数组大小都是2的幂次，双指针，一生产者一消费者。
 * 3.多了一个long数组----sequenceBuffer
 * 这是怎么联动的呢？
 * 生产者指针pIndex是一直自增的，相当于AtomicLong
 * 消费者指针cIndex是一直自增的，相当于AtomicLong
 * 这保证了每个线程拿到的指针都是唯一的
 * index和capacity取余会映射出对应的下标。
 * 假设我们的capacity是4，index就会在[0,1,2,3]这个集合中。
 * sequenceBuffer中存的是什么呢？
 * 会存pIndex+1和cIndex+capacity。下标就是指针对应的位置。
 * 这么存有什么作用呢？
 * 假如有竞争，某个线程取出pIndex=4,下标=0，此时看seq。
 * seq==pIndex,说明sequenceBuffer中0位置的元素为4，0位置处元素被消费了，
 * 可以放！先改掉pIndex，这样别的线程就可感知，之后再放。
 * seq<pIndex,说明因为pIndex=4,若小，肯定是seq=1，之前pIndex=0放的元素没有被消费。
 * 有可能是满了，也有可能不满，不满说明，cIndex改了，但是seq还没更改，再走一遍循环。
 * seq>pIndex,说明seq>4，seq=5或8，seq=5说明另一个线程放进去了，8说明别的线程放了，
 * 还有别的线程消费了，此线程饿的太久了，pIndex过期了，再循环拿新的。
 * 假设上面的线程A取出pIndex=4时,另一个线程B取出pIndex=8（起了很多线程）,下标0，看seq。
 * 此时的seq只会比pIndex=8小，因为为8放入的是9（B还没放），之前的A为4放入5，消费者放4或8，
 * 当你线程多的时候，pIndex只会不断变大。而seq都是有规律的，值是可预算的。
 * 这样就保证了多个线程不会覆盖一个位置的写。
 * pIndex变大后，seq小，不进行写，进自旋。
 * <p>
 * 双指针自增就保证了可以用pIndex-cIndex=size，一条射线。
 * 这样用pIndex-cIndex和capacity判断就可以知道是不是满了，
 * 而空了就是pIndex==cIndex。现在明白为啥用long了吧。
 *
 *
 *
 *
 * </p>
 *
 * @since 2021/10/11 15:52
 */
public class ArrayQueue<E> implements Queue<E> {

    private static final Unsafe UNSAFE = UnsafeUtil.UNSAFE;
    private final static long P_INDEX_OFFSET = UnsafeUtil.fieldOffset(ArrayQueue.class, "putIndex");
    private final static long C_INDEX_OFFSET = UnsafeUtil.fieldOffset(ArrayQueue.class, "takeIndex");
    private final long mask;
    private final long[] sequenceBuffer;
    private Object[] buffer;
    private volatile long putIndex;
    private volatile long takeIndex;


    public ArrayQueue(int capacity) {
        if (capacity < 2)
            throw new IllegalArgumentException("容量太小！");
        //把容量变成2的幂次
        int actualCapacity = Pow2Util.calPow2(capacity);
        mask = actualCapacity - 1;
        buffer = new Object[actualCapacity];
        sequenceBuffer = new long[actualCapacity];
        for (int i = 0; i < actualCapacity; i++) {
            long offset = UnsafeLongArrayAccess.calcCircularLongElementOffset(i, mask);
            UNSAFE.putOrderedLong(sequenceBuffer, offset, i);
        }
    }

    public static void main(String[] args) throws Exception {

        int n = 10;
//        test3(n);
//        Queue<String> queue = new ArrayQueue13<>(4);
        MpmcArrayQueue<String> jcQueue = new MpmcArrayQueue<>(4);
        for (int i = 0; i < n; i++) {
            final int x = i;
            new Thread(() -> {
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((x+"-"+j));
                    while (!jcQueue.offer(x + "-" + j)) {
                        try {
                            Thread.sleep(1_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 20_000) {
//                    String obj = queue.dequeue();
                    String obj = jcQueue.poll();
                    if (obj != null) System.out.println(obj);
                }
            }).start();

        }

    }

    /**
     * @param n
     * @throws InterruptedException
     */
    private static void test3(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(n + n + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);

        for (int i = 0; i < n; i++) {
//            int finalI = i;
            new Thread(() -> {
//                int x= finalI;
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((j + 1)*x);
                    queue.enqueue((j + 1));
                }
                closePhaser.arrive();
            }).start();

        }
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                startLatch.awaitAdvance(0);
                int c = 0;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 20_000) {
                    Integer num = queue.dequeue();
                    if (num != null) {
                        count.getAndIncrement();
//                    System.out.println("数据：" + num);
                    } else {
                        c++;
                    }
                }

//            System.out.println("c="+c);
                closePhaser.arrive();
            }).start();

        }

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }

    @Override
    public void enqueue(E e) {
        if (e == null) {
            return;
        }
        final long mask = this.mask;
        final long capacity = mask + 1;
        final long[] sBuffer = sequenceBuffer;
        long pIndex;
        long seqOffset;
        long seq;//sequence下标
        long cIndex = Long.MIN_VALUE;
        do {
            pIndex = putIndex;
            seqOffset = UnsafeLongArrayAccess.calcCircularLongElementOffset(pIndex, mask);
            seq = UNSAFE.getLongVolatile(sequenceBuffer, seqOffset);
            if (seq < pIndex) {
                if (pIndex - capacity >= cIndex &&
                        pIndex - capacity >= (cIndex = this.takeIndex)) {
                    return;
                } else {
                    seq = pIndex + 1;
                }
            }
        } while (seq > pIndex ||
                !UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, pIndex, pIndex + 1));
        UNSAFE.putObject(buffer, UnsafeRefArrayAccess.calcCircularRefElementOffset(pIndex, mask), e);
        //seq++
        UNSAFE.putOrderedLong(sBuffer, seqOffset, pIndex + 1);
    }

    @Override
    public E dequeue() {
        final long[] sBuffer = sequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seq;
        long seqOffset;
        long expectedSeq;
        long pIndex = -1;

        do {
            cIndex = this.takeIndex;
            seqOffset = UnsafeLongArrayAccess.calcCircularLongElementOffset(cIndex, mask);
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;
            if (seq < expectedSeq) {
                //槽位没有被生产者移动
                if (cIndex >= pIndex &&
                        cIndex == (pIndex = this.takeIndex))//说明队列空了，该生产了。
                    return null;
                else
                    seq = expectedSeq + 1;
            }
        } while (seq > expectedSeq ||
                !UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, cIndex, cIndex + 1));
        final long offset = UnsafeRefArrayAccess.calcCircularRefElementOffset(cIndex, mask);
        final E e = (E) UNSAFE.getObject(buffer, cIndex);
        UNSAFE.putObject(buffer, offset, null);
        //i.e. seq+=capacity
        UNSAFE.putOrderedLong(sBuffer, seqOffset, cIndex + mask + 1);
        return e;
    }

    @Override
    public int size() {
        long after = this.takeIndex;
        long size;
        while (true) {
            final long before = after;
            final long currentProducerIndex = this.putIndex;
            after = this.takeIndex;
            if (before == after) {
                size = currentProducerIndex - after;
                break;
            }
        }
        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (size < 0)
            return 0;
        else if (capacity() != -1 && size > capacity())//-1表示无界容量
            return capacity();
        else
            return (int) size;
    }

    /**
     * 获取容量
     *
     * @return
     */
    public int capacity() {
        return (int) (this.mask + 1);
    }


}
