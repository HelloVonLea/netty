package com.fisrt.ftl.v3;


import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 仿写netty的FastThreadLocal
 * 提供init(), get(), set(T), remove方法
 */
public class NettyFastThreadLocal<T> {
    //REMOVE_INDEX就是移除元素集合的下标
    //它是所有的FastThreadLocal所存集合所在下标
    private static final int REMOVE_INDEX = NettyInternalThreadLocalMap.nextIndex();
    //NettyFastThreadLocal在InternalMap的数组的下标
    private final int index;

    public NettyFastThreadLocal() {
        this.index = NettyInternalThreadLocalMap.nextIndex();
    }

    /**
     * 添加一个NettyFastThreadLocal到Set集合中
     *
     * @param map
     * @param tNettyFastThreadLocal
     */
    private static void addFastThreadLocalToRemoveSet(NettyInternalThreadLocalMap map, NettyFastThreadLocal<?> tNettyFastThreadLocal) {
        //获取REMOVE_INDEX位置的元素Object
        Object object = map.getIndexedObject(REMOVE_INDEX);
        //存放NettyFastThreadLocal的集合
        Set<NettyFastThreadLocal<?>> removeSet;
        //object为null或DEFAULT_VALUE都是还没初始化
        if (object == null || object == NettyInternalThreadLocalMap.DEFAULT_VALUE) {
            //创建一个IdentityHashMap(靠==比较key是否相同)
            //Question: 这里为啥要用IdentityHashMap?
            Map<NettyFastThreadLocal<?>, Boolean> identityHashMap = new IdentityHashMap<>();
            //把identityHashMap转换成一个set，方面签名说了，返回的set和源map，同序、同并发
            //Question: 为啥要map转set，而不是直接new set?
            //因为用的IdentityHashMap，所以需要这个方法，HashMap有对应的HashSet,TreeMap有对应的TreeSet
            //其实就是一个工厂方法，提供了Map对应的Set实现
            removeSet = Collections.newSetFromMap(identityHashMap);
            //此处就存在一个问题：它为啥不直接就来一个new Set();
            //还是要看你写的这个类的hashcode,说明你对HashSet了解的还不够，
            //同样对一个类的hashcode也了解的不够，不知道这个加了泛型的类会出现一样的hashcode的情况

            //数组对应位置设置
            map.setIndexedValue(REMOVE_INDEX, removeSet);
        } else {
            //Object对象已存在，强转。严谨点的话，应该判断类型，不是Set类型的，抛异常或者其它处理方法
            removeSet = (Set<NettyFastThreadLocal<?>>) object;
        }
        //Question：为啥要把NettyThreadLocal放入这个RemoveSet，它什么时候操作这个Set
        removeSet.add(tNettyFastThreadLocal);
    }

    /**
     * 从集合中移除NettyFastThreadLocal
     *
     * @param internalMap
     * @param tNettyFastThreadLocal
     */
    private static void removeNettyFastThreadLocalFromRemoveSet(NettyInternalThreadLocalMap internalMap, NettyFastThreadLocal<?> tNettyFastThreadLocal) {
        //获取REMOVE_INDEX位置的对象
        Object object = internalMap.getIndexedObject(REMOVE_INDEX);
        if (object == null || object == NettyInternalThreadLocalMap.DEFAULT_VALUE) {
            return;
        }
        //强转为Set，然后移除它
        Set<NettyFastThreadLocal<?>> removeSet = (Set<NettyFastThreadLocal<?>>) object;
        removeSet.remove(tNettyFastThreadLocal);
    }

    /**
     * 移除当前线程绑定的所有的NettyFastThreadLocal
     * 当在一个容器环境，你不想留下任何你不管理的thread local的变量
     * 时，他就是有用的
     * <p>
     * 此处，我在设计的时候，当时也考虑了很久，不知道写在NettyFastThreadLocal
     * 还是NettyInternalThreadLocalMap中，因为遇到一个是普通线程和
     * NettyFastThreadLocal线程问题，这样就不知道怎么清除了
     */
    public static void removeAll() {
        //先是getIfSet然后校验
        NettyInternalThreadLocalMap internalMap = NettyInternalThreadLocalMap.getIfSet();
        if (internalMap == null) {
            return;
        }

        try {
            //NettyFastThreadLocal每次生成都会放入这个Set集合中
            //这样就可以一次拿到，和我们想的遍历依次移除不一样
            Object obj = internalMap.getIndexedObject(REMOVE_INDEX);
            //校验
            if (obj != null && obj != NettyInternalThreadLocalMap.DEFAULT_VALUE) {
                Set<NettyFastThreadLocal<?>> removeSet = (Set<NettyFastThreadLocal<?>>) obj;
                //copy一份，这样就不存在竞争问题了，就是多用了点内存
                NettyFastThreadLocal[] removeArray = removeSet.toArray(new NettyFastThreadLocal[0]);
                for (NettyFastThreadLocal ftl : removeArray) {
                    //在netty的源码中是调的remove(InternalMap),就是少了一遍get
                    ftl.remove();
                }
            }
        } finally {
            //移除线程中的InternalMap  精细！！！我想不到。
            NettyInternalThreadLocalMap.remove();
        }
    }

