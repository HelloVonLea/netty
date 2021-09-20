package com.fisrt.ftl.v2;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * final 修饰的，不可被继承
 */
public final class NettyInternalThreadLocalMap {
    //Object数组填充的默认值
    public static final Object DEFAULT_VALUE = new Object();
    //下标生成器
    private static final AtomicInteger INDEX_UPDATER = new AtomicInteger(0);
    //当是普通线程时，存放变量的thread local
    //注意它的思想是ThreadLocal中存放Map，而我们的是map放ThreadLocal
    private static final ThreadLocal<NettyInternalThreadLocalMap> THREAD_LOCAL = new ThreadLocal<>();
    // Cache line padding (must be public)
    // With CompressedOops enabled, an instance of this class should occupy at least 128 bytes.
    public long rp1, rp2, rp3, rp4, rp5, rp6, rp7, rp8, rp9;
    //存放变量的数组，一个NettyFastThreadLocal一个位置
    private Object[] values;

    /**
     * 产生index的函数
     *
     * @return 下标，一个int型的值
     */
    public static int nextIndex() {
        AtomicInteger update = INDEX_UPDATER;
        int index = update.getAndIncrement();
        //这里是我们写的
//        if (index >= Integer.MAX_VALUE) {
//            INDEX_UPDATER.set(0);
//            index = 0;
//        }
        //这是netty写的
        if (index < 0) {
            update.decrementAndGet();
            throw new IllegalStateException("too many thread-local indexed variables");
        }
        //两者的区别是什么？
        //我们知道Integer的MAX_VALUE加1后变为负数，故netty这么写
        //我写的就是重置想循环利用，可能会出问题。一般不会写这么多ThreadLocal的
        return index;
    }

    /**
     * 获取NettyInternalThreadLocalMap的静态方法
     * <p>
     * 当这个map为null的时候，创建新的InternalMap
     *
     * @return 内部Map
     */
    public static NettyInternalThreadLocalMap get() {
        Thread currentThread = Thread.currentThread();
        //首先，对当前线程类型进行判断
        if (currentThread instanceof NettyFastThreadLocalThread) {
            NettyFastThreadLocalThread thread = (NettyFastThreadLocalThread) currentThread;
            NettyInternalThreadLocalMap map = thread.internalThreadLocalMap();
            //懒加载，有需要才进行map的创建，避免了浪费
            if (map == null) {
                thread.setInternalThreadLocalMap(map = new NettyInternalThreadLocalMap());
            }
            return map;
        }
        ThreadLocal<NettyInternalThreadLocalMap> threadLocal = THREAD_LOCAL;
        NettyInternalThreadLocalMap map = threadLocal.get();
        if (map == null) {
            threadLocal.set(map = new NettyInternalThreadLocalMap());
        }
        return map;
    }

    /**
     * 获取NettyInternalThreadLocalMap的静态方法，
     * 如果存在的话返回map，不存在就是null
     * 与get()方法不同的是，map为null时，它不创建对象
     *
     * @return 内部Map
     */
    public static NettyInternalThreadLocalMap getIfSet() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof NettyFastThreadLocalThread) {
            return ((NettyFastThreadLocalThread) currentThread).internalThreadLocalMap();
        }
        return THREAD_LOCAL.get();
    }

    /**
     * 移除InternalMap
     * 最初，我以为是移除元素的
     */
    public static void remove() {
        Thread thread = Thread.currentThread();
        if (thread instanceof NettyFastThreadLocalThread) {
            NettyFastThreadLocalThread fastThreadLocalThread = (NettyFastThreadLocalThread) thread;
            //setter方法的另一个好处
            fastThreadLocalThread.setInternalThreadLocalMap(null);
        } else {
            THREAD_LOCAL.remove();
        }
    }

    /**
     * 获取index位置的元素
     *
     * @param index 下标
     * @return 元素
     */
    public Object getIndexedObject(int index) {
        Object[] lookup = this.values;
        //这里和我想的不一样，我的对index进行了一个校验，不合法抛异常
        //netty是返回了一个默认值回去，考量不同
        return index < lookup.length ? lookup[index] : DEFAULT_VALUE;
    }

    /**
     * 在object数组中添加新值T
     * 涉及到数组的扩容
     *
     * @param index 下标
     * @param t     元素
     * @param <T>   泛型
     * @return Boolean结果
     */
    public <T> boolean setIndexedValue(int index, T t) {
        Object[] old = this.values;
        //在数组长度内，
        if (index < old.length) {
            //我写的,这里采用我自己写的
            old[index] = t;
            return true;
//            //netty写的
//            Object oldValue = old[index];
//            old[index] = t;
//            //这里为true的时候是第一次set值，
//            // 第二次set值就为false了，难道不是添加成功？
//            //netty说当且仅当添创建一个新的线程本地的变量放这时才返回true
//            //你牛！
//            return oldValue == DEFAULT_VALUE;
        }
        //我的扩容
//        int oldLen = old.length;
//        int newLen = oldLen << 1;
//        if (newLen < 0) {
//            //太长了，不让放
//            return false;
//        }
//        Object[] newValues = new Object[newLen];
//        System.arraycopy(old, 0, newValues, 0, old.length);
//        newValues[index] = t;
//        values = newValues;

        //netty的
        int oldCapacity = old.length;//我设置的是32，就假设是32
        int newCapacity = index;//此时的index为32 =10 0000
        newCapacity |= newCapacity >>> 1;//11 0000
        newCapacity |= newCapacity >>> 2;//11 1100
        newCapacity |= newCapacity >>> 4;//11 1111
        newCapacity |= newCapacity >>> 8;//11 1111
        newCapacity |= newCapacity >>> 16;//运算得63
        newCapacity++;//++变64
        //整体扩容流程就是  原容量的2倍，同hashmap但是它缺校验
        if (newCapacity < 0 || newCapacity >= Integer.MAX_VALUE) {
            throw new IllegalStateException("no more space!");
        }
        //Question：为啥不是直接左移1位，
        // 这里计算的时候考虑到了初始的capacity不是2的幂次的情况
        //如17，通过上面的方法，计算可以得到32，如果是直接左移1位的话就是34.
        //其实它还是通过一个数找到它最近的2的幂次的数，高端！
        Object[] newArray = Arrays.copyOf(old, newCapacity);//把旧数组复制到新数组
        Arrays.fill(newArray, oldCapacity, newCapacity, DEFAULT_VALUE);//空白填充默认值
        newArray[index] = t;
        values = newArray;
        return true;
    }

    /**
     * 移除index位置的元素
     *
     * @param index 下标
     * @return 被移除的元素
     */
    public Object removeIndexedValue(int index) {
        Object[] values = this.values;
        if (index < values.length) {
            Object value = values[index];
            values[index] = DEFAULT_VALUE;
            return value;
        }
        return null;//这里netty返回的是DEFAULT_VALUE
    }

}
