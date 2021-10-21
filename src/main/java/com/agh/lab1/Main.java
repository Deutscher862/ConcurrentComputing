package com.agh.lab1;

import java.io.*;
import java.util.concurrent.TimeUnit;

class Counter {
    private int _val;

    public Counter(int n) {
        _val = n;
    }

    public void inc() {
        _val++;
    }

    public void dec() {
        _val--;
    }

    public int value() {
        return _val;
    }
}

class OperationCounter {
    private int operationsDone = 0;
    private int operationToBeDone = 0;

    void incrementDoneOperations() {
        operationsDone++;
    }

    void incrementToBeDone() {
        operationToBeDone++;
    }

    boolean areAllOperationsDone() {
        return operationsDone == operationToBeDone;
    }
}

class SynchronizingThread extends Thread {
    private final Counter counter;
    private final OperationCounter incCounter;
    private final OperationCounter decCounter;

    SynchronizingThread(Counter counter, OperationCounter incCounter, OperationCounter decCounter) {
        this.counter = counter;
        this.incCounter = incCounter;
        this.decCounter = decCounter;
    }

    @Override
    public void run() {
        while (!incCounter.areAllOperationsDone() || !decCounter.areAllOperationsDone()) {
            if (!incCounter.areAllOperationsDone()) {
                counter.inc();
                incCounter.incrementDoneOperations();
            }
            if (!decCounter.areAllOperationsDone()) {
                counter.dec();
                decCounter.incrementDoneOperations();
            }
        }
    }
}

class IThread extends Thread {
    private final OperationCounter incCounter = new OperationCounter();

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            incCounter.incrementToBeDone();
        }
    }

    public OperationCounter getIncCounter() {
        return incCounter;
    }
}

class DThread extends Thread {
    private final OperationCounter decCounter = new OperationCounter();

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            decCounter.incrementToBeDone();
        }
    }

    public OperationCounter getDecCounter() {
        return decCounter;
    }
}

class Race {
    public static void main(String[] args) {
        Counter cnt;
        SynchronizingThread synchronizingThread;
        IThread iThread;
        DThread dThread;

        try (Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
            for (int i = 0; i < 10; i++) {

                cnt = new Counter(0);
                iThread = new IThread();
                dThread = new DThread();
                synchronizingThread = new SynchronizingThread(cnt, iThread.getIncCounter(), dThread.getDecCounter());

                iThread.start();
                dThread.start();

                TimeUnit.MILLISECONDS.sleep(100);
                synchronizingThread.start();

                iThread.join();
                dThread.join();
                synchronizingThread.join();

                output.append(String.valueOf(cnt.value()).concat("\n"));

                System.out.println("Stan= " + cnt.value());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
