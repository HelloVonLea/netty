package com.fisrt.ftl.v1;

/**
 * 含有MyInternalThreadLocalMap的线程
 * A special thread that contains a <code>MyInternalThreadLocalMap</code>
 */
public class MyFastThreadLocalThread extends Thread {
    //final的防止别人修改，提供获取的方法
    private final MyInternalThreadLocalMap threadLocalMap;

    public MyFastThreadLocalThread(Runnable target) {
        super(new MyFastThreadLocalRunnable(target));
        this.threadLocalMap = new MyInternalThreadLocalMap();
    }

    public MyFastThreadLocalThread(Runnable target, String name) {
        super(new MyFastThreadLocalRunnable(target), name);
        this.threadLocalMap = new MyInternalThreadLocalMap();
    }

    public MyInternalThreadLocalMap threadLocalMap() {
        return this.threadLocalMap;
    }

}
