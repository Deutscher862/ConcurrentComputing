package com.agh.lab2;

class Counter {
    private int _val;
    private int op = 0;

    public Counter(int n) {
        _val = n;
    }

    public void inc() {
        _val++;
        op++;
//        System.out.println(_val);
    }

    public void dec() {
        _val--;
        op++;
//        System.out.println(_val);
    }

    public int value() {
        return _val;
    }

    public int getOp() {
        return op;
    }
}

class CountingSemaphore {
    private int counter = 1;
    Semaphore canUseResources = new Semaphore();
    Semaphore canChangeCounterValue = new Semaphore();

    void P() {
        canChangeCounterValue.P();
        if (counter <= 0) {
            counter--;
            canChangeCounterValue.V();
            canUseResources.P();
        } else {
            counter--;
            canChangeCounterValue.V();
        }
    }

    void V() {
        canChangeCounterValue.P();
        if (counter < 0) {
            canUseResources.V();
        }
        counter++;
        canChangeCounterValue.V();
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

class IThread extends Thread {
    private final Counter counter;
    private final CountingSemaphore countingSemaphore;

    IThread(Counter counter, CountingSemaphore countingSemaphore) {
        this.counter = counter;
        this.countingSemaphore = countingSemaphore;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100000; i++) {
            this.countingSemaphore.P();
            counter.inc();
            this.countingSemaphore.V();
        }
    }
}

class DThread extends Thread {
    private final Counter counter;
    private final CountingSemaphore countingSemaphore;

    DThread(Counter counter, CountingSemaphore countingSemaphore) {
        this.counter = counter;
        this.countingSemaphore = countingSemaphore;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100000; i++) {
            this.countingSemaphore.P();
            counter.dec();
            this.countingSemaphore.V();
        }
    }
}

class Race {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {

            Counter cnt;
            IThread iThread;
            DThread dThread;
            cnt = new Counter(0);
            CountingSemaphore countingSemaphore = new CountingSemaphore();

            iThread = new IThread(cnt, countingSemaphore);
            dThread = new DThread(cnt, countingSemaphore);
            iThread.start();
            dThread.start();
            try {
                iThread.join();
                dThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Stan= " + cnt.value());
            System.out.println("Wykonane operacje= " + cnt.getOp());
        }
    }
}

