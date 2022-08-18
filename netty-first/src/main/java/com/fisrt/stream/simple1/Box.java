package com.fisrt.stream.simple1;

/**
 * 用于存放Set
 *
 * @since 2022/8/18 15:21
 */
public class Box<U> {
    U state;

    public Box() {
    }

    public U get() {
        return state;
    }
}
