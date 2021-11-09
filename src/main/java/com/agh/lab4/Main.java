package com.agh.lab4;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
            int noValues = (int) (((Math.random() * 10 * M) % M) + 1);
            System.out.println("Producer puts " + noValues + " elements");
            List<Integer> valuesToPut = new ArrayList<>();
            for (int j = 0; j < noValues; j++)
                valuesToPut.add(j);
            _buf.put(valuesToPut);
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            int noValues = (int) (((Math.random() * 10 * M) % M) + 1);
            System.out.println("Consumer gets " + noValues + " elements");
            List<Integer> returnedValues = _buf.get(noValues);
            System.out.println("Returned values: " + returnedValues);
        }
        try {
            sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Buffer {
    private final List<Integer> values = new ArrayList<>();
    private final int maxSize;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    Buffer(int m) {
        this.maxSize = m;
    }

    synchronized void put(List<Integer> valuesToPut) {
        while (values.size() + valuesToPut.size() >= maxSize) {
            try {
                System.out.println("Producer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock.writeLock().lock();
        try {
            while (!valuesToPut.isEmpty()) {
                values.add(valuesToPut.get(0));
                valuesToPut.remove(0);
            }
        } finally {
            lock.writeLock().unlock();
        }
        notify();
    }

    synchronized List<Integer> get(int noValues) {
        while (values.size() < noValues) {
            try {
                System.out.println("Consumer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock.readLock().lock();
        List<Integer> results = new ArrayList<>();
        try {
            for (int i = 0; i < noValues; i++) {
                results.add(values.get(0));
                values.remove(0);
            }
        } finally {
            lock.readLock().unlock();
        }
        notify();
        return results;
    }
}

class PKmon {
    public static int M = 500;

    public static void main(String[] args) {
        Buffer buffer = new Buffer(2 * M);
        List<Thread> threads = new ArrayList<>();

        int noProducers = 25;
        int noConsumers = 25;

        try (Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
            {
                Instant start = Instant.now();
                for (int i = 0; i < noConsumers + noProducers; i++) {
                    Thread thread = i < noProducers ? new Producer(buffer, 1) : new Consumer(buffer, 1);
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
                System.out.println("Czas działania: " + timeElapsed / 1000 + "s");

                output.append(String.valueOf(timeElapsed / 1000)).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
