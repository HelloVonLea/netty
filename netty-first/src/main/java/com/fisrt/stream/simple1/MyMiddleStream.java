package com.fisrt.stream.simple1;

import java.util.function.Function;

/**
 * @since 2022/7/26 17:07
 */
public class MyMiddleStream<E_IN, E_OUT>
        extends MyAbstractStream<E_IN, E_OUT>
        implements MyStream<E_OUT> {

    //     public Function<? super E_IN,? extends E_OUT> function;
    public Function<? super E_IN, ? extends E_OUT> function;

//     public MyStream<? super E_IN> previousStream;
//
//     public MyStream<? super E_OUT> nextStream;

//    public MyMiddleStream(Function<? super E_IN, ? super E_OUT> function) {
//        this.function=function;
//    }


    /**
     * 这里有几个？需要解释一下
     * MyAbstractStream的？是因为前一个流的输入元素，当前流是不知道的
     * Function第一个？，是由于function输入元素当前流不知道，
     * 当前流的E_IN是前一个的E_OUT，而function是前一个流的E_IN故不能用本流的E_IN
     * 其实应该就是E_IN，但似乎是推断不出来泛型。
     * 第二个？毫无疑问应该是输出元素的子类
     *
     * @param previousStream
     * @param function
     */
    public MyMiddleStream(MyAbstractStream<?, E_IN> previousStream,
                          Function<? super E_IN, ? extends E_OUT> function) {
//    public MyMiddleStream(MyAbstractStream<?,E_IN> previousStream,
//                          Function<?, ? extends E_OUT> function) {
        super(previousStream);
//        this.previousStream=previousStream;
        this.function = function;
//        previousStream.nextStream=this;
    }

    @Override
    public <R> MyStream<R> map(Function<? super E_OUT, ? extends R> function) {
        MyStream<R> stream = new MyMiddleStream<>(this, function);
        return stream;
    }

    @Override
    public <A, R> R collect(MyCollector<? super E_OUT, A, R> collector) {
        //TODO 这里有个大错误，
        //supplier.get是每次都创建新的对象，故不能这么用
        //只能像源码中的那样，用一次supplier.get()
//        A container = collector.supplier().get();
        A container;
//        BiConsumer<A, ? super E_OUT> accumulator = collector.accumulator();
        //第一个对function构造处理链
        //f1->f2->f3->...->fn
//        FunctionChainNode<?, ?> functionChain = buildFunctionChain();
//        FunctionChainNode<Object, Object> chain1 = buildFunctionChain1();
        TerminalOpHandle<E_OUT, A, R> terminalNode = new TerminalOpHandle<>(collector);
        Handle<Object> handle = buildChain(terminalNode);
        //这里面临一个问题，好不容易把Function处理链构造出来了，
        //拿源数组的时候，不知道泛型如何匹配了
        handle.before();
        Object[] data = getDataSource();
        for (int i = 0; i < data.length; i++) {
            //
//            IN in=data[i];
//            functionChain.handle(data[i]);
            handle.accept(data[i]);
//            accumulator.accept(container,data[i]);
        }
        handle.end();
        container = terminalNode.get();
        return (R) container;
//        return null;
    }

    /**
     * 获取源数据 源数组
     *
     * @return
     */
    private <T> T[] getDataSource() {
        return (T[]) this.sourceNode.supplier.get();
//        return new Object[0];
    }


    //    private <T,A,R> Handle<T> buildChain(MyCollector<? super E_OUT, A, R> collector) {
//        Handle<E_OUT> terminalNode = new TerminalOpHandle<>(collector);
//        Handle<? super E_IN> node = new FunctionHandle<>(this.function, terminalNode);
//        MyAbstractStream<?, ?> stream = this;
//        while (stream.previousStream != null) {
//            stream = stream.previousStream;
//            if (stream instanceof MyMiddleStream) {
//                MyMiddleStream ms = (MyMiddleStream) stream;
//                node = new FunctionHandle<>(ms.function, node);
//            }
//        }
//        return (Handle<T>) node;
//    }
    private <T, A, R> Handle<T> buildChain(Handle<E_OUT> terminalNode) {
        Handle<? super E_IN> node = new FunctionHandle<>(this.function, terminalNode);
        MyAbstractStream<?, ?> stream = this;
        while (stream.previousStream != null) {
            stream = stream.previousStream;
            if (stream instanceof MyMiddleStream) {
                MyMiddleStream ms = (MyMiddleStream) stream;
                node = new FunctionHandle<>(ms.function, node);
            }
        }
        return (Handle<T>) node;
    }

}
