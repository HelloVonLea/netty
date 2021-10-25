package com.fisrt.queue.v1.array.drafts.tmp;

import com.fisrt.util.UnsafeUtil;

/**
 * @author good
 * @since 2021/10/18 17:35
 */
abstract class MyMpmcArrayQueueConsumerIndexField<E> extends MyMpmcArrayQueueProducerIndexField<E> {

    protected final static long C_INDEX_OFFSET = UnsafeUtil.fieldOffset(MyMpmcArrayQueueConsumerIndexField.class, "consumerIndex");

    //消费者index
    protected volatile long consumerIndex;

    public MyMpmcArrayQueueConsumerIndexField(int capacity) {
        super(capacity);
    }

    public long getConsumerIndex() {
        return consumerIndex;
    }
}
