package com.fisrt.queue.v1.array.devtest;

import com.fisrt.queue.v1.Queue;
import com.fisrt.queue.v1.array.dev.ArrayQueue1;

/**
 * @author
 * @since 2021/10/25 10:17
 */
public class ArrayQueue1Test {

    /**
     * 测试放N个数据，取N个数据的耗时
     * <p>
     * 经测32,768个数据操作耗时大约1335毫秒，1秒多一点。
     * 要是上1亿的数据就别指望了。
     * 这个时间复杂度扛不住哇。
     *
     * @param args
     */
    public static void main(String[] args) {
        //1<<27=134,217,728
        int n = 1 << 15;//32,768
        Queue<Integer> queue = new ArrayQueue1<>(n);
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            queue.enqueue(i + 1);
        }
        for (int i = 0; i < n; i++) {
            queue.dequeue();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - start));

    }

}
