package com.agh.lab4;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.agh.lab4.PKmon.M;

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
            int noValues = (int) (((Math.random() * 100) % M) + 1);
            System.out.println("Producer puts " + i + " elements");
            for (int j = 0; j < noValues; j++)
                _buf.put(j);
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
            int noValues = (int) (((Math.random() * 100) % M) + 1);
            System.out.println("Consumer gets " + noValues + " elements");
            for (int j = 0; j < noValues; j++) {
                System.out.println("Consumer received: " + _buf.get());
            }
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
    public static int M = 2;

    public static void main(String[] args) {
        Buffer buffer = new Buffer(2 * M);
        List<Thread> threads = new ArrayList<>();

        int noProducers = 1;
        int noConsumers = 1;

        Instant start = Instant.now();

        for (int i = 0; i < noConsumers + noProducers; i++) {
            Thread thread = i < noProducers ? new Producer(buffer, 5) : new Consumer(buffer, 5);
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
        Instant finish = Instant.now();
        double timeElapsed = Duration.between(start, finish).toMillis();
    }
}
