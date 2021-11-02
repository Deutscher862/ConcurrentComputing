package com.agh.lab3.semaphore;

import java.util.ArrayList;
import java.util.List;

class Producer extends Thread {
    private final Buffer _buf;
    private final int iterations;

    Producer(Buffer buffer, int iterations) {
        this._buf = buffer;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; ++i) {
            _buf.put(i);
        }
    }
}

class Consumer extends Thread {
    private final Buffer _buf;
    private final int iterations;

    Consumer(Buffer buffer, int iterations) {
        this._buf = buffer;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; ++i) {
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
        while (values.size() >= M) {
            try {
                System.out.println("Producer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        semaphore.P();
        System.out.println("Producer puts " + i);
        values.add(i);
        semaphore.V();
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
        semaphore.P();
        int returnVal = values.get(0);
        values.remove(0);
        semaphore.V();
        notify();
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
        List<Thread> threads = new ArrayList<>();

        int noProducers = 100;
        int noConsumers = 100;

        for (int i = 0; i < noConsumers + noProducers; i++) {
            Thread thread = i < noProducers ? new Producer(buffer, 100) : new Consumer(buffer, 100);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
