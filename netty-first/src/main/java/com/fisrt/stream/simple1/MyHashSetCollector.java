package com.fisrt.stream.simple1;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @since 2022/7/26 10:20
 */
public class MyHashSetCollector<T, A, R> implements MyCollector<T, A, R> {

    private final Supplier<A> supplier;
    private BiConsumer<A, T> accumulator;
    private BinaryOperator<A> combiner;
    private Function<A, R> finisher;


    public MyHashSetCollector(Supplier<A> supplier, BiConsumer<A, T> accumulator) {
        this.supplier = supplier;
        this.accumulator = accumulator;
    }

    public static <T> MyCollector<T, ?, Set<T>> toSet() {
//        return new MyHashSetCollector<>((Supplier<Set<T>>)HashSet::new);
        MyHashSetCollector<T, ?, Set<T>> collector =
                new MyHashSetCollector<>((Supplier<Set<T>>) HashSet::new, Set::add);
        return collector;
    }


    @Override
    public Supplier<A> supplier() {
        return this.supplier;
    }

    @Override
    public BiConsumer<A, T> accumulator() {
        return this.accumulator;
    }

    @Override
    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    @Override
    public Function<A, R> finisher() {
        return this.finisher;
    }
}
