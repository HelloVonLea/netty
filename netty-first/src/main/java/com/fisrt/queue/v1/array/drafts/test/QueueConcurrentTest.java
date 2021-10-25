package com.fisrt.queue.v1.array.drafts.test;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue4;
import com.fisrt.queue.v1.array.dev.ArrayQueue5;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 这个测试类演示了解决了并发问题后的结果
 * 第一次：
 * 演示以ArrayQueue4为例,同时队列中数组保障容量
 * 第二次：
 * 演示以ArrayQueue4为例,同时队列中数组容量有限
 *
 * @since 2021/10/12 16:06
 */
public class QueueConcurrentTest {

    public static void main(String[] args) throws InterruptedException {

        int n = 1 << 4;

//        arrayQueue2Question2Test(n);
        //我们起N个线程，每个线程放N个数据，我们期待queue中有N*N个数据
        //N=512，实际：262,144，期望：262,144
        //N=256，实际：65,536，期望：65,536
        //可以看到，数据少了。理论和实际相符。
//        arrayQueue4Test1(n);
        arrayQueue4Test2(n);
//        arrayBlockingQueueTest3(n);

    }

    //测试失败的，这个代码没跑成功
    private static void arrayBlockingQueueTest3(int n) throws InterruptedException {
        java.util.Queue<Integer> queue = new ArrayBlockingQueue<>(512);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    boolean offer = false;
                    do {
                        offer = queue.offer(j + 1);
                    } while (!offer);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            latch.awaitAdvance(0);
            int c = 0;
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.poll();
                if (num != null) {
//                    System.out.println(num);
                    count.getAndIncrement();
                } else {
                    c++;
                }
            }
            System.out.println("c=" + c);
            phaser.arrive();
        }).start();
        Thread.sleep(1_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
//        System.out.println("put=="+count1.get());
        System.out.println("queue的大小" + count.get());

    }


    /**
     * 这个方法容量定长
     * 这个时候出现问题：消费者拿到了null数据
     * 当n=4的时候，就有这样的情况出现了。
     * 通过打印日志，我们发现这样一行
     * <b>
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：5
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：6
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：7
     * 生产者添加数据后数组的样子：[3, 4, 1, 2]	 大小size：8
     * 生产者添加数据后数组的样子：[3, 4, 5, 2]	 大小size：9
     * </b>
     * 说明什么，说明队列满了，size还是增加了。
     * 起初我以为时没加volatile的问题，size没刷新到线程。
     * 经测试发现不是。这里明显的多线程有地方没理解。
     * 之后目光注意到，队列满了之后，不应该只判断一次。
     * 判断一次是不对的，wait/notify中就说了应该循环判断，
     * 防止被错误唤醒，这不就是嘛。
     * <p>
     * 回顾整个过程：
     * 首先，发现问题。写了一个测试代码，用多个生产者，单消费者进行测试。
     * 最后在统计消费的数据时候，发现实际的元素比期望的少很多。
     * 经日志打印，发现拿出的数据为null，而takeIndex是正常的。
     * 而putIndex处的数据也为null。一时从逻辑上思考原因根本找不到头绪。
     * 还以为自己的测试代码有问题。但是arrayQueue4Test1()的代码测试正确。
     * 还怀疑过自己的Phaser使用，因为这个也不熟，不过测试后发现这个没问题。
     * 当确认并发控制器和测试代码没问题后，才回过头来再审视ArrayQueue4。
     * 既然是拿出时候的问题，我就打印了拿出时数据、拿出时数组的样子。
     * 一度debug，看通过debug看出问题，但是很遗憾没有。多线程的情况下debug确实
     * 不好排除问题。还得靠日志。
     * 在查看打印后的数组和size后，意外发现size竟然大于4了。
     * 心想，卧槽，又一个问题。哭死。
     * 起初我以为是线程数据没同步，跑去加了volatile，但是加了之后还是不对。
     * <p>
     * size大于4说明是size++造成的，因为只有一处使size增加的地方。
     * 之后打印生产这enqueue中添加数据后数组的样子，发现size竟然达到过216。
     * 这下震惊了。
     * 仔细一看，发现满了之后等待，唤醒之后就执行size++了。
     * 卧槽，少了什么？少了while条件的循环判断。
     * 之前学习的时候while就是防止线程意外被唤醒，而条件依然满足的情况。
     * 很明显，这里有多个生产者，可能生产线程1被唤醒了，生产线程2也被唤醒了。
     * 一个if判断的话，唤醒之后就进行size++了，而线程1可能生产了一个，线程2
     * 也生产了一个size就多加了。
     * 经过改正之后，测试出正确结果。
     * <p>
     * 总结：
     * 1.代码需要测试，不测试你的代码的问题你发现不了。
     * 2.测试会发现问题，首先考虑你的这个测试程序是不是正确。
     * 确保测试程序逻辑和代码都没问题。对比一下。相同条件下不同变量。
     * 期望与实际。
     * 3.测试程序没问题下考虑代码逻辑有问题。你知道他有问题，但是你这时
     * 肯定不知道原因。怎么办？
     * 打日志或debug。优先打日志。因为多线程下用日志调试最方便。
     * 日志的目的就是帮助我们发现问题。
     * 我们应该怎样打日志？
     * 第一次，出问题地方打日志。如果看不出原因，
     * 第二次，在此处打印更多的变量等信息。
     * 这个时候你一定得发现有地方错误了。并思考是在哪个地方产生了
     * 这个错误。
     * 第三次，在产生错误的地方打印全面，详细的日志。然后进行复现、
     * 更改、测试。基本就能解决。
     * 如果还不能解决，说明一定有一块知识你不知道，或者没掌握。
     * 慢慢来，一点点啃，你就会学会那块知识。
     * 可能你在每次日志的时候，都会猜，可能是哪儿错了，然后改正。
     * 测试，却发现不对。
     * <p>
     * 你解决不了问题，主要原因是，一：你没懂这块知识，或许了解但一定
     * 没明白原理，就是不会。二：没有解决问题的方式方法。不知道一步步
     * 该怎么解决问题。可能你在第一步就卡住了，如测试代码的书写，你不确定
     * 是不是正确。这个时候就需要，你去啃这块知识。还是那句话，探索。
     * 一定要保证用你已有的，会的基础上去走下一步。
     * 比如说，这个测试代码中的同步器，我用的新的，旧的没用，就耽误我不少时间。
     * 遇到问题、困难，不要慌，不要随意放弃。多去确定一些正确的，没错的。（会的）
     * 然后，掌握更多的真实信息(日志)，这样你就可以发现问题出处。
     * 再针对这个问题进行研究，就可以解决了。
     * <p>
     * <p>
     * 简单流程：
     * 确保对的东西==>
     * 确定问题的方向==>
     * 获取信息(打印日志)==>猜想，验证。如果猜想不对==>
     * 获取更多的信息==>
     * 信息足够多的时候，仔细观察信息，确定问题发生点==>
     * 针对问题点进行获取信息，分析信息，猜想，验证。
     * <p>
     * 回顾：其实对volatile和锁认识并不到位。
     * <p>
     * 一个点一个点的串联起来，就构成复杂的东西了。
     * 先学一个一个的点，然后学怎么串联。
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test2(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue5<>(4);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger();
        new Thread(() -> {
            latch.awaitAdvance(0);
            int c = 0;
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    System.out.println("循环次数" + i + "值：" + num);
                    count.getAndIncrement();
                } else {
                    c++;
                }
            }
            System.out.println("c=" + c);
            phaser.arrive();
        }).start();
        Thread.sleep(1_000);
        latch.arrive();
        phaser.arriveAndAwaitAdvance();
//        System.out.println("put=="+count1.get());
        System.out.println("总共拿到的数据：" + count.get());

    }

    /**
     * 这个测试保证queue的容量够用
     *
     * @param n
     * @throws InterruptedException
     */
    private static void arrayQueue4Test1(int n) throws InterruptedException {
        Queue<Integer> queue = new ArrayQueue4<>(n * n);
        Phaser phaser = new Phaser(n + 2);
        //latch控制同时开始
        Phaser latch = new Phaser(1);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                latch.awaitAdvance(0);
//                System.out.println(Thread.currentThread().getName() + "线程开始了");
                for (int j = 0; j < n; j++) {
                    queue.enqueue(j + 1);
                }
                phaser.arrive();
            }).start();
        }
        AtomicInteger count = new AtomicInteger(0);
        new Thread(() -> {
            latch.awaitAdvance(0);
            for (int i = 0; i < (n * n); i++) {
                Integer num = queue.dequeue();
                if (num != null) {
                    count.getAndIncrement();
//                    System.out.println(num);
                }
            }
            phaser.arrive();
        }).start();
        Thread.sleep(10_000);
        int arrive = latch.arrive();
        System.out.println("phaser:" + arrive);
        phaser.arriveAndAwaitAdvance();
        System.out.println(count.get());

    }


}
