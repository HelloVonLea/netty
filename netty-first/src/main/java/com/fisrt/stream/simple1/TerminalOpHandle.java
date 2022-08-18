package com.fisrt.stream.simple1;

import java.util.function.BiConsumer;

/**
 * @since 2022/8/5 16:58
 */
//public class TerminalOpNode<T,A,R> implements Consumer<T> {
//public class TerminalOpHandle<T,A,R>  implements Handle<T> {
//这里添加Box的目的就是为了不让多次去supplier.get
//那就需要在执行accept方法之前进行且只进行一次supplier.get
public class TerminalOpHandle<T, A, R> extends Box<A> implements Handle<T> {
    MyCollector<? super T, A, R> collector;

    public TerminalOpHandle(MyCollector<? super T, A, R> collector) {
        this.collector = collector;
    }

    @Override
    public void before() {
        state = collector.supplier().get();
    }

    @Override
    public void end() {

    }

    @Override
    public void accept(T t) {
//        A a = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
//        accumulator.accept(a,t);
        accumulator.accept(state, t);
    }
}
