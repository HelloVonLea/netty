package com.fisrt.stream.simple1;

import java.util.function.Consumer;

/**
 * 处理器
 * function形成的处理链
 * 一个Handle是一个Node。
 * <p>
 * 为啥要继承Consumer，为了消费数据
 * 其实还有个问题，就是泛型问题
 * HandleNode<IN,OUT>比带两个泛型，
 * 而nextHandle的泛型却只知道一个也就是上一个的OUT，
 * 故引入一个接口Handle<T>来进行声明就放心了。
 * 为啥不直接用Consumer接口来做？
 * consumer是一个单一的接口，功能单一，Handle接口可以提供多功能
 *
 * @since 2022/7/22 17:05
 */
public interface Handle<T> extends Consumer<T> {
    void before();

    void end();
}
