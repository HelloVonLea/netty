package com.fisrt.ftl.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仿FastThreadLocal
 * 1.同ThreadLocal有泛型
 * 2.同ThreadLocal，有构造、get、set、remove，
 * 另外多一个removeAll的静态方法
 */
public class MyFastThreadLocal<T> {
    //所有MyFastThreadLocal存放的index
//    private static final int REMOVE_INDEX = MyInternalThreadLocalMap.nextIndex();

    private static final Map<Thread, ThreadLocal<?>> CACHE = new ConcurrentHashMap<>();
    //当前MyFastThreadLocal所在下标
    private final int index;

    /**
     * 构造函数
     * 当一个MyFastThreadLocal创建之后它有一个确定的下标
     */
    public MyFastThreadLocal() {
        this.index = MyInternalThreadLocalMap.nextIndex();
    }

    public static void removeAll() {
        Thread thread = Thread.currentThread();
        if (thread instanceof MyFastThreadLocalThread) {
            MyFastThreadLocalThread curr = (MyFastThreadLocalThread) thread;
            MyInternalThreadLocalMap threadMap = curr.threadLocalMap();
            threadMap.removeAll();
        } else {//普通线程
            //这里就有问题，一个线程结束后，不能移除其它线程的value
            CACHE.forEach((k, v) -> v.remove());
            CACHE.clear();
        }
    }

    public T get() {
        Thread thread = Thread.currentThread();
        if (thread instanceof MyFastThreadLocalThread) {
            MyInternalThreadLocalMap map = ((MyFastThreadLocalThread) thread).threadLocalMap();
            return map.getIndexedObject(index);
        }
        ThreadLocal<?> threadLocal = CACHE.get(thread);
        if (threadLocal != null)
            //没有做类型校验
            return (T) threadLocal.get();
        return null;
    }

    public void set(T t) {
        Thread thread = Thread.currentThread();
        if (thread instanceof MyFastThreadLocalThread) {
            MyFastThreadLocalThread curr = (MyFastThreadLocalThread) thread;
            MyInternalThreadLocalMap threadMap = curr.threadLocalMap();
            threadMap.set(t, index);
        } else {
            ThreadLocal threadLocal = CACHE.get(thread);
            if (threadLocal == null) {
                threadLocal = new ThreadLocal();
                CACHE.putIfAbsent(thread, threadLocal);
            }
            threadLocal.set(t);
        }
    }

    public void remove() {
        Thread thread = Thread.currentThread();
        if (thread instanceof MyFastThreadLocalThread) {
            MyFastThreadLocalThread curr = (MyFastThreadLocalThread) thread;
            MyInternalThreadLocalMap threadMap = curr.threadLocalMap();
            threadMap.remove(index);
        } else {//普通线程
            CACHE.remove(thread);
        }
    }

}
