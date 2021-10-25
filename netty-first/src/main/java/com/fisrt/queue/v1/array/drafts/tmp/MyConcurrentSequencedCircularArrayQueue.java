package com.fisrt.queue.v1.array.drafts.tmp;

import com.fisrt.queue.util.UnsafeLongArrayUtil;
import com.fisrt.util.UnsafeUtil;

/**
 * @author
 * @since 2021/10/18 18:08
 */
abstract class MyConcurrentSequencedCircularArrayQueue<E> extends MyConcurrentCircularArrayQueue<E> {
    protected final long[] sequenceBuffer;

    public MyConcurrentSequencedCircularArrayQueue(int capacity) {
        super(capacity);
        int actualCapacity = (int) (this.mask + 1);
        sequenceBuffer = new long[actualCapacity];
        for (int i = 0; i < actualCapacity; i++) {
            //jcTools的原生方法看起来不方便，还原了一下
//            UnsafeLongArrayAccess.soLongElement(sequenceBuffer, UnsafeLongArrayAccess.calcCircularLongElementOffset(i, mask), i);
            //计算处放入元素的下标
            long offset = UnsafeLongArrayUtil.calcCircularLongElementOffset(i, mask);
            //把元素放进去，
            UnsafeUtil.UNSAFE.putOrderedLong(buffer, offset, i);
        }
    }


}
