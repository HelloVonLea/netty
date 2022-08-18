package com.fisrt.stream.simple1;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @since 2022/7/26 16:53
 */
//为啥用E_IN而不用T，主要为了方便直观
//public class MyHeadStream<T,E_OUT>
//        extends MyAbstractStream<T,E_OUT>
//        implements MyStream<T> {
public class MyHeadStream<E_IN, E_OUT>
        extends MyAbstractStream<E_IN, E_OUT>
        //很多人这里写的不对，觉得是E_IN，其实是E_OUT
        //Head这里有两个泛型，如果是E_IN，这个map、collect方法就没法处理，匹配
//        implements MyStream<E_IN> {
        implements MyStream<E_OUT> {

    private E_IN[] data;

    public MyHeadStream(E_IN[] data) {
        super(() -> data);
        this.data = data;
    }

    /**
     * 这里E_IN就是很好理解，E_OUT不是R也很清楚
     *
     * @param function 其参数的下限是T，上限是R才行
     *                 很容易直接写成T，R，这样是不合适的。
     * @param <R>
     * @return
     */
    @Override
    public <R> MyStream<R> map(Function<? super E_OUT, ? extends R> function) {
        MyStream<R> middleStream = new MyMiddleStream<E_OUT, R>(this, function);
        return middleStream;
    }

    @Override
    public <A, R> R collect(MyCollector<? super E_OUT, A, R> collector) {
        A container = collector.supplier().get();
        BiConsumer<A, ? super E_OUT> accumulator = collector.accumulator();
        for (int i = 0; i < data.length; i++) {
            //
            accumulator.accept(container, (E_OUT) data[i]);
        }
        return (R) container;
    }
}
