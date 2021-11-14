package com.agh.lab5;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Main {
    public static void main(String[] args) {
        for (int writersAmount = 1; writersAmount <= 10; ++writersAmount) {
            for (int readersAmount = 10; readersAmount <= 100; readersAmount += 5) {
                MainThread mainThread = new MainThread(readersAmount, writersAmount);
                mainThread.start();
            }
        }
    }
}

class MainThread extends Thread {
    final int noReaders;
    final int noWriters;
    private final Buffer library;
    private final List<Thread> threads = new ArrayList<>();

    MainThread(int noReaders, int noWriters) {
        this.noReaders = noReaders;
        this.noWriters = noWriters;
        this.library = new Buffer();
    }

    @Override
    public void run() {
        long start = System.nanoTime();
        for (int i = 0; i < noReaders + noWriters; i++) {
            Thread thread = i < noReaders ? new Reader(library) : new Writer(library);
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
        saveResults(System.nanoTime() - start);
    }

    void saveResults(long time) {
        try (java.io.Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
            output.append(String.valueOf(noReaders)).append(" ");
            output.append(String.valueOf(noWriters)).append(" ");
            output.append(String.valueOf(time)).append("\n");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}

class Buffer {
    private final Semaphore readSemaphore = new Semaphore(1);
    private final Semaphore writeSemaphore = new Semaphore(1);
    private int readingCounter = 0;

    void beginReading() throws InterruptedException {
        readSemaphore.acquire();
        if (readingCounter == 0) {
            writeSemaphore.acquire();
        }
        readingCounter++;
        readSemaphore.release();
    }

    public void endReading() throws InterruptedException {
        readSemaphore.acquire();
        readingCounter--;
        if (readingCounter == 0) {
            writeSemaphore.release();
        }
        readSemaphore.release();
    }

    public void beginWriting() throws InterruptedException {
        writeSemaphore.acquire();
    }

    public void endWriting() {
        writeSemaphore.release();
    }
}

class Reader extends Thread {
    private final Buffer buffer;

    Reader(Buffer library) {
        this.buffer = library;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            try {
                buffer.beginReading();
                buffer.endReading();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Writer extends Thread {
    private final Buffer buffer;

    Writer(Buffer library) {
        this.buffer = library;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            try {
                buffer.beginWriting();
                buffer.endWriting();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
