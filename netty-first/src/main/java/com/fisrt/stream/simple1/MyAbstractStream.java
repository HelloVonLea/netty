package com.fisrt.stream.simple1;

import java.util.function.Supplier;

/**
 * @since 2022/7/27 16:42
 */
public abstract class MyAbstractStream<P_IN, P_OUT> {

    public MyAbstractStream<?, P_OUT> previousStream;

    public MyAbstractStream<P_OUT, ?> nextStream;

    public MyAbstractStream<?, ?> sourceNode;

    public Supplier<Object[]> supplier;


    public MyAbstractStream(MyAbstractStream previousStream) {
        if (previousStream == null) {
            this.previousStream = null;
            this.sourceNode = this;
        } else {
            this.previousStream = previousStream;
            previousStream.nextStream = this;
            this.sourceNode = previousStream.sourceNode;
        }
    }

    public MyAbstractStream(Supplier<Object[]> supplier) {
        this.supplier = supplier;
        this.previousStream = null;
        this.sourceNode = this;
    }
}
