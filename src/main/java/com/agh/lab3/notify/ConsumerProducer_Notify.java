package com.agh.lab3.notify;

import java.util.concurrent.TimeUnit;

class Producer extends Thread {
    private final Buffer _buf;

    Producer(Buffer buffer) {
        _buf = buffer;
    }

    public void run() {
        for (int i = 0; i < 100; ++i) {
            _buf.put(i);
        }
    }
}

class Consumer extends Thread {
    private final Buffer _buf;

    Consumer(Buffer buffer) {
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

            System.out.println(_buf.get());
        }
    }
}

class BufferNode {
    BufferNode next;
    final int value;

    BufferNode(int value) {
        this.value = value;
    }
}

class Buffer {
    BufferNode head;
    BufferNode tail;
    private final int N;
    private int currentSize = 0;

    Buffer(int n) {
        N = n;
    }

    synchronized void put(int value) {
        while(currentSize >= N){
            try {
                System.out.println("Producer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (head == null) {
            head = new BufferNode(value);
        } else if (tail == null) {
            tail = new BufferNode(value);
            head.next = tail;
        } else {
            tail.next = new BufferNode(value);
            tail = tail.next;
        }
        currentSize++;
        notify();
    }

    synchronized int get() {
        while(isEmpty()){
            try {
                System.out.println("Consumer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        currentSize--;
        int res = head.value;
        head = head.next;
        notify();
        return res;
    }

    boolean isEmpty() {
        return currentSize == 0;
    }
}

class PKmon {
    public static void main(String[] args) {
        Buffer buffer = new Buffer(10);

        Consumer consumer = new Consumer(buffer);
        Producer producer = new Producer(buffer);

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
