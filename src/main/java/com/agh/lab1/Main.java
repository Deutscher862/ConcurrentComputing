package com.agh.lab1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

class Counter {
    private int _val;
    public Counter(int n) {
        _val = n;
    }
    public void inc() { _val++; }
    public void dec() { _val--; }
    public int value() { return _val; }
}
class IThread extends Thread {
    private final Counter counter;
    IThread(Counter counter) {
        this.counter = counter;
    }
    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            counter.inc();
        }
    }
}
class DThread extends Thread {
    private final Counter counter;
    DThread(Counter counter) {
        this.counter = counter;
    }
    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            counter.dec();
        }
    }
}
class Race {
    public static void main(String[] args) {
        Counter cnt;
        IThread iThread;
        DThread dThread;
        try (Writer output = new BufferedWriter(
                new FileWriter("results.txt", true))) {
            cnt = new Counter(0);
            iThread = new IThread(cnt);
            dThread = new DThread(cnt);
            iThread.start();
            dThread.start();
            iThread.join();
            dThread.join();
            output.append(String.valueOf(cnt.value()).concat("\n"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
