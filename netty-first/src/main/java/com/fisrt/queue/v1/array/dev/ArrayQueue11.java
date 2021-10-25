package com.fisrt.queue.v1.array.dev;

import com.fisrt.queue.v1.Queue;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 循环数组实现的队列
 * 无锁队列
 * <p>
 * 使用Unsafe进行操作。
 * <p>
 * <p>
 * 我们发现并不能解决size和elements不匹配的问题。
 * 即使添加上volatile后，还是出现
 * <code>
 * 队列满了	 数组=[6, 7, null, null]
 * 队列满了	数组=[null, null, null, null]
 * </code>
 * 从代码上我们也能发现，这两者是没有同步的。
 * 而且测试的时候，发现我们的代码会引起CPU急速升高，甚至到100%的情况。
 * 我们在ArrayQueue12中改正。
 * </p>
 *
 * @since 2021/10/11 15:52
 */
@Deprecated
public class ArrayQueue11<E> implements Queue<E> {

    private volatile Object[] elements;
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger putIndex = new AtomicInteger(0);
    private AtomicInteger takeIndex = new AtomicInteger(0);

    private Unsafe unsafe;

    public ArrayQueue11(int capacity) {
        elements = new Object[capacity];
        try {
            unsafe = getUnsafe();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
//        ArrayQueue11<Integer> queue = new ArrayQueue11<>(4);
//        queue.put(1);

        int n = 10;
//        test1(n);
        test2(n);
//        test3(n);

    }

    /**
     * 多生产者-多消费者
     * 我们先不管之前的并发问题，来验证ABA问题。
     * 期待有N*N个数取出。
     * <p>
     * 实际：代码直接卡住了。得！测不了。
     * 实际：拿出得个数正确，ABA的问题没有复现，
     * 但是，CPU直接100%了，把咱的系统差点干崩了。
     * ABA的问题先放一边，咱们先解决并发问题。
     * <p>
     * 目前，并发的问题在于，size和数组不匹配。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void test3(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue11<>(4);
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

    /**
     * 多生产者-单消费者
     * N个生产者，放N*N个数据，看能取出来多少个。
     * 猜想：可能会出错，这里面有ABA的问题，还有几个成员变量不是原子操作的问题
     * 期望：N*N，实际：
     * <p>
     * 为了观察，我们先把ABA的问题解决掉，很简单，放入的数值不一样就OK
     * 经测试，我们就发现
     * <code>
     * 队列满了	 数组=[3, null, 5, 6]
     * </code>
     * 很明显，原子变量组合到一起不加锁就不正确了。
     * <p>
     * 再说ABA的问题，在一个index出，可能生产者1把null->e，
     * 消费者1把e->null,生产者2拿到的是null，一下null->e了，我们在test3中演示。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue11<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(n + 1 + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);
        //n=4;
        //线程0，放1,2,3,4
        //线程1，放10,20,30,40
        //线程2，放100,200,300,400
        //线程3，放1000,2000,3000,4000
        for (int i = 0; i < n; i++) {
            int finalI = i;
            new Thread(() -> {
                int x = finalI;
                //当我们把这段代码注释掉，然后4个线程都放1,2,3,4，ABA的问题就来了
                //给N，只要少于N*N就是发生了ABA的问题
//                if(x==0){
//                    x=1;
//                }else if(x==1){
//                    x=10;
//                }else if(x==2){
//                    x=100;
//                }else if(x==3){
//                    x=1000;
//                }
                startLatch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
//                    queue.enqueue((j + 1)*x);
                    queue.enqueue((j + 1));
                }
                closePhaser.arrive();
            }).start();

        }
        AtomicInteger count = new AtomicInteger();
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

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }

    /**
     * 单生产者-单消费者
     * 放N个数据，看能取出来多少个。
     * 期望：N，实际：N
     *
     * @param n
     * @throws InterruptedException
     */
    private static void test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue11<>(4);
        //子线程结束后，main线程再结束控制器
        Phaser closePhaser = new Phaser(1 + 1 + 1);
        //latch控制同时开始
        Phaser startLatch = new Phaser(1);
        new Thread(() -> {
            startLatch.awaitAdvance(0);
            for (int j = 0; j < n; j++) {
                queue.enqueue(j + 1);
            }
            closePhaser.arrive();
        }).start();
        AtomicInteger count = new AtomicInteger();
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

        Thread.sleep(3_000);
        startLatch.arrive();
        closePhaser.arriveAndAwaitAdvance();
        System.out.println("总共拿到" + count.get() + "个数据");

    }

    @Override
    public void enqueue(E e) {
        Object[] elements = this.elements;
        do {
            //队列满，自旋
            while (size.get() == elements.length) {
                System.out.println("队列满了\t 数组=" + Arrays.toString(elements));
                Thread.yield();
            }
        } while (!compareAndSwap(elements, putIndex.get(), null, e));
        if (putIndex.incrementAndGet() == elements.length)
            putIndex.set(0);
        size.getAndIncrement();
    }

    @Override
    public E dequeue() {
        Object[] elements = this.elements;
        int index = 0;
        Object e;
        do {
            //队列空判断
            long start = System.currentTimeMillis();
            while (size.get() == 0) {
                //超过1秒，放弃
                if (System.currentTimeMillis() - start > 1_000)
                    return null;
                //空了，自旋
                System.out.println("队列空了\t数组=" + Arrays.toString(elements));
                Thread.yield();
            }
            index = takeIndex.get();
            e = elements[index];
//            if (e != null)
//                System.out.println("取出数据：" + e + "\t index=" + index + "\t ");
        } while (e == null || !compareAndSwap(elements, index, e, null));

        if (takeIndex.incrementAndGet() == elements.length)
            takeIndex.set(0);
        size.decrementAndGet();
        return (E) e;

    }

    @Override
    public int size() {
        return this.size.get();
    }

    private boolean compareAndSwap(Object o, int index, Object expected, Object x) {
        Unsafe unsafe = this.unsafe;
        long valueOffset = calValueOffset(unsafe, index);
        return unsafe.compareAndSwapObject(elements, valueOffset, expected, x);
    }

    public void put(E e) throws Exception {
        Unsafe unsafe = getUnsafe();
        for (int i = 0; i < elements.length; i++) {
            long valueOffset = calValueOffset(unsafe, i);
            boolean b = unsafe.compareAndSwapObject(elements, valueOffset, null, i + 1);
            System.out.println(b);

        }
        System.out.println(Arrays.toString(elements));
    }

    private long calValueOffset(Unsafe unsafe, int index) {
        //获取数组的基础起始位置
        int arrayBaseOffset = unsafe.arrayBaseOffset(Object[].class);
        //获取下标的范围
        int arrayIndexScale = unsafe.arrayIndexScale(Object[].class);
        //计算偏移量 base+单个大小*index
        long valueOffset = arrayBaseOffset + arrayIndexScale * index;
        return valueOffset;
    }

    /**
     * 获取Unsafe
     * 由于Unsafe不能直接获得，可以通过反射获得。
     *
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        //Unsafe被设计成只能从引导类加载器（bootstarp class loader）加载，
        // 如果不是从启动类加载器直接调用getUnsafe方法则会抛出异常。
//    Unsafe unsafe=Unsafe.getUnsafe();
        //多说一句，通过追踪源码，你会发现classLoader==null得判断
        //这个时候你就会卡壳了。不知道原因是啥。若是你能自己追踪出原因，
        //你就掌握了更多得东西。
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return unsafe;
    }


}
