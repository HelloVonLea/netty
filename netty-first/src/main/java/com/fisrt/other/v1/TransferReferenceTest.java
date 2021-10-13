package com.fisrt.other.v1;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 * @author libo
 * @since 2021/9/23 15:51
 */
public class TransferReferenceTest {

    public static void main(String[] args) {
        /**
         * TransferQueue是一个BlockingQueue阻塞队列。
         * 生产者会等待消费者接收元素。
         * A BlockingQueue in which producers may wait for consumers
         * to receive elements.
         * //题外话，wait+时间，地点而wait for+等待的目的
         * 这里是一个in which引导的定语从句。
         * A TransferQueue may be useful for example in message
         * passing applications in which producers sometimes await
         * receipt of  elements by consumers invoking take or poll,
         * while at other times enqueue elements without waiting for
         * receipt.
         * 一个TransferReference可能是有用的，比如：
         * 传递消息的应用程序，生产者有时候等候消费者执行take
         * 或poll来接收元素，然而在其它时候入队元素不用等待
         * 消费者接收
         * Non-blocking and time-out versions of tryTransfer re also available.
         * 非阻塞和超时在tryTransfer支持。
         * A TransferQueue may also be queried, via hasWaitingConsumer,
         * whether there are any threads waiting for item, which is a converse
         * analogy to a peek operation.
         * hasWaitingConsumer方法可以判断时候有消费者，这是peek操作
         * 的反向类比。
         * Like other blocking queues, a TransferQueue may be capacity bounded.
         * If so, an attempted transfer operation may initially block waiting for
         * available space, and/or subsequently block waiting for reception by a
         * consumer. Note that in a queue with zero capacity, such as
         * SynchronousQueue, put and transfer are effectively synonymous.
         * 同其它的blocking queue，TransferQueue的容量是有限的，一个
         * 传递可能会阻塞等待可用空间，并且/或者阻塞消费者。注意：
         * 一个0容量的queue，如SynchronousQueue，put和transfer是同义的
         *
         *
         *
         *
         */
        TransferQueue<String> queue = new LinkedTransferQueue<>();


    }


}
