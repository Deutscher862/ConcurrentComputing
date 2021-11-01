package com.agh.lab3.semaphore;

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
            System.out.println("Consumer received " + _buf.get());
        }
    }
}

class Buffer {
    private final List<Integer> values = new ArrayList<>();
    private final int M;
    private final Semaphore semaphore;

    Buffer(int m, Semaphore semaphore) {
        this.M = m;
        this.semaphore = semaphore;
    }

    synchronized void put(int i) {
        semaphore.P();
        System.out.println("Producer puts " + i);
        values.add(i);
        semaphore.V();
    }

    synchronized int get() {
        semaphore.P();
        int returnVal = values.get(0);
        values.remove(0);
        semaphore.V();
        return returnVal;
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
        Buffer buffer = new Buffer(100, semaphore);

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
