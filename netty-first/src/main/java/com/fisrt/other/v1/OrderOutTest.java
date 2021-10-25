package com.fisrt.other.v1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 最近在网上看到一个题：
 * 用两个线程，一个输出数字，一个输出字母，交替输出 1A2B3C4D...26Z
 *
 * @since 2021/9/22 17:13
 */
public class OrderOutTest {

    private static Thread t1, t2;

    /**
     * 任务：
     * 用两个线程，一个输出数字，一个输出字母，交替输出 1A2B3C4D...26Z
     *
     * @param args
     */
    public static void main(String[] args) {
//       method_synchronized();
//        method_lock();
//        method_lockSupport();
//        method_cas();
//        method_transferReference();
        method_blockingQueue();

    }

    /**
     * 这其实是生产-消费问题
     */
    private static void method_blockingQueue() {
        BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(1);
        Thread t1 = new Thread(() -> {
            int i = 1;
            try {
                while (i < 27) {
                    System.out.print(i + " ");
                    int take = blockingQueue.take();
                    System.out.print((char) take + " ");
                    i++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            int i = 65;//A
            try {
                while (i < 91) {
                    blockingQueue.put(i);
                    i++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t2");
        t1.start();
        t2.start();
    }

    private static void method_transferReference() {
        TransferQueue<Integer> transferQueue = new LinkedTransferQueue<>();
        Thread t1 = new Thread(() -> {
            int i = 1;
            try {
                while (i < 27) {
                    transferQueue.transfer(i);
                    int take = transferQueue.take();
                    System.out.print((char) take + " ");
                    i++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            int i = 65;//A
            try {
                while (i < 91) {
                    Integer take = transferQueue.take();
                    System.out.print(take + " ");
                    transferQueue.transfer(i);
                    i++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t2");
        t1.start();
        t2.start();
    }

    /**
     * 利用自旋原理进行操作，这个比较难想到
     * 能想到的都是高手！！！
     * 有cas+enum,cas+AtomicInteger,cas+AtomicReference
     */
    private static void method_cas() {
        final AtomicInteger control = new AtomicInteger(1);
        Thread t1 = new Thread(() -> {
            int i = 1;
            while (i < 27) {
                while (control.get() != 1) {
                }//CAS自旋
                System.out.print(i + " ");
                i++;
                control.set(2);
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            int i = 65;//A
            while (i < 91) {
                while (control.get() != 2) {
                }
                System.out.print((char) i + " ");
                i++;
                control.set(1);
            }
        }, "t2");
        t1.start();
        t2.start();
    }

    /**
     * 这种方式需要使用static的thread变量。
     */
    private static void method_lockSupport() {
        //        Thread t1, t2 ;
        t1 = new Thread(() -> {
            int i = 1;
            while (i < 27) {
                System.out.print(i + " ");
                i++;
                LockSupport.unpark(t2);
                LockSupport.park();
            }
        }, "t1");
        t2 = new Thread(() -> {
            int i = 65;//A
            while (i < 91) {
                System.out.print((char) i + " ");
                i++;
                LockSupport.unpark(t1);
                LockSupport.park();
            }
        }, "t2");
        t1.start();
        t2.start();
    }

    private static void method_lock() {
        Lock lock = new ReentrantLock();
        Condition condition1 = lock.newCondition();
        Condition condition2 = lock.newCondition();
        new Thread(() -> {
            try {
                int i = 1;
                lock.lock();
                while (i < 27) {
                    System.out.print(i + " ");
                    i++;
                    condition2.signal();
                    condition1.await();
                }
                lock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1").start();
        new Thread(() -> {
            try {
                int i = 65;//A
                lock.lock();
                while (i < 91) {
                    System.out.print((char) i + " ");
                    i++;
                    condition1.signal();
                    condition2.await();
                }
                lock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t2").start();
    }

    /**
     * 利用wait+notifyAll
     */
    private static void method_synchronized() {
        Object monitor = new Object();
        new Thread(() -> {
            try {
                int i = 1;
                while (i < 27) {
                    synchronized (monitor) {
                        System.out.print(i + " ");
                        i++;
                        monitor.notifyAll();
                        monitor.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                int i = 65;//A
                while (i < 91) {
                    synchronized (monitor) {
                        System.out.print((char) i + " ");
                        i++;
                        monitor.notifyAll();
                        monitor.wait();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
