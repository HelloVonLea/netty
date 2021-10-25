package com.fisrt.queue.v1.array.drafts.tmp;

import com.fisrt.queue.util.UnsafeLongArrayUtil;
import com.fisrt.queue.util.UnsafeRefArrayUtil;
import org.jctools.util.RangeUtil;

/**
 * @since 2021/10/18 16:03
 */
public class MyMpmcArrayQueue<E> extends MyMpmcArrayQueueConsumerIndexField<E> {

    public static final int MAX_LOOK_AHEAD_STEP = 4096;
    private final int lookAheadStep;


    public MyMpmcArrayQueue(final int capacity) {
        super(RangeUtil.checkGreaterThanOrEqual(capacity, 2, "capacity"));
        this.lookAheadStep = Math.max(2, Math.min(capacity() / 4, MAX_LOOK_AHEAD_STEP));
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
            //
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(pIndex, mask);
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            if (seq < pIndex) {
                if (pIndex - capacity >= cIndex &&
                        pIndex - capacity >= (cIndex = this.consumerIndex)) {
                    return false;
                } else {
                    seq = pIndex + 1;
                }
            }

        } while (seq > pIndex ||
                !UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, pIndex, pIndex + 1));
        UNSAFE.putObject(buffer, UnsafeRefArrayUtil.calcCircularRefElementOffset(pIndex, mask), e);
        //seq++
        UNSAFE.putOrderedLong(sBuffer, seqOffset, pIndex + 1);
        return true;
    }

    public E poll() {
        final long[] sBuffer = sequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seq;
        long seqOffset;
        long expectedSeq;
        long pIndex = -1;

        do {
            cIndex = this.consumerIndex;
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(cIndex, mask);
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;
            if (seq < expectedSeq) {
                if (cIndex >= pIndex && cIndex == (pIndex = this.producerIndex))
                    return null;
                else
                    seq = expectedSeq + 1;
            }
        } while (seq > expectedSeq ||
                !UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, cIndex, cIndex + 1));

        final long offset = UnsafeRefArrayUtil.calcCircularRefElementOffset(cIndex, mask);
        final E e = (E) UNSAFE.getObject(buffer, cIndex);
        UNSAFE.putObject(buffer, offset, null);
        //i.e. seq+=capacity
        UNSAFE.putOrderedLong(sBuffer, seqOffset, cIndex + mask + 1);

        return e;
    }

    public int size() {
        long after = this.consumerIndex;
        long size;
        while (true) {
            final long before = after;
            final long currentProducerIndex = this.producerIndex;
            after = this.consumerIndex;
            if (before == after) {
                size = currentProducerIndex - after;
                break;
            }
        }
        //size是long型的
        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (size < 0)
            return 0;
        else if (capacity() != -1 && size > capacity())//-1表示无界容量
            return capacity();
        else
            return (int) size;
    }


}
