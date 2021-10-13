package com.fisrt.ftl.v3;

/**
 * 含有InternalThreadLocalMap的线程
 * <p>
 * 在netty中有一个cleanupFastThreadLocals标识，当一个Runnable被包装后就置为true
 * 目前不清楚它这么写的用意是什么
 * 在我们的例子中，直接在构造函数中new出来，这其实是饥饿加载
 * 而netty是在获取map时，发现internalMap为空，再new，懒加载，而且避免了
 * 如果InternalMap没有被使用，避免了内存的浪费
 * 提供一个获取internalMap的方法
 * 提供一个setter方法，既用于放对象，也可用于remove的时候清除map对象
 */
public class NettyFastThreadLocalThread extends Thread {
    //线程持有的InternalMap对象，在netty中它竟然是可以set的，而不是构造时生成的
    private NettyInternalThreadLocalMap internalThreadLocalMap;

    public NettyFastThreadLocalThread() {
        super();
//        internalThreadLocalMap=new NettyInternalThreadLocalMap();
    }

    /**
     * 重写了Thread的构造方法，对Runnable进行了包装
     *
     * @param target
     */
    public NettyFastThreadLocalThread(Runnable target) {
        super(NettyFastThreadLocalRunnable.wrap(target));
//        internalThreadLocalMap=new NettyInternalThreadLocalMap();
    }

    public NettyFastThreadLocalThread(Runnable target, String name) {
        super(target, name);
//        internalThreadLocalMap=new NettyInternalThreadLocalMap();
    }

    public NettyInternalThreadLocalMap internalThreadLocalMap() {
        return internalThreadLocalMap;
    }

    public void setInternalThreadLocalMap(NettyInternalThreadLocalMap internalThreadLocalMap) {
        this.internalThreadLocalMap = internalThreadLocalMap;
    }
}
