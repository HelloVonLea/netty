package com.fisrt.stream.simple1;

/**
 * @since 2022/7/22 17:40
 */
public class Result<R> implements Handle<R> {

    private R r;

    public Result() {
    }

    public R getR() {
        return r;
    }

    @Override
    public void accept(R r) {
        this.r = r;
    }

    @Override
    public void before() {

    }

    @Override
    public void end() {

    }
}
