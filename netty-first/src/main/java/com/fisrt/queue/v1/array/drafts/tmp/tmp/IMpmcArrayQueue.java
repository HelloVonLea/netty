package com.fisrt.queue.v1.array.drafts.tmp.tmp;

import com.fisrt.queue.util.UnsafeLongArrayUtil;
import com.fisrt.queue.util.UnsafeRefArrayUtil;
import com.fisrt.util.UnsafeUtil;
import org.jctools.util.Pow2;
import org.jctools.util.RangeUtil;
import sun.misc.Unsafe;

/**
 * 无锁循环数组队列的Java实现
 * 此实现作者是参考https://www.1024cores.net/home/lock-free-algorithms/queues/bounded-mpmc-queue
 * 1.他用了两个数组。一个额外的long数组，大小同元素数组大小。这是为buffer分配内存的2-3倍。
 * 2.数组的大小都是power of 2(2的幂次)
 * <p>
 * 原理：
 * buffer存元素，sequenceBuffer存index。用了两个数组来做这件事。
 *
 * @author libo
 * @since 2021/10/20 16:20
 */
public class IMpmcArrayQueue<E> {

    protected static final Unsafe UNSAFE = UnsafeUtil.UNSAFE;
    protected static final long P_INDEX_OFFSET = UnsafeUtil.fieldOffset(IMpmcArrayQueue.class, "producerIndex");
    protected final static long C_INDEX_OFFSET = UnsafeUtil.fieldOffset(IMpmcArrayQueue.class, "consumerIndex");
    protected final long mask;
    protected final E[] buffer;
    protected final long[] sequenceBuffer;
    protected volatile long producerIndex;
    //消费者index
    protected volatile long consumerIndex;

    public IMpmcArrayQueue(int capacity) {
        //保证容量比2大
        int cap = RangeUtil.checkGreaterThanOrEqual(capacity, 2, "capacity");
        //把容量变成2的幂次
        int actualCapacity = Pow2.roundToPowerOfTwo(cap);
        mask = actualCapacity - 1;
        buffer = (E[]) new Object[actualCapacity];
        sequenceBuffer = new long[actualCapacity];
        for (int i = 0; i < actualCapacity; i++) {
            //计算i应该存储的内存位置
            long offset = UnsafeLongArrayUtil.calcCircularLongElementOffset(i, mask);
            //把元素放进去，
            UNSAFE.putOrderedLong(sequenceBuffer, offset, i);
        }
    }

    //mask是容量减1
    public int capacity() {
        return (int) (mask + 1);
    }


    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final long mask = this.mask;
        final long capacity = mask + 1;
        final long[] sBuffer = sequenceBuffer;
        long pIndex;
        long seqOffset;
        long seq;
        long cIndex = Long.MIN_VALUE;
        do {
            //生产者指针
            pIndex = this.producerIndex;
            //拿到pIndex在sequenceBuffer中的内存位置
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(pIndex, mask);
            //拿到sequenceBuffer中的指针
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            //消费者没有动seq，此时seq是上次生产者留下的
            if (seq < pIndex) {
                //再检查一次，确保当队列满了的时候返回false
                if (pIndex - capacity >= cIndex &&//判断缓存中的cIndex
                        pIndex - capacity >= (cIndex = this.consumerIndex)) {//判断最新的cIndex
                    return false;
                } else {//没满生产者指针进1位
                    seq = pIndex + 1;
                }
            }
            //seq>pIndex要么是走了+1要么是取出来的时候就比pIndex大，另一个生产者移动了它
            //然后把pIndex的指针+1
        } while (seq > pIndex ||
                !UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, pIndex, pIndex + 1));//cas进行+1
        //在pIndex处放入元素，cas，此时保证了pIndex中的元素就此线程操作
        UNSAFE.putObject(buffer, UnsafeRefArrayUtil.calcCircularRefElementOffset(pIndex, mask), e);
        //seq++
        UNSAFE.putOrderedLong(sBuffer, seqOffset, pIndex + 1);
        return true;
    }

    public E poll() {
        //避免了在volatile读后重复的加载
        final long[] sBuffer = sequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seq;
        long seqOffset;
        long expectedSeq;
        long pIndex = -1;

        do {
            cIndex = this.consumerIndex;
            //拿到sBuffer中cIndex内存地址
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(cIndex, mask);
            //获取seq
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;
            //没有竞争的情况下，小的进入if，大或等的话说明另一个消费者改了，直接下一个循环
            if (seq < expectedSeq) {
                //槽位没有被生产者移动
                if (cIndex >= pIndex &&//测试缓存的pIndex 这里测试的意义是？可能是下次循环的pIndex值
                        cIndex == (pIndex = this.producerIndex))//说明队列空了，该生产了。
                    //Question：循环数组判断满或空，cIndex==pIndex可能满也可能空哇？
                    return null;
                else
                    seq = expectedSeq + 1;//seq是cIndex移了2位
            }
        } while (seq > expectedSeq ||//说明其它消费者消费了，
                !UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, cIndex, cIndex + 1));//cas改cIndex的值
        //上面的什么时候终止循环，seq==expectedSeq或cas==true
        //cIndex的在Object数组中的内存位置
        final long offset = UnsafeRefArrayUtil.calcCircularRefElementOffset(cIndex, mask);
        //获取元素
        final E e = (E) UNSAFE.getObject(buffer, cIndex);
        //将数组该处的元素置为null
        UNSAFE.putObject(buffer, offset, null);
        //将long数组中seqOffset处元素置为cIndex+capacity
        //也就是long[cIndex]=cIndex+capacity
        //i.e. seq+=capacity
        UNSAFE.putOrderedLong(sBuffer, seqOffset, cIndex + mask + 1);

        return e;
    }

    public int size() {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and
         * consumer indices, therefore protection is required to ensure size is within valid range. In the
         * event of concurrent polls/offers to this method the size is OVER estimated as we read consumer
         * index BEFORE the producer index.
         */
        long after = this.consumerIndex;
        long size;
        while (true) {
            final long before = after;
            final long currentProducerIndex = this.producerIndex;
            after = this.consumerIndex;
            //两次消费者指针未变化时，计算size，否则自旋。
            //Why?因为在并发情况下这两次行为之间，若是消费了，size就变大了。
            if (before == after) {
                size = currentProducerIndex - after;
                break;
            }
        }
        //size是long型的
        // Long overflow is impossible (), so size is always positive. Integer overflow is possible for the unbounded
        // indexed queues.
        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
            // concurrent updates to cIndex and pIndex may lag behind other progress enablers (e.g. FastFlow), so we need
            // to check bound
        else if (size < 0)
            return 0;
        else if (capacity() != -1 && size > capacity())//-1表示无界容量
            return capacity();
        else
            return (int) size;
    }


}
