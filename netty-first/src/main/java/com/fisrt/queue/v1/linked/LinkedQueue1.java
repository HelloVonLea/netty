package com.fisrt.queue.v1.linked;

import com.fisrt.queue.v1.Queue;

/**
 * 链表实现的队列
 *
 * @since 2021/10/14 18:05
 */
public class LinkedQueue1<E> implements Queue<E> {

    private Node head;
    private Node tail;
    private int size;

    @Override
    public void enqueue(E e) {
        Node node = new Node(e, null);
        Node oldTail = this.tail;
        oldTail.next = node;
        tail = node;
        if (head == tail) {
            head.next = node;
        }
        size++;
    }

    @Override
    public E dequeue() {
        if (head == tail)
            return null;
        Node node = this.head;
        this.head = node.next;
        size--;
        return (E) node.e;
    }

    @Override
    public int size() {
        return this.size;
    }

    private class Node<E> {
        E e;
        Node next;

        public Node(E e, Node next) {
            this.e = e;
            this.next = next;
        }
    }
}
