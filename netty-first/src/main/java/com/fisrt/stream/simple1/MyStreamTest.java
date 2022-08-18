package com.fisrt.stream.simple1;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @since 2022/7/22 16:54
 */
public class MyStreamTest {
    public static void main(String[] args) {
        int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Set<Integer> set1 = new HashSet<>();
        Function<Integer, Integer> f1 = x -> x * x;
        Function<Integer, Integer> f2 = y -> y += 1;
        Result<Integer> handle3 = new Result<>();
        FunctionHandle<Integer, Integer> handle2 = new FunctionHandle<>(f2, handle3);
        FunctionHandle<Integer, Integer> handle1 = new FunctionHandle<>(f1, handle2);
        for (int i = 0; i < data.length; i++) {
//            int x=data[i]*data[i];
//            int y=x+1;
//            Integer x = f1.apply(data[i]);
//            Integer y = f2.apply(x);
            handle1.accept(data[i]);
            int y = handle3.getR();
            set1.add(y);
        }
        System.out.println(set1);
//        MyCollector<Integer,Set<Integer>,Set<Integer>> collector=
        MyCollector<Integer, ?, Set<Integer>> collector = MyHashSetCollector.toSet();

        Set<Integer> collect = MyStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .map(f1)
                .map(f2)
                .collect(collector);
        System.out.println("最终结果" + collect);


    }
}
