package com.fisrt.ftl.v3;

import java.util.Objects;

/**
 * 包装过的runnable
 * 主要有两点功能
 * 1.通过包装，为NettyFastThreadLocal提供了自动清除功能
 * 2.提供了static方法把一个普通Runnable包装成NettyFastThreadLocalRunnable
 */
public class NettyFastThreadLocalRunnable implements Runnable {
    //final修饰的，咱们写会漏掉这个
    private final Runnable runnable;

    //构造的时候多了一层校验
    public NettyFastThreadLocalRunnable(Runnable runnable) {
        this.runnable = Objects.requireNonNull(runnable, "Runnable required.");
    }

    /**
     * 静态方法--包装runnable
     *
     * @param runnable
     * @return
     */
    static Runnable wrap(Runnable runnable) {
        //这个判断是我没想到的，一是严谨，
        //二是什么时候会传NettyFastThreadLocal
        if (runnable instanceof NettyFastThreadLocal)
            return runnable;
        return new NettyFastThreadLocalRunnable(runnable);
    }

    @Override
    public void run() {
        //这么一包装就具备了自动清除功能，比ThreadLocal好多了
        try {
            runnable.run();
        } finally {
            //竟然是NettyFastThreadLocal的removeAll，
            // 我在写的时候想了半天，写在了InternalMap中
            NettyFastThreadLocal.removeAll();
        }
    }

}
