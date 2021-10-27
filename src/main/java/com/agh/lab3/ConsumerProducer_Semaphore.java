package com.agh.lab3;

import java.util.concurrent.TimeUnit;

class Producer extends Thread {
    private final Buffer _buf;
    private final Semaphore semaphore;

    Producer(Semaphore semaphore, Buffer buffer) {
        _buf = buffer;
        this.semaphore = semaphore;
    }

    public void run() {
        for (int i = 0; i < 100; ++i) {
            semaphore.P();
            _buf.put(i);
            semaphore.V();
        }
    }
}

class Consumer extends Thread {
    private final Semaphore semaphore;
    private final Buffer _buf;

    Consumer(Semaphore semaphore, Buffer buffer) {
        this.semaphore = semaphore;
        this._buf = buffer;
    }

    public void run() {
        for (int i = 0; i < 100; ++i) {
            while (_buf.isEmpty()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            semaphore.P();
            System.out.println(_buf.get());
            semaphore.V();
        }
    }
}

class BufferNode {
    BufferNode next;
    final int value;

    BufferNode(int value) {
        this.value = value;
    }

    boolean hasNext() {
        return next != null;
    }
}

class Buffer {
    BufferNode head;
    BufferNode tail;

    public void put(int value) {
        if (head == null) {
            head = new BufferNode(value);
        } else if (tail == null) {
            tail = new BufferNode(value);
            head.next = tail;
        } else {
            tail.next = new BufferNode(value);
            tail = tail.next;
        }
    }

    public int get() {
        int res = head.value;
        head = head.next;
        return res;
    }

    boolean isEmpty() {
        return head == null;
    }
}

class Semaphore {
    private boolean state = true;
    private int waitCounter = 0;

    synchronized void P() {
        waitCounter++;
        while (!state) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        state = false;
        waitCounter--;
    }

    synchronized void V() {
        if (waitCounter > 0) {
            this.notify();
        }
        state = true;
    }
}

class PKmon {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore();
        Buffer buffer = new Buffer();

        Consumer consumer = new Consumer(semaphore, buffer);
        Producer producer = new Producer(semaphore, buffer);

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
