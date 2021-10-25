package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.util.UnsafeLongArrayUtil;
import com.fisrt.queue.util.UnsafeRefArrayUtil;
import com.fisrt.queue.v1.Queue;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.util.Pow2;
import org.jctools.util.RangeUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 循环数组实现的无锁队列
 *
 * <p>
 * 在并发情况下，我们会遇到包括不限于这些问题：
 * 1.如何防止多个线程覆盖写一个元素？
 * 2.如何防止读到还未写的元素？
 * 3.size()如何统计？
 * 4.队列满或空如何判断？
 * <p>
 * <p>
 * 我们采用jcTools中的MpmcArrayQueue的原理。
 * 1.我们用CAS来保证原子性。
 * 2.数组大小都是2的幂次，双指针，一生产者一消费者。
 * 3.多了一个long数组----sequenceBuffer
 * 这是怎么联动的呢？
 * 生产者指针pIndex是一直自增的，相当于AtomicLong
 * 消费者指针cIndex是一直自增的，相当于AtomicLong
 * 这保证了每个线程拿到的指针都是唯一的
 * index和capacity取余会映射出对应的下标。
 * 假设我们的capacity是4，index就会在[0,1,2,3]这个集合中。
 * sequenceBuffer中存的是什么呢？
 * 会存pIndex+1和cIndex+capacity。下标就是指针对应的位置。
 * 这么存有什么作用呢？
 * 假如有竞争，某个线程取出pIndex=4,下标=0，此时看seq。
 * seq==pIndex,说明sequenceBuffer中0位置的元素为4，0位置处元素被消费了，
 * 可以放！先改掉pIndex，这样别的线程就可感知，之后再放。
 * seq<pIndex,说明因为pIndex=4,若小，肯定是seq=1，之前pIndex=0放的元素没有被消费。
 * 有可能是满了，也有可能不满，不满说明，cIndex改了，但是seq还没更改，再走一遍循环。
 * seq>pIndex,说明seq>4，seq=5或8，seq=5说明另一个线程放进去了，8说明别的线程放了，
 * 还有别的线程消费了，此线程饿的太久了，pIndex过期了，再循环拿新的。
 * 假设上面的线程A取出pIndex=4时,另一个线程B取出pIndex=8（起了很多线程）,下标0，看seq。
 * 此时的seq只会比pIndex=8小，因为为8放入的是9（B还没放），之前的A为4放入5，消费者放4或8，
 * 当你线程多的时候，pIndex只会不断变大。而seq都是有规律的，值是可预算的。
 * 这样就保证了多个线程不会覆盖一个位置的写。
 * pIndex变大后，seq小，不进行写，进自旋。
 * <p>
 * 双指针自增就保证了可以用pIndex-cIndex=size，一条射线。
 * 这样用pIndex-cIndex和capacity判断就可以知道是不是满了，
 * 而空了就是pIndex==cIndex。现在明白为啥用long了吧。
 *
 *
 *
 *
 * </p>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue13<E> implements Queue<E> {

    private static final Unsafe UNSAFE = getUnsafe();
    protected static final long P_INDEX_OFFSET = fieldOffset(ArrayQueue13.class, "producerIndex");
    //Question:为啥不用AtomicLong这个来做？
    //Answer:你需要自己循环做事，故不能。
    protected final static long C_INDEX_OFFSET = fieldOffset(ArrayQueue13.class, "consumerIndex");
    protected final long[] sequenceBuffer;
    private final long mask;
    protected volatile long producerIndex;
    //消费者index
    protected volatile long consumerIndex;
    private Object[] buffer;


    public ArrayQueue13(int capacity) {
        //保证容量比2大
        int cap = RangeUtil.checkGreaterThanOrEqual(capacity, 2, "capacity");
        //把容量变成2的幂次
        int actualCapacity = Pow2.roundToPowerOfTwo(cap);
        mask = actualCapacity - 1;
        buffer = new Object[actualCapacity];
        sequenceBuffer = new long[actualCapacity];
        for (int i = 0; i < actualCapacity; i++) {
            //计算i应该存储的内存位置
            long offset = UnsafeLongArrayUtil.calcCircularLongElementOffset(i, mask);
            //把元素放进去，
            UNSAFE.putOrderedLong(sequenceBuffer, offset, i);
        }
        System.out.println("初始时sequenceBuffer是：" + Arrays.toString(sequenceBuffer));
    }

    /**
     * 由于Java中Unsafe在外部由其静态方法获得，
     * 故使用反射大法。
     *
     * @return
     */
    private static Unsafe getUnsafe() {
        Unsafe unsafe;
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                Constructor<Unsafe> constructor = Unsafe.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                unsafe = constructor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return unsafe;
    }

    /**
     * 返回field的内存起始地址
     *
     * @param clz
     * @param fieldName
     * @return
     */
    public static long fieldOffset(Class clz, String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(clz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
//        ArrayQueue11<Integer> queue = new ArrayQueue11<>(4);
//        queue.put(1);

        int n = 10;
//        test1(n);
//        test2(n);
//        test3(n);
//        Queue<String> queue = new ArrayQueue13<>(4);
        MpmcArrayQueue<String> jcQueue = new MpmcArrayQueue<>(4);
        for (int i = 0; i < n; i++) {
            final int x = i;
            new Thread(() -> {
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((x+"-"+j));
                    while (!jcQueue.offer(x + "-" + j)) {
                        try {
                            Thread.sleep(1_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 20_000) {
//                    String obj = queue.dequeue();
                    String obj = jcQueue.poll();
                    if (obj != null) System.out.println(obj);
                }
            }).start();

        }

    }

    /**
     * @param n
     * @throws InterruptedException
     */
    private static void test3(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue13<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(n + n + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);

        for (int i = 0; i < n; i++) {
//            int finalI = i;
            new Thread(() -> {
//                int x= finalI;
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((j + 1)*x);
                    queue.enqueue((j + 1));
                }
                closePhaser.arrive();
            }).start();

        }
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                startLatch.awaitAdvance(0);
                int c = 0;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 20_000) {
                    Integer num = queue.dequeue();
                    if (num != null) {
                        count.getAndIncrement();
//                    System.out.println("数据：" + num);
                    } else {
                        c++;
                    }
                }

//            System.out.println("c="+c);
                closePhaser.arrive();
            }).start();

        }

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }

    @Override
    public void enqueue(E e) {
        while (!offer(e)) {
            try {
                Thread.sleep(1_00);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    public boolean offer(E e) {
        if (e == null) {
            return false;
        }
        //mask时final修饰的成员变量，如果我们直接用this.mask，jvm在执行时，
        //由于final不可变，jvm不能确定这个变量是否改变了，故它每次从内存中加载一次
        //赋给成员变量后，jvm使用局部变量，jvm就可以将该变量放入CPU缓存中，提高了速度
        final long mask = this.mask;
        final long capacity = mask + 1;
        final long[] sBuffer = sequenceBuffer;
        long pIndex;
        long seqOffset;
        long seq;//sequence下标
        long cIndex = Long.MIN_VALUE;
        do {
            pIndex = producerIndex;//从主存中加载一次
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(pIndex, mask);
            seq = UNSAFE.getLongVolatile(sequenceBuffer, seqOffset);
//            System.out.println(Thread.currentThread().getName()+"\tseq="+seq+"\t pIndex="+pIndex+"\tseqOffset="+seqOffset);
            //seq和pIndex的关系
            //seq==pIndex ==> long[pIndex]=pIndex
            if (seq < pIndex) {
                //再检查一次，确保当队列满了的时候返回false
                if (pIndex - capacity >= cIndex &&//判断缓存中的cIndex
                        pIndex - capacity >= (cIndex = this.consumerIndex)) {//判断最新的cIndex
//                    System.out.println("满了");
                    return false;
                } else {//没满生产者指针进1位
                    seq = pIndex + 1;
                }
            }
        } while (seq > pIndex ||
                !UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, pIndex, pIndex + 1));
//        System.out.println(Thread.currentThread().getName()+"\tpIndex="+(pIndex&capacity));
        UNSAFE.putObject(buffer, UnsafeRefArrayUtil.calcCircularRefElementOffset(pIndex, mask), e);
        //seq++
        UNSAFE.putOrderedLong(sBuffer, seqOffset, pIndex + 1);
//        System.out.println(Thread.currentThread().getName()+"放入元素后sequenceBuffer是："+Arrays.toString(sequenceBuffer));
//        System.out.println(Thread.currentThread().getName()+"\tpIndex="+pIndex+"\t seq="+seq+"\t放入元素["+e+"]后sequenceBuffer是："+Arrays.toString(sequenceBuffer));
//        System.out.println("pIndex="+pIndex);
        return true;
    }

    @Override
    public E dequeue() {
        //避免了在volatile读后重复的加载
        final long[] sBuffer = sequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seq;
        long seqOffset;
        long expectedSeq;
        long pIndex = -1;

        do {
            cIndex = this.consumerIndex;
            //拿到sBuffer中cIndex内存地址
            seqOffset = UnsafeLongArrayUtil.calcCircularLongElementOffset(cIndex, mask);
            //获取seq
            seq = UNSAFE.getLongVolatile(sBuffer, seqOffset);
            expectedSeq = cIndex + 1;//期待的seq就是放入线程放入那时的pIndex+1。
            //如果一放一拿，期待的seq正好和当前seq相等。cas
            //如果拿的时候没元素(初始时没放或消费时没放元素)造成期待的seq>当前seq，即seq<expectedSeq，
            // 可能空，不是空就进入下一个循环，自旋。
            //如果被另一个消费者给抢了，期待的seq<seq，即seq>expected。如，cIndex=4,期待seq=5,但拿出8，说明另一个线程抢了。或者此线程饿的太厉害了。
            //如果恰好另一个消费者消费了，
            //没有竞争的情况下，小的进入if，大或等的话说明另一个消费者改了，直接下一个循环
            if (seq < expectedSeq) {
                //槽位没有被生产者移动
                if (cIndex >= pIndex &&//测试缓存的pIndex 这里测试的意义是？可能是下次循环的pIndex值
                        cIndex == (pIndex = this.producerIndex))//说明队列空了，该生产了。
                    //Question：循环数组判断满或空，cIndex==pIndex可能满也可能空哇？
                    return null;
                else
                    seq = expectedSeq + 1;//seq是cIndex移了2位
            }
        } while (seq > expectedSeq ||//说明其它消费者消费了，
                !UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, cIndex, cIndex + 1));//cas改cIndex的值
        //上面的什么时候终止循环，seq==expectedSeq或cas==true
        //cIndex的在Object数组中的内存位置
        final long offset = UnsafeRefArrayUtil.calcCircularRefElementOffset(cIndex, mask);
        //获取元素
        final E e = (E) UNSAFE.getObject(buffer, cIndex);
        //将数组该处的元素置为null
        UNSAFE.putObject(buffer, offset, null);
        //将long数组中seqOffset处元素置为cIndex+capacity
        //也就是long[cIndex]=cIndex+capacity
        //i.e. seq+=capacity
        UNSAFE.putOrderedLong(sBuffer, seqOffset, cIndex + mask + 1);
//        System.out.println("消费者消费"+seqOffset+"\t x="+(cIndex+mask+1)+"\t seq="+seq);
//        System.out.println(Thread.currentThread().getName()+"\tcIndex="+cIndex+"\t seq="+seq+"\t消费元素["+e+"]后sequenceBuffer是："+Arrays.toString(sBuffer));

        return e;
    }

    @Override
    public int size() {
        long after = this.consumerIndex;
        long size;
        while (true) {
            final long before = after;
            final long currentProducerIndex = this.producerIndex;
            after = this.consumerIndex;
            //两次消费者指针未变化时，计算size，否则自旋。
            //Why?因为在并发情况下这两次行为之间，若是消费了，size就变大了。
            if (before == after) {
                size = currentProducerIndex - after;
                break;
            }
        }
        //size是long型的
        // Long overflow is impossible (), so size is always positive. Integer overflow is possible for the unbounded
        // indexed queues.
        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
            // concurrent updates to cIndex and pIndex may lag behind other progress enablers (e.g. FastFlow), so we need
            // to check bound
        else if (size < 0)
            return 0;
        else if (capacity() != -1 && size > capacity())//-1表示无界容量
            return capacity();
        else
            return (int) size;
    }

    /**
     * 获取容量
     *
     * @return
     */
    public int capacity() {
        return (int) (this.mask + 1);
    }


}
