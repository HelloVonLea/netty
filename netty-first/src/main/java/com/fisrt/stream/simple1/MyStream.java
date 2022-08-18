package com.fisrt.stream.simple1;

import java.util.function.Function;

/**
 * @since 2022/7/26 16:49
 */
public interface MyStream<T> {

    static <T> MyStream<T> of(T... values) {
        return new MyHeadStream<>(values);
    }

    /**
     * 映射处理
     *
     * @param function 其参数的下限是T，上限是R才行
     *                 很容易直接写成T，R，这样是不合适的。
     * @param <R>
     * @return
     */
//    <R> MyStream<R> map(Function<? super T,? super R> function);
    <R> MyStream<R> map(Function<? super T, ? extends R> function);

    /**
     * 收集
     *
     * @param collector
     * @param <A>       在toSet中 A和R是一样的，故可以强转
     * @param <R>
     * @return
     */
    <A, R> R collect(MyCollector<? super T, A, R> collector);


}
