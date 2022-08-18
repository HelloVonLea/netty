package com.fisrt.stream.simple1;

import java.util.function.Function;

/**
 * 一个FunctionHandle是一个处理行为链条的节点Node
 * 它实现了Handle，它的下一个处理Node也是Handle
 *
 * @since 2022/7/22 17:00
 */
//为啥不直接用Consumer在接口声明已经说过，不在赘述。
//public class FunctionHandle<IN,OUT> implements Consumer<IN>
public class FunctionHandle<E_IN, E_OUT>
        implements Handle<E_IN> {
    private Function<? super E_IN, ? extends E_OUT> f;
    private Handle<E_OUT> next;

    public FunctionHandle(Function<? super E_IN, ? extends E_OUT> f, Handle<E_OUT> next) {
        this.f = f;
        this.next = next;
    }

    @Override
    public void accept(E_IN in) {
        E_OUT e_out = f.apply(in);
        next.accept(e_out);
    }

    @Override
    public void before() {
        next.before();
    }

    @Override
    public void end() {
        next.end();
    }
}
