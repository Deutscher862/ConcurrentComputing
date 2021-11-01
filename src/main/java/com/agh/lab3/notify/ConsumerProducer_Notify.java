package com.agh.lab3.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Producer extends Thread {
    private final Buffer _buf;

    Producer(Buffer buffer) {
        _buf = buffer;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; ++i) {
            System.out.println("Producer puts " + i);
            _buf.put(i);
        }
    }
}

class Consumer extends Thread {
    private final Buffer _buf;

    Consumer(Buffer buffer) {
        this._buf = buffer;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; ++i) {
            System.out.println("Consumer received: " + _buf.get());
        }
    }
}

class Buffer {
    private final List<Integer> values = new ArrayList<>();
    private final int M;

    Buffer(int m) {
        this.M = m;
    }

    synchronized void put(int i) {
        while (values.size() >= M) {
            try {
                System.out.println("Producer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        values.add(i);
        notify();
    }

    synchronized int get() {
        while (values.isEmpty()) {
            try {
                System.out.println("Consumer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int returnVal = values.get(0);
        values.remove(0);
        notify();
        return returnVal;
    }
}

class PKmon {
    public static void main(String[] args) {
        Buffer buffer = new Buffer(100);

        int noProducers = 100;
        int noConsumers = 100;

        ExecutorService service = Executors.newFixedThreadPool(noProducers + noConsumers);

        for (int i = 1; i <= noProducers; i++) {
            service.submit(new Producer(buffer));
        }

        for (int i = 1; i <= noConsumers; i++) {
            service.submit(new Consumer(buffer));
        }
        service.shutdown();
    }
}
