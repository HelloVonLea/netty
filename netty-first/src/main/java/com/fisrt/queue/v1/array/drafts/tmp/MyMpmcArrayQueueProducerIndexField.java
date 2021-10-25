package com.fisrt.queue.v1.array.drafts.tmp;

import com.fisrt.util.UnsafeUtil;

/**
 * @author libo
 * @since 2021/10/18 18:04
 */
abstract class MyMpmcArrayQueueProducerIndexField<E> extends MyConcurrentSequencedCircularArrayQueue<E> {
    protected static final long P_INDEX_OFFSET = UnsafeUtil.fieldOffset(MyMpmcArrayQueueProducerIndexField.class, "producerIndex");
    protected volatile long producerIndex;

    public MyMpmcArrayQueueProducerIndexField(int capacity) {
        super(capacity);
    }

    public long getProducerIndex() {
        return producerIndex;
    }
}