    /**
     * 在netty中它做了4步，分别是
     * 1.拿到InternalThreadLocalMap, 对于FastThreadLocalThread中含有一个InternalThreadLocalMap，
     * 对于普通线程它是在static的ThreadLocal中存了一个InternalThreadLocalMap
     * 2.通过InternalThreadLocalMap找到下标为index的元素
     * 3.如果该Object不是UNSET，强转返回。（在第一版里面也需要强转）
     * 4.如果是UNSET说明还没有设置值，初始化，进行塞初始化值
     *
     * @return
     */
    public T get() {
        //任何线程都会拿到这么一个map
        NettyInternalThreadLocalMap map = NettyInternalThreadLocalMap.get();
        //从数组下标处取出这个元素
        Object value = map.getIndexedObject(index);
        if (value != NettyInternalThreadLocalMap.DEFAULT_VALUE) {
            return (T) value;
        }
        //是DEFAULT_VALUE时，初始化
        return initialize(map);
    }

    /**
     * 进行值的初始化
     * 1.initialValue 生成一个value
     * 2.在InternalMap中放置此值，也就是Object[]数组index位置
     * 3.添加到待移除的变量集合
     *
     * @param map
     * @return
     */
    private T initialize(NettyInternalThreadLocalMap map) {
        T t = null;
        try {
            //返回一个初始值的方法
            t = initialValue();
        } catch (Exception e) {
            //异常处理，netty有一套它的异常处理机制
            //留坑待研究
        }
        //放值
        map.setIndexedValue(index, t);
        //把当前FastThreadLocal放入待移除集合里
        addFastThreadLocalToRemoveSet(map, this);
        return t;
    }

    /**
     * 初始化值
     * protected的，说明子类可以重写这个方法
     * 使得它返回子类想要的初始化的值，
     * 如：PoolThreadLocalCache就重写后返回了PoolThreadCache类
     * 这里默认返回的是null，也是大多数用的时候可以返回的
     * 这么写是大多数人没想到的，可能在上一步就直接设值进去了
     * 主要你的代码没这个需求
     *
     * @return
     */
    protected T initialValue() {
        return null;
    }


    public void set(T t) {
        //这里对t这个值的判断，没看懂！什么情况下会出现这种情况？
        //而且还要移除，why?
        if (t == NettyInternalThreadLocalMap.DEFAULT_VALUE) {
            remove();
            return;
        }
        //获得到InternalMap
        NettyInternalThreadLocalMap internalMap = NettyInternalThreadLocalMap.get();
//        如果让我写，我就这么写了
//        internalMap.setIndexedValue(index, t);
        //下面是netty的写法
        //先放值，然后添加到待移除集合中
        //Question:若是initial是放了一边，现在又放一边？
        if (internalMap.setIndexedValue(index, t)) {
            addFastThreadLocalToRemoveSet(internalMap, this);
        }
    }

    /**
     * 移除缓存的元素
     */
    public void remove() {
        //这里是调用的internalMap的getIfSet方法，
        //而本类中的get()、set()方法都调的map中的get()方法
        //还有这个名字也很奇怪叫getIfSet，为啥不是getIfExist?
        NettyInternalThreadLocalMap internalMap = NettyInternalThreadLocalMap.getIfSet();

        //首先对这个map进行了校验，严谨！
        if (internalMap == null) return;
        //InternalMap移除该下标对应的元素
        Object value = internalMap.removeIndexedValue(index);

        //接下来netty还做了两件事
        //1.从removeSet中移除当前NettyFastThreadLocal
        removeNettyFastThreadLocalFromRemoveSet(internalMap, this);
        //2.如果不是默认值，进行资源清理
        if (value != NettyInternalThreadLocalMap.DEFAULT_VALUE) {
            try {
                onRemoval((T) value);
            } catch (Exception e) {
                //netty统一异常处理
            }
        }
    }

    /**
     * 当通过remove()方法移除当前NettyFastThreadLocal对象是执行此方法
     * 应当注意的是，当线程完成后并不保证会调用remove()方法，
     * 也就是说，在这种情况下，你不能依靠这个进行资源的清理
     * <p>
     * 可以看到这个是protected方法，而且在这里没有实现，在子类有
     * 如：在PoolThreadLocalCache中就进行了资源的释放
     * 它是缓存时就牵扯到内存的问题。
     *
     * @param value
     */
    protected void onRemoval(T value) {
    }


}
