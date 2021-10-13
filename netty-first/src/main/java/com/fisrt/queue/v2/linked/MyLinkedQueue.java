package com.fisrt.queue.v2.linked;

import com.fisrt.queue.v2.MyQueue;

/**
 * 参考LinkedBlockingQueue
 *
 * @author libo
 * @since 2021/10/12 10:22
 */
public class MyLinkedQueue<E> implements MyQueue<E> {

    private int size;
    private Node head;
    private Node tail;

    public MyLinkedQueue() {
        head = tail = new Node(null);
    }

    @Override
    public boolean add(Object o) {
        return false;
    }

    @Override
    public boolean offer(Object o) {
        return false;
    }

    public void enqueue(E e) {
        Node node = new Node(e);
        Node oldTail = this.tail;
        oldTail.next = node;
        tail = node;
        if (head == tail) {
            head.next = node;
        }
        size++;
    }

    @Override
    public E remove() {
        return null;
    }

    @Override
    public E poll() {
        return null;
    }

    public E dequeue() {
        if (head == tail)
            return null;
        Node node = this.head;
        this.head = node.next;
        size--;
        return node.e;
    }

    @Override
    public E element() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    private class Node {
        E e;
        Node next;

        public Node(E e) {
            this.e = e;
        }
    }


}
