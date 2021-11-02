package com.agh.lab3.pipelining;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class Producer extends Thread {
    private final Buffer _buf;
    private final int iterations;
    private final ThreadPool threadPool;
    private final Semaphore semaphore;

    Producer(Buffer buffer, int iterations, ThreadPool threadPool, Semaphore semaphore) {
        this._buf = buffer;
        this.iterations = iterations;
        this.threadPool = threadPool;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; ++i) {
            try {
                semaphore.P();
                threadPool.doOperation(_buf.getClass().getDeclaredMethod("put", int.class), i);
                semaphore.V();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }
}

class Consumer extends Thread {
    private final int iterations;
    private final ThreadPool threadPool;
    private final Buffer _buf;
    private final Semaphore semaphore;

    Consumer(Buffer buffer, int iterations, ThreadPool threadPool, Semaphore semaphore) {
        this._buf = buffer;
        this.iterations = iterations;
        this.threadPool = threadPool;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; ++i) {
            try {
                semaphore.P();
                threadPool.doOperation(_buf.getClass().getDeclaredMethod("get"), -1);
                semaphore.V();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
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
        System.out.println("Consumer received: " + returnVal);
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

class OperationThread extends Thread {
    private Method currentMethod;
    private final Buffer _buf;
    private int currentIndex;

    OperationThread(Buffer _buf) {
        this._buf = _buf;
    }

    public void setCurrentMethod(Method currentMethod, int num) {
        this.currentMethod = currentMethod;
        this.currentIndex = num;
        start();
    }

    @Override
    public void run() {
        try {
            if (currentIndex != -1)
                currentMethod.invoke(_buf, currentIndex);
            else currentMethod.invoke(_buf);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}


class ThreadPool {
    private final Buffer buffer;
    private final List<OperationThread> threads = new ArrayList<>();
    private int lastUsedThread = 0;

    ThreadPool(int size, Buffer buffer) {
        this.buffer = buffer;
        for (int i = 0; i < size; i++) {
            OperationThread operationThread = new OperationThread(buffer);
            threads.add(operationThread);
        }
    }

    void doOperation(Method method, int num) {
        threads.get(lastUsedThread).setCurrentMethod(method, num);
        threads.set(lastUsedThread, new OperationThread(buffer));
        lastUsedThread++;
        if (lastUsedThread == threads.size()) lastUsedThread = 0;
    }
}

class PKmon {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore();
        Semaphore threadPoolSem = new Semaphore();
        Buffer buffer = new Buffer(100, semaphore);
        ThreadPool threadPool = new ThreadPool(5, buffer);

        Producer producer = new Producer(buffer, 10, threadPool, threadPoolSem);
        Consumer consumer = new Consumer(buffer, 10, threadPool, threadPoolSem);

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
