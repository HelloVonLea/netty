package com.fisrt.ftl;

import java.util.Objects;

/**
 * 包装Runnable
 * 使得MyFastThreadLocal可以自动移除
 * Wrap <code>Runnable</code> which makes us to remove
 * thread local variable automatically.
 */
public class MyFastThreadLocalRunnable implements Runnable {
    private Runnable target;

    public MyFastThreadLocalRunnable(Runnable target) {
        this.target = Objects.requireNonNull(target, "Runnable can't be null");
    }

    @Override
    public void run() {
        try {
            target.run();
        } finally {
            MyFastThreadLocal.removeAll();
        }
    }
}
